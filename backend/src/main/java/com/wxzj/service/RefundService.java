package com.wxzj.service;

import com.wxzj.common.BizException;
import com.wxzj.dto.RefundApplyCreateRequest;
import com.wxzj.entity.FundAccount;
import com.wxzj.entity.FundFlow;
import com.wxzj.entity.RefundApply;
import com.wxzj.repository.FundAccountRepository;
import com.wxzj.repository.FundFlowRepository;
import com.wxzj.repository.RefundApplyRepository;
import com.wxzj.security.LoginUser;
import com.wxzj.service.WorkflowService.ApprovalResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundApplyRepository refundApplyRepository;
    private final FundAccountRepository fundAccountRepository;
    private final FundFlowRepository fundFlowRepository;
    private final WorkflowService workflowService;

    public List<RefundApply> list(Long communityId, String status, LoginUser user) {
        return refundApplyRepository.findAll((root, q, cb) -> {
            var p = cb.conjunction();
            if (communityId != null) {
                p = cb.and(p, cb.equal(root.get("communityId"), communityId));
            }
            if (StringUtils.hasText(status)) {
                p = cb.and(p, cb.equal(root.get("status"), status));
            }
            if (user != null && user.isOwner()) {
                p = cb.and(p, cb.equal(root.get("ownerId"), user.getOwnerId()));
            }
            return p;
        }, Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional
    public RefundApply create(RefundApplyCreateRequest req, LoginUser user) {
        FundAccount acc = fundAccountRepository.findById(req.getAccountId())
                .orElseThrow(() -> new BizException("资金账户不存在"));
        if (user.isOwner() && !user.getOwnerId().equals(acc.getOwnerId())) {
            throw new BizException("无权操作他人账户");
        }
        if (req.getAmount() == null || req.getAmount().signum() <= 0) {
            throw new BizException("退款金额必须大于 0");
        }
        if (acc.getBalance().compareTo(req.getAmount()) < 0) {
            throw new BizException("退款金额不能超过账户余额");
        }
        RefundApply r = new RefundApply();
        r.setRefundNo(com.wxzj.common.NoGenerator.gen("TK"));
        r.setCommunityId(acc.getCommunityId());
        r.setHouseId(acc.getHouseId());
        r.setAccountId(acc.getId());
        r.setOwnerId(acc.getOwnerId());
        r.setReason(req.getReason());
        r.setAmount(req.getAmount());
        r.setBalanceAtApply(acc.getBalance());
        r.setStatus("PENDING");
        r.setApplyUserId(user.getId());
        r.setApplyTime(LocalDateTime.now());
        r.setRemark(req.getRemark());
        RefundApply saved = refundApplyRepository.save(r);
        workflowService.start(WorkflowService.BIZ_REFUND, saved.getId());
        return saved;
    }

    @Transactional
    public ApprovalResult approve(Long id, String action, String opinion, LoginUser user) {
        RefundApply r = getApply(id);
        if (!"PENDING".equals(r.getStatus())) {
            throw new BizException("仅审批中可操作");
        }
        ApprovalResult result = workflowService.approve(WorkflowService.BIZ_REFUND, id, user, action, opinion);
        if ("COMPLETED".equals(result.getInstanceStatus())) {
            r.setStatus("APPROVED");
        } else if ("TERMINATED".equals(result.getInstanceStatus())) {
            r.setStatus("REJECTED");
            r.setFinishTime(LocalDateTime.now());
        }
        refundApplyRepository.save(r);
        return result;
    }

    /** 退款完结：扣减余额并生成退款流水 */
    @Transactional
    public RefundApply confirm(Long id, LoginUser user) {
        RefundApply r = getApply(id);
        if (!"APPROVED".equals(r.getStatus())) {
            throw new BizException("仅已批准状态可办理退款");
        }
        FundAccount acc = fundAccountRepository.findById(r.getAccountId())
                .orElseThrow(() -> new BizException("账户不存在"));
        if (acc.getBalance().compareTo(r.getAmount()) < 0) {
            throw new BizException("账户余额不足，无法退款");
        }
        acc.setBalance(acc.getBalance().subtract(r.getAmount()));
        acc.setTotalRefund(acc.getTotalRefund().add(r.getAmount()));
        fundAccountRepository.save(acc);

        FundFlow f = new FundFlow();
        f.setFlowNo(com.wxzj.common.NoGenerator.gen("LS"));
        f.setAccountId(acc.getId());
        f.setHouseId(acc.getHouseId());
        f.setCommunityId(acc.getCommunityId());
        f.setType("REFUND");
        f.setDirection("OUT");
        f.setAmount(r.getAmount());
        f.setBalance(acc.getBalance());
        f.setRelatedNo(r.getRefundNo());
        f.setOperatorId(user.getId());
        f.setBizTime(LocalDateTime.now());
        f.setRemark("退款：" + reasonText(r.getReason()));
        fundFlowRepository.save(f);

        r.setStatus("REFUNDED");
        r.setFinishTime(LocalDateTime.now());
        return refundApplyRepository.save(r);
    }

    public RefundApply getApply(Long id) {
        return refundApplyRepository.findById(id).orElseThrow(() -> new BizException("退款申请不存在"));
    }

    private String reasonText(String reason) {
        return switch (reason == null ? "" : reason) {
            case "TRANSFER" -> "产权转移";
            case "DEMOLITION" -> "房屋灭失";
            case "OVERPAY" -> "多缴误缴";
            default -> reason;
        };
    }
}
