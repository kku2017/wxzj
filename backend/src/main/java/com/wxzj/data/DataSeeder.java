package com.wxzj.data;

import com.wxzj.entity.*;
import com.wxzj.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final BuildingRepository buildingRepository;
    private final HouseRepository houseRepository;
    private final OwnerRepository ownerRepository;
    private final FundAccountRepository fundAccountRepository;
    private final DepositStandardRepository depositStandardRepository;
    private final FlowDefRepository flowDefRepository;
    private final FlowNodeRepository flowNodeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedFlowDefs();
        seedDemoData();
        log.info("演示数据初始化完成");
    }

    private void seedUsers() {
        createUser("admin", "admin123", "系统管理员", "13800000001", "ADMIN", null);
        createUser("property", "property123", "物业小王", "13800000002", "PROPERTY", null);
        createUser("committee", "committee123", "业委会张叔", "13800000003", "COMMITTEE", null);
        User ownerUser = createUser("owner", "owner123", "业主李女士", "13800000004", "OWNER", null);
        if (!ownerRepository.existsByIdCard("110101198801011234")) {
            Owner owner = ownerRepository.save(createOwner("李女士", "110101198801011234", "13900000004", "女"));
            ownerUser.setOwnerId(owner.getId());
            userRepository.save(ownerUser);
        }
        log.info("默认账号：admin/property/committee/owner，密码分别为 ...123");
    }

    private User createUser(String username, String rawPassword, String realName, String phone, String role, Long ownerId) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setPassword(passwordEncoder.encode(rawPassword));
            u.setRealName(realName);
            u.setPhone(phone);
            u.setRole(role);
            u.setOwnerId(ownerId);
            return userRepository.save(u);
        });
    }

    private void seedFlowDefs() {
        seedFlow("USE", "资金使用审批流程",
                List.of(new String[]{"1", "业委会审核", "COMMITTEE"}, new String[]{"2", "主管部门审批", "ADMIN"}));
        seedFlow("REFUND", "资金退款审批流程",
                List.of(new String[]{"1", "物业核对", "PROPERTY"}, new String[]{"2", "主管部门审批", "ADMIN"}));
    }

    private void seedFlow(String code, String name, List<String[]> nodes) {
        FlowDef def = flowDefRepository.findByCode(code).orElse(null);
        if (def == null) {
            def = new FlowDef();
            def.setCode(code);
            def.setName(name);
            def = flowDefRepository.save(def);
        }
        List<FlowNode> existing = flowNodeRepository.findByFlowDefIdOrderByNodeNoAsc(def.getId());
        if (existing.isEmpty()) {
            for (String[] n : nodes) {
                FlowNode node = new FlowNode();
                node.setFlowDefId(def.getId());
                node.setNodeNo(Integer.parseInt(n[0]));
                node.setNodeName(n[1]);
                node.setApproverRole(n[2]);
                flowNodeRepository.save(node);
            }
        }
    }

    private void seedDemoData() {
        if (communityRepository.count() > 0) {
            return;
        }
        Community c1 = new Community();
        c1.setName("阳光花园");
        c1.setAddress("市中心幸福路 1 号");
        c1.setDeveloper("城市房地产开发有限公司");
        c1.setBuildYear(2015);
        c1.setArea(new BigDecimal("80000"));
        c1 = communityRepository.save(c1);

        Community c2 = new Community();
        c2.setName("幸福里小区");
        c2.setAddress("南城新区人民大道 88 号");
        c2.setDeveloper("幸福置业");
        c2.setBuildYear(2020);
        c2.setArea(new BigDecimal("120000"));
        c2 = communityRepository.save(c2);

        DepositStandard s1 = new DepositStandard();
        s1.setCommunityId(c1.getId());
        s1.setName("阳光花园初始缴存标准");
        s1.setUnitPrice(new BigDecimal("80"));
        s1.setType("INITIAL");
        s1.setEffectiveDate(LocalDate.of(2015, 6, 1));
        depositStandardRepository.save(s1);

        DepositStandard s2 = new DepositStandard();
        s2.setCommunityId(c2.getId());
        s2.setName("幸福里初始缴存标准");
        s2.setUnitPrice(new BigDecimal("100"));
        s2.setType("INITIAL");
        s2.setEffectiveDate(LocalDate.of(2020, 1, 1));
        depositStandardRepository.save(s2);

        Owner o1 = ownerRepository.save(createOwner("王先生", "110101197501012345", "13800000011", "男"));
        Owner o2 = ownerRepository.save(createOwner("刘女士", "110101198002025678", "13800000012", "女"));
        Owner li = ownerRepository.findAll().stream()
                .filter(o -> "110101198801011234".equals(o.getIdCard()))
                .findFirst().orElse(null);

        seedBuilding(c1, "1", 11, "20000", bd("95.5", "120.0", "88.0"),
                new Long[]{o1.getId(), o2.getId(), o1.getId()});
        seedBuilding(c1, "2", 18, "35000", bd("105.0", "96.5", "130.0"),
                new Long[]{o2.getId(), o1.getId(), o2.getId()});
        seedBuilding(c2, "A", 26, "50000", bd("110.0", "88.8"),
                new Long[]{li != null ? li.getId() : o1.getId(), o2.getId()});
        seedBuilding(c2, "B", 30, "60000", bd("125.0", "99.0"),
                new Long[]{o2.getId(), o1.getId()});
    }

    private void seedBuilding(Community c, String no, int floors, String area, BigDecimal[] areas, Long[] ownerIds) {
        Building b = new Building();
        b.setCommunityId(c.getId());
        b.setBuildingNo(no);
        b.setName(no + "号楼");
        b.setFloors(floors);
        b.setArea(new BigDecimal(area));
        b = buildingRepository.save(b);

        for (int i = 0; i < areas.length; i++) {
            House h = new House();
            h.setCommunityId(c.getId());
            h.setBuildingId(b.getId());
            h.setHouseNo(no + "-10" + (i + 1));
            h.setFloor(1);
            h.setArea(areas[i]);
            h.setStatus("ACTIVE");
            h.setOwnerId(ownerIds[i % ownerIds.length]);
            h = houseRepository.save(h);

            FundAccount acc = new FundAccount();
            acc.setAccountNo("WX" + String.format("%06d", h.getId()));
            acc.setHouseId(h.getId());
            acc.setOwnerId(h.getOwnerId());
            acc.setCommunityId(c.getId());
            fundAccountRepository.save(acc);
        }
        c.setHouseCount((int) houseRepository.countByCommunityId(c.getId()));
        communityRepository.save(c);
    }

    private BigDecimal[] bd(String... vals) {
        BigDecimal[] arr = new BigDecimal[vals.length];
        for (int i = 0; i < vals.length; i++) {
            arr[i] = new BigDecimal(vals[i]);
        }
        return arr;
    }

    private Owner createOwner(String name, String idCard, String phone, String gender) {
        Owner o = new Owner();
        o.setName(name);
        o.setIdCard(idCard);
        o.setPhone(phone);
        o.setGender(gender);
        o.setAddress(name + "房产");
        return o;
    }
}
