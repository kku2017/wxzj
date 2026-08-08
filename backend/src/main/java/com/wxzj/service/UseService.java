package com.wxzj.service;

import com.wxzj.common.BizException;
import com.wxzj.dto.UseApplyCreateRequest;
import com.wxzj.entity.*;
import com.wxzj.repository.*;
import com.wxzj.security.LoginUser;
import com.wxzj.service.WorkflowService.ApprovalResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UseService {

    private final UseApplyRepository useApplyRepository;
    private final UseItemRepository useItemRepository;
    private final UsePaymentRepository usePaymentRepository;
    private final HouseRepository houseRepository;
    private final FundAccountRepository fundAccountRepository;
    private final FundFlowRepository fundFlowRepository;
    private final WorkflowService workflowService;

    public List<UseApply> list(Long communityId, String status) {
        return useApplyRepository.findAll((root, q, cb) -> {
            var p = cb.conjunction();
            if (communityId != null) {
                p = cb.and(p, cb.equal(root.get("communityId"), communityId));
            }
            if (StringUtils.hasText(status)) {
                p = cb.and(p, cb.equal(root.get("status"), status));
            }
            return p;
        }, Sort.by(Sort.Direction.DESC, "id"));
    }

    public List<UseItem> items(Long useApplyId) {
        return useItemRepository.findByUseApplyIdOrderByIdAsc(useApplyId);
    }

    /** 提交申请（草稿），按面积分摊到户 */
    @Transactional
    public UseApply create(UseApplyCreateRequest req, LoginUser user) {
        if (req.getHouseIds() == null || req.getHouseIds().isEmpty()) {
            throw new BizException("请选择涉及房屋");
        }
        List<House> houses = req.getHouseIds().stream()
                .map(id -> houseRepository.findById(id).orElseThrow(() -> new BizException("房屋不存在: " + id)))
                .toList();

        UseApply apply = new UseApply();
        apply.setApplyNo(com.wxzj.common.NoGenerator.gen("SY"));
        apply.setCommunityId(req.getCommunityId());
        apply.setTitle(req.getTitle());
        apply.setReason(req.getReason());
        apply.setItemDesc(req.getItemDesc());
        apply.setTotalAmount(req.getTotalAmount());
        apply.setStatus("DRAFT");
        apply.setApplyUserId(user.getId());
        apply.setRemark(req.getRemark());
        UseApply saved = useApplyRepository.save(apply);

        BigDecimal shareArea = houses.stream().map(House::getArea).reduce(BigDecimal.ZERO, BigDecimal::add);
        apply.setShareArea(shareArea);
        useApplyRepository.save(apply);

        List<UseItem> items = new ArrayList<>();
        for (House h : houses) {
            FundAccount acc = fundAccountRepository.findByHouseId(h.getId())
                    .orElseThrow(() -> new BizException("房屋未开户: " + h.getHouseNo()));
            BigDecimal amount = req.getTotalAmount().multiply(h.getArea()).divide(shareArea, 2, RoundingMode.HALF_UP);
            UseItem item = new UseItem();
            item.setUseApplyId(saved.getId());
            item.setHouseId(h.getId());
            item.setAccountId(acc.getId());
            item.setShareArea(h.getArea());
            item.setShareAmount(amount);
            items.add(item);
        }
        useItemRepository.saveAll(items);
        return saved;
    }

    @Transactional
    public UseApply submit(Long id, LoginUser user) {
        UseApply apply = getApply(id);
        if (!"DRAFT".equals(apply.getStatus())) {
            throw new BizException("仅草稿可提交审批");
        }
        apply.setStatus("PENDING");
        apply.setApplyTime(LocalDateTime.now());
        useApplyRepository.save(apply);
        workflowService.start(WorkflowService.BIZ_USE, id);
        return apply;
    }

    /** 审批：PASS 通过 / REJECT 拒绝（流程驱动） */
    @Transactional
    public ApprovalResult approve(Long id, String action, String opinion, LoginUser user) {
        UseApply apply = getApply(id);
        if (!"PENDING".equals(apply.getStatus())) {
            throw new BizException("仅审批中可操作");
        }
        ApprovalResult r = workflowService.approve(WorkflowService.BIZ_USE, id, user, action, opinion);
        if ("COMPLETED".equals(r.getInstanceStatus())) {
            apply.setStatus("APPROVED");
        } else if ("TERMINATED".equals(r.getInstanceStatus())) {
            apply.setStatus("REJECTED");
            apply.setFinishTime(LocalDateTime.now());
        }
        useApplyRepository.save(apply);
        return r;
    }

    /** 拨付完结：按分摊明细扣减各户余额并生成使用流水 */
    @Transactional
    public UseApply pay(Long id, LoginUser user) {
        UseApply apply = getApply(id);
        if (!"APPROVED".equals(apply.getStatus())) {
            throw new BizException("仅已批准状态可拨付");
        }
        List<UseItem> items = useItemRepository.findByUseApplyIdOrderByIdAsc(id);
        for (UseItem item : items) {
            if ("PAID".equals(item.getStatus())) {
                continue;
            }
            FundAccount acc = fundAccountRepository.findById(item.getAccountId())
                    .orElseThrow(() -> new BizException("账户不存在"));
            if (acc.getBalance().compareTo(item.getShareAmount()) < 0) {
                throw new BizException("账户余额不足（房号账户）");
            }
            acc.setBalance(acc.getBalance().subtract(item.getShareAmount()));
            acc.setTotalUsed(acc.getTotalUsed().add(item.getShareAmount()));
            fundAccountRepository.save(acc);
            item.setStatus("PAID");
            useItemRepository.save(item);

            FundFlow f = new FundFlow();
            f.setFlowNo(com.wxzj.common.NoGenerator.gen("LS"));
            f.setAccountId(acc.getId());
            f.setHouseId(acc.getHouseId());
            f.setCommunityId(acc.getCommunityId());
            f.setType("USE");
            f.setDirection("OUT");
            f.setAmount(item.getShareAmount());
            f.setBalance(acc.getBalance());
            f.setRelatedNo(apply.getApplyNo());
            f.setOperatorId(user.getId());
            f.setBizTime(LocalDateTime.now());
            f.setRemark("使用拨付：" + apply.getTitle());
            fundFlowRepository.save(f);
        }

        UsePayment payment = new UsePayment();
        payment.setUseApplyId(id);
        payment.setAmount(apply.getTotalAmount());
        payment.setPayTime(LocalDateTime.now());
        payment.setOperatorId(user.getId());
        payment.setRemark("拨付完结");
        usePaymentRepository.save(payment);

        apply.setStatus("PAID");
        apply.setFinishTime(LocalDateTime.now());
        return useApplyRepository.save(apply);
    }

    public UseApply getApply(Long id) {
        return useApplyRepository.findById(id).orElseThrow(() -> new BizException("使用申请不存在"));
    }
}
