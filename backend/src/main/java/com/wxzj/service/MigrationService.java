package com.wxzj.service;

import com.wxzj.common.BizException;
import com.wxzj.common.NoGenerator;
import com.wxzj.dto.MigratePreview;
import com.wxzj.dto.MigrateReport;
import com.wxzj.entity.*;
import com.wxzj.repository.*;
import com.wxzj.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationService {

    private final CommunityRepository communityRepository;
    private final BuildingRepository buildingRepository;
    private final HouseRepository houseRepository;
    private final OwnerRepository ownerRepository;
    private final HouseOwnerRepository houseOwnerRepository;
    private final FundAccountRepository fundAccountRepository;
    private final DepositRepository depositRepository;
    private final FundFlowRepository fundFlowRepository;

    private static final List<DateTimeFormatter> TIME_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm"),
            DateTimeFormatter.ofPattern("yyyy年M月d日"));

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyyMMdd"));

    private Map<String, Community> communityCache = new HashMap<>();

    public MigratePreview preview(MultipartFile file) {
        List<CSVRecord> records = parse(file);
        if (records.isEmpty()) {
            throw new BizException("文件无数据行");
        }
        MigratePreview p = new MigratePreview();
        p.setTotalRows(records.size());
        List<String> headers = new ArrayList<>(records.get(0).toMap().keySet());
        p.setHeaders(headers);
        for (CSVRecord r : records) {
            if (p.getSample().size() >= 100) {
                break;
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (String k : headers) {
                row.put(k, r.get(k));
            }
            p.getSample().add(row);
        }
        checkHeaders(headers);
        return p;
    }

    /** 执行迁移：社区/楼栋/房屋/业主/账户 自动补齐，按摘要生成缴存单与资金流水 */
    @Transactional
    public MigrateReport migrate(MultipartFile file, LoginUser user) {
        List<CSVRecord> records = parse(file);
        MigrateReport report = new MigrateReport();
        report.setTotalRows(records.size());
        if (records.isEmpty()) {
            return report;
        }
        Map<String, Integer> header = records.get(0).toMap().keySet().stream().collect(HashMap::new,
                (m, k) -> m.put(norm(k), 1), HashMap::putAll);
        if (!header.containsKey("小区名称") || !header.containsKey("摘要") || !header.containsKey("交易金额")) {
            throw new BizException("缺少必需列：小区名称 / 摘要 / 交易金额");
        }
        communityCache.clear();
        long start = System.currentTimeMillis();
        int rowNo = 0;
        for (CSVRecord r : records) {
            rowNo++;
            try {
                processRow(r, report, user, rowNo);
            } catch (Exception e) {
                report.error("第 " + rowNo + " 行: " + e.getMessage());
                log.warn("迁移第 {} 行失败: {}", rowNo, e.getMessage());
            }
        }
        // 刷新小区房屋数
        for (Community c : communityCache.values()) {
            c.setHouseCount((int) houseRepository.countByCommunityId(c.getId()));
            communityRepository.save(c);
        }
        log.info("迁移完成 共{}行 耗时{}ms", records.size(), System.currentTimeMillis() - start);
        return report;
    }

    private void processRow(CSVRecord r, MigrateReport report, LoginUser user, int rowNo) {
        String communityName = r.get("小区名称").trim();
        if (communityName.isEmpty()) {
            throw new BizException("小区名称为空");
        }
        String buildingNo = get(r, "楼").trim();
        String unit = get(r, "单元").trim();
        String houseNoRaw = get(r, "房屋").trim();
        String ownerName = get(r, "业主姓名").trim();
        String idCard = get(r, "证件号码").trim();
        String summary = get(r, "摘要").trim();
        String amountStr = get(r, "交易金额").trim();
        String timeStr = get(r, "交易时间").trim();
        String invoice = get(r, "发票号").trim();

        if (houseNoRaw.isEmpty()) {
            throw new BizException("房屋为空");
        }
        if (idCard.isEmpty()) {
            throw new BizException("证件号码为空");
        }
        String houseNo = unit.isEmpty() ? houseNoRaw : unit + "-" + houseNoRaw;

        // 1 小区
        Community community = communityCache.get(communityName);
        if (community == null) {
            community = communityRepository.findAll().stream()
                    .filter(c -> c.getName().equals(communityName)).findFirst().orElse(null);
            if (community == null) {
                community = new Community();
                community.setName(communityName);
                community.setStatus("ACTIVE");
                community.setHouseCount(0);
                community.setArea(BigDecimal.ZERO);
                community = communityRepository.save(community);
                report.setCommunityCreated(report.getCommunityCreated() + 1);
            }
            communityCache.put(communityName, community);
        }

        // 2 楼栋
        Building building = buildingRepository.findByCommunityIdOrderByBuildingNo(community.getId()).stream()
                .filter(b -> b.getBuildingNo().equals(buildingNo)).findFirst().orElse(null);
        if (building == null) {
            building = new Building();
            building.setCommunityId(community.getId());
            building.setBuildingNo(buildingNo);
            building.setName(buildingNo + "号楼");
            building.setFloors(0);
            building.setArea(BigDecimal.ZERO);
            building = buildingRepository.save(building);
            report.setBuildingCreated(report.getBuildingCreated() + 1);
        }

        // 3 房屋
        House house = houseRepository.findByBuildingIdOrderByHouseNo(building.getId()).stream()
                .filter(h -> h.getHouseNo().equals(houseNo)).findFirst().orElse(null);
        if (house == null) {
            house = new House();
            house.setCommunityId(community.getId());
            house.setBuildingId(building.getId());
            house.setHouseNo(houseNo);
            house.setFloor(parseFloor(houseNoRaw));
            BigDecimal area = parseArea(get(r, "面积"));
            house.setArea(area == null ? BigDecimal.ZERO : area);
            house.setStatus("ACTIVE");
            house = houseRepository.save(house);
            report.setHouseCreated(report.getHouseCreated() + 1);
        }

        // 4 业主（一房多主：同一房屋多行不同证件号）
        Owner owner = ownerRepository.findAll().stream()
                .filter(o -> o.getIdCard().equals(idCard)).findFirst().orElse(null);
        if (owner == null) {
            owner = new Owner();
            owner.setName(ownerName);
            owner.setIdCard(idCard);
            owner.setPhone("");
            ownerRepository.save(owner);
            report.setOwnerCreated(report.getOwnerCreated() + 1);
        }
        if (!houseOwnerRepository.existsByHouseIdAndOwnerId(house.getId(), owner.getId())) {
            HouseOwner ho = new HouseOwner();
            ho.setHouseId(house.getId());
            ho.setOwnerId(owner.getId());
            ho.setRelationType("OWNER");
            ho.setIsMain(house.getOwnerId() == null);
            houseOwnerRepository.save(ho);
        }
        if (house.getOwnerId() == null) {
            house.setOwnerId(owner.getId());
            houseRepository.save(house);
        }

        // 5 资金账户
        FundAccount account = fundAccountRepository.findByHouseId(house.getId()).orElse(null);
        LocalDateTime txTime = parseTime(timeStr);
        if (account == null) {
            account = new FundAccount();
            account.setAccountNo("WX" + System.nanoTime() % 100000000L + house.getId());
            account.setHouseId(house.getId());
            account.setOwnerId(house.getOwnerId());
            account.setCommunityId(community.getId());
            account.setOpenTime(txTime);
            account = fundAccountRepository.save(account);
            report.setAccountCreated(report.getAccountCreated() + 1);
        }

        // 6 交易
        String bizType = normalizeSummary(summary);
        if ("OPEN".equals(bizType)) {
            return;
        }
        if ("UNKNOWN".equals(bizType)) {
            throw new BizException("无法识别的摘要: " + summary);
        }
        BigDecimal amount = parseAmount(amountStr);
        if (amount == null || amount.signum() == 0) {
            throw new BizException("交易金额无效: " + amountStr);
        }

        // 幂等：发票号相同且已存在流水则跳过，防止重复导入
        if (!invoice.isEmpty()) {
            final Long acctId = account.getId();
            boolean exists = fundFlowRepository.findAll().stream()
                    .anyMatch(f -> invoice.equals(f.getRelatedNo()) && acctId.equals(f.getAccountId()));
            if (exists) {
                report.setSkipped(report.getSkipped() + 1);
                return;
            }
        }

        boolean in = "DEPOSIT".equals(bizType) || "DIVIDEND".equals(bizType);
        BigDecimal newBalance;
        if (in) {
            newBalance = account.getBalance().add(amount);
            account.setBalance(newBalance);
            account.setTotalDeposit(account.getTotalDeposit().add(amount));
            if ("DEPOSIT".equals(bizType)) {
                createDepositRecord(house, account, amount, txTime, invoice, summary);
                report.setDepositCreated(report.getDepositCreated() + 1);
            }
        } else {
            newBalance = account.getBalance().subtract(amount);
            account.setBalance(newBalance);
            if ("USE".equals(bizType)) {
                account.setTotalUsed(account.getTotalUsed().add(amount));
            } else {
                account.setTotalRefund(account.getTotalRefund().add(amount));
            }
        }
        fundAccountRepository.save(account);

        FundFlow f = new FundFlow();
        f.setFlowNo(NoGenerator.gen("LS"));
        f.setAccountId(account.getId());
        f.setHouseId(house.getId());
        f.setCommunityId(community.getId());
        f.setType("DIVIDEND".equals(bizType) ? "DEPOSIT" : bizType);
        f.setDirection(in ? "IN" : "OUT");
        f.setAmount(amount);
        f.setBalance(newBalance);
        f.setRelatedNo(invoice.isEmpty() ? NoGenerator.gen("MG") : invoice);
        f.setOperatorId(user.getId());
        f.setBizTime(txTime);
        f.setRemark(summary + (invoice.isEmpty() ? "" : "（发票号 " + invoice + "）"));
        fundFlowRepository.save(f);
        report.setFlowCreated(report.getFlowCreated() + 1);
    }

    private void createDepositRecord(House house, FundAccount account, BigDecimal amount,
                                     LocalDateTime txTime, String invoice, String summary) {
        Deposit d = new Deposit();
        d.setOrderNo(invoice.isEmpty() ? NoGenerator.gen("CJ") : "CJ" + invoice);
        d.setCommunityId(house.getCommunityId());
        d.setHouseId(house.getId());
        d.setAccountId(account.getId());
        d.setOwnerId(house.getOwnerId());
        d.setType("INITIAL");
        d.setQuantity(house.getArea());
        d.setUnitPrice(BigDecimal.ZERO);
        d.setAmount(amount);
        d.setStatus("PAID");
        d.setPayTime(txTime);
        d.setRemark(summary + "（历史迁移）");
        depositRepository.save(d);
    }

    // ---------------- 解析工具 ----------------

    private List<CSVRecord> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请上传 CSV 文件");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BizException("文件读取失败");
        }
        String content = decode(bytes);
        if (content.isEmpty()) {
            throw new BizException("文件内容为空");
        }
        try {
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setTrim(true).setIgnoreEmptyLines(true).setHeader().setSkipHeaderRecord(true)
                    .build().parse(new java.io.StringReader(content));
            return parser.getRecords();
        } catch (Exception e) {
            throw new BizException("CSV 解析失败: " + e.getMessage());
        }
    }

    private String decode(byte[] bytes) {
        // 去 BOM
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            bytes = Arrays.copyOfRange(bytes, 3, bytes.length);
        }
        for (Charset cs : List.of(StandardCharsets.UTF_8, Charset.forName("GBK"), Charset.forName("GB18030"))) {
            try {
                return cs.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException ignored) {
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String norm(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFKC).replace(" ", "").replace("\u00a0", "");
    }

    private String get(CSVRecord r, String col) {
        try {
            String v = r.get(col);
            return v == null ? "" : v;
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizeSummary(String s) {
        String t = norm(s);
        if (t.contains("开户")) return "OPEN";
        if (t.contains("缴存")) return "DEPOSIT";
        if (t.contains("退款")) return "REFUND";
        if (t.contains("分红")) return "DIVIDEND";
        if (t.contains("支用") || t.contains("支取") || t.contains("使用")) return "USE";
        return "UNKNOWN";
    }

    private BigDecimal parseAmount(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return new BigDecimal(norm(s).replace("￥", "").replace(",", "").replace("元", "").replace("-", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseArea(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return new BigDecimal(norm(s));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseFloor(String houseNo) {
        try {
            return Integer.parseInt(houseNo.replaceAll("\\D.*", ""));
        } catch (Exception e) {
            return 1;
        }
    }

    private LocalDateTime parseTime(String s) {
        if (s == null || s.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        String t = s.trim();
        for (DateTimeFormatter fmt : TIME_FORMATS) {
            try {
                return LocalDateTime.parse(t, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(t, fmt).atStartOfDay();
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new BizException("交易时间无法解析: " + s);
    }

    private void checkHeaders(List<String> headers) {
        List<String> required = List.of("小区名称", "楼", "单元", "房屋", "业主姓名", "证件号码", "摘要", "交易时间", "交易金额", "发票号");
        List<String> missing = required.stream()
                .filter(h -> headers.stream().noneMatch(x -> norm(x).equals(h)))
                .toList();
        if (!missing.isEmpty()) {
            throw new BizException("文件缺少列: " + String.join(", ", missing));
        }
    }
}
