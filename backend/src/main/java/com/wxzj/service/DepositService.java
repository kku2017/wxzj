package com.wxzj.service;

import com.wxzj.common.BizException;
import com.wxzj.entity.*;
import com.wxzj.repository.*;
import com.wxzj.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepositService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final DepositRepository depositRepository;
    private final HouseRepository houseRepository;
    private final FundAccountRepository fundAccountRepository;
    private final DepositStandardRepository depositStandardRepository;
    private final FundFlowRepository fundFlowRepository;

    public List<Deposit> list(Long communityId, Long houseId, String status, String type) {
        return depositRepository.findAll((root, q, cb) -> {
            var p = cb.conjunction();
            if (communityId != null) {
                p = cb.and(p, cb.equal(root.get("communityId"), communityId));
            }
            if (houseId != null) {
                p = cb.and(p, cb.equal(root.get("houseId"), houseId));
            }
            if (StringUtils.hasText(status)) {
                p = cb.and(p, cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(type)) {
                p = cb.and(p, cb.equal(root.get("type"), type));
            }
            return p;
        }, Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional
    public Deposit create(Long houseId, Long standardId, String type, BigDecimal customUnitPrice,
                          String remark, LoginUser user) {
        House house = houseRepository.findById(houseId).orElseThrow(() -> new BizException("房屋不存在"));
        FundAccount account = fundAccountRepository.findByHouseId(houseId)
                .orElseThrow(() -> new BizException("该房屋未开户，请先在基础数据中登记房屋"));

        BigDecimal unitPrice = customUnitPrice;
        if (unitPrice == null) {
            if (standardId == null) {
                List<DepositStandard> standards = depositStandardRepository
                        .findByCommunityIdAndStatusOrderByEffectiveDateDesc(house.getCommunityId(), "ACTIVE");
                if (standards.isEmpty()) {
                    throw new BizException("未配置缴存标准，请先维护缴存标准");
                }
                standardId = standards.get(0).getId();
                unitPrice = standards.get(0).getUnitPrice();
            } else {
                DepositStandard s = depositStandardRepository.findById(standardId)
                        .orElseThrow(() -> new BizException("缴存标准不存在"));
                unitPrice = s.getUnitPrice();
            }
        }

        BigDecimal quantity = house.getArea();
        BigDecimal amount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

        Deposit d = new Deposit();
        d.setOrderNo(com.wxzj.common.NoGenerator.gen("CJ"));
        d.setCommunityId(house.getCommunityId());
        d.setHouseId(houseId);
        d.setAccountId(account.getId());
        d.setStandardId(standardId);
        d.setOwnerId(house.getOwnerId());
        d.setType(type == null ? "INITIAL" : type);
        d.setQuantity(quantity);
        d.setUnitPrice(unitPrice);
        d.setAmount(amount);
        d.setOperatorId(user.getId());
        d.setRemark(remark);
        return depositRepository.save(d);
    }

    @Transactional
    public Deposit confirm(Long id, LoginUser user) {
        Deposit d = depositRepository.findById(id).orElseThrow(() -> new BizException("缴存单不存在"));
        if (!"PENDING".equals(d.getStatus())) {
            throw new BizException("仅待缴状态可确认到账");
        }
        FundAccount acc = fundAccountRepository.findById(d.getAccountId())
                .orElseThrow(() -> new BizException("账户不存在"));
        d.setStatus("PAID");
        d.setPayTime(LocalDateTime.now());
        depositRepository.save(d);

        acc.setBalance(acc.getBalance().add(d.getAmount()));
        acc.setTotalDeposit(acc.getTotalDeposit().add(d.getAmount()));
        fundAccountRepository.save(acc);

        saveFlow(acc, "DEPOSIT", "IN", d.getAmount(), d.getOrderNo(), user, "缴存到账");
        return d;
    }

    @Transactional
    public Deposit cancel(Long id, LoginUser user) {
        Deposit d = depositRepository.findById(id).orElseThrow(() -> new BizException("缴存单不存在"));
        if (!"PENDING".equals(d.getStatus())) {
            throw new BizException("仅待缴状态可作废");
        }
        d.setStatus("CANCELLED");
        d.setRemark((d.getRemark() == null ? "" : d.getRemark()) + " [已作废]");
        return depositRepository.save(d);
    }

    private void saveFlow(FundAccount acc, String type, String direction, BigDecimal amount,
                          String relatedNo, LoginUser user, String remark) {
        FundFlow f = new FundFlow();
        f.setFlowNo(com.wxzj.common.NoGenerator.gen("LS"));
        f.setAccountId(acc.getId());
        f.setHouseId(acc.getHouseId());
        f.setCommunityId(acc.getCommunityId());
        f.setType(type);
        f.setDirection(direction);
        f.setAmount(amount);
        f.setBalance(acc.getBalance());
        f.setRelatedNo(relatedNo);
        f.setOperatorId(user.getId());
        f.setBizTime(LocalDateTime.now());
        f.setRemark(remark);
        fundFlowRepository.save(f);
    }
}
