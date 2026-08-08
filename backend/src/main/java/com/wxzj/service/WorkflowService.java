package com.wxzj.service;

import com.wxzj.common.BizException;
import com.wxzj.entity.Approval;
import com.wxzj.entity.FlowDef;
import com.wxzj.entity.FlowInstance;
import com.wxzj.entity.FlowNode;
import com.wxzj.repository.ApprovalRepository;
import com.wxzj.repository.FlowDefRepository;
import com.wxzj.repository.FlowInstanceRepository;
import com.wxzj.repository.FlowNodeRepository;
import com.wxzj.security.LoginUser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    public static final String BIZ_USE = "USE";
    public static final String BIZ_REFUND = "REFUND";

    private final FlowDefRepository flowDefRepository;
    private final FlowNodeRepository flowNodeRepository;
    private final FlowInstanceRepository flowInstanceRepository;
    private final ApprovalRepository approvalRepository;

    @Data
    public static class ProcessView {
        private FlowInstance instance;
        private List<FlowNode> nodes;
        private List<Approval> approvals;
        private FlowNode currentNode;
        private boolean canApprove;
    }

    @Data
    public static class ApprovalResult {
        private String instanceStatus;
        private Integer currentNodeNo;
        private boolean lastPassed;
    }

    /** 启动一个流程实例（置于节点 1） */
    @Transactional
    public FlowInstance start(String bizType, Long bizId) {
        FlowDef def = flowDefRepository.findByCode(bizType)
                .orElseThrow(() -> new BizException("流程定义不存在: " + bizType));
        List<FlowNode> nodes = flowNodeRepository.findByFlowDefIdOrderByNodeNoAsc(def.getId());
        if (nodes.isEmpty()) {
            throw new BizException("流程未配置节点: " + bizType);
        }
        FlowInstance inst = flowInstanceRepository.findByBizTypeAndBizId(bizType, bizId).orElse(null);
        if (inst == null) {
            inst = new FlowInstance();
            inst.setFlowDefId(def.getId());
            inst.setBizType(bizType);
            inst.setBizId(bizId);
            inst.setStatus("RUNNING");
            inst.setStartTime(LocalDateTime.now());
        }
        inst.setCurrentNodeNo(nodes.get(0).getNodeNo());
        inst.setStatus("RUNNING");
        inst.setEndTime(null);
        return flowInstanceRepository.save(inst);
    }

    /** 审批当前节点 */
    @Transactional
    public ApprovalResult approve(String bizType, Long bizId, LoginUser user, String action, String opinion) {
        if (!"PASS".equals(action) && !"REJECT".equals(action)) {
            throw new BizException("非法的审批动作");
        }
        FlowInstance inst = flowInstanceRepository.findByBizTypeAndBizId(bizType, bizId)
                .orElseThrow(() -> new BizException("流程实例不存在"));
        if (!"RUNNING".equals(inst.getStatus())) {
            throw new BizException("流程已结束，不能审批");
        }
        FlowDef def = flowDefRepository.findById(inst.getFlowDefId()).orElseThrow();
        List<FlowNode> nodes = flowNodeRepository.findByFlowDefIdOrderByNodeNoAsc(def.getId());
        FlowNode currentNode = nodes.stream()
                .filter(n -> n.getNodeNo().equals(inst.getCurrentNodeNo()))
                .findFirst()
                .orElseThrow(() -> new BizException("当前节点不存在"));
        checkPermission(currentNode, user);

        Approval ap = new Approval();
        ap.setFlowInstanceId(inst.getId());
        ap.setBizType(bizType);
        ap.setBizId(bizId);
        ap.setNodeNo(currentNode.getNodeNo());
        ap.setNodeName(currentNode.getNodeName());
        ap.setApproverId(user.getId());
        ap.setApproverName(user.getRealName());
        ap.setAction(action);
        ap.setOpinion(opinion);
        ap.setTime(LocalDateTime.now());
        approvalRepository.save(ap);

        ApprovalResult result = new ApprovalResult();
        int maxNo = nodes.get(nodes.size() - 1).getNodeNo();
        if ("REJECT".equals(action)) {
            inst.setStatus("TERMINATED");
            inst.setEndTime(LocalDateTime.now());
            result.setInstanceStatus("TERMINATED");
            result.setCurrentNodeNo(currentNode.getNodeNo());
        } else if (currentNode.getNodeNo() >= maxNo) {
            inst.setStatus("COMPLETED");
            inst.setEndTime(LocalDateTime.now());
            result.setInstanceStatus("COMPLETED");
            result.setCurrentNodeNo(currentNode.getNodeNo());
            result.setLastPassed(true);
        } else {
            int next = nodes.get(nodes.indexOf(currentNode) + 1).getNodeNo();
            inst.setCurrentNodeNo(next);
            result.setInstanceStatus("RUNNING");
            result.setCurrentNodeNo(next);
        }
        flowInstanceRepository.save(inst);
        return result;
    }

    private void checkPermission(FlowNode node, LoginUser user) {
        if (user.isAdmin()) {
            return;
        }
        if (!node.getApproverRole().equalsIgnoreCase(user.getRole())) {
            throw new BizException("当前节点需要角色 [" + node.getApproverRole() + "] 审批");
        }
    }

    /** 流程视图（实例/节点/审批记录/是否可审批） */
    public ProcessView view(String bizType, Long bizId, LoginUser user) {
        ProcessView v = new ProcessView();
        FlowInstance inst = flowInstanceRepository.findByBizTypeAndBizId(bizType, bizId).orElse(null);
        v.setInstance(inst);
        if (inst != null) {
            FlowDef def = flowDefRepository.findById(inst.getFlowDefId()).orElse(null);
            if (def != null) {
                v.setNodes(flowNodeRepository.findByFlowDefIdOrderByNodeNoAsc(def.getId()));
            }
            v.setApprovals(approvalRepository.findByBizTypeAndBizIdOrderByNodeNoAsc(bizType, bizId));
            v.setCurrentNode(v.getNodes() == null ? null : v.getNodes().stream()
                    .filter(n -> n.getNodeNo().equals(inst.getCurrentNodeNo()))
                    .findFirst().orElse(null));
            if (v.getCurrentNode() != null && "RUNNING".equals(inst.getStatus())) {
                v.setCanApprove(user.isAdmin()
                        || v.getCurrentNode().getApproverRole().equalsIgnoreCase(user.getRole()));
            }
        }
        return v;
    }

    public Map<String, List<FlowNode>> definitions() {
        List<FlowDef> defs = flowDefRepository.findAll();
        java.util.LinkedHashMap<String, List<FlowNode>> map = new java.util.LinkedHashMap<>();
        for (FlowDef d : defs) {
            map.put(d.getCode(), flowNodeRepository.findByFlowDefIdOrderByNodeNoAsc(d.getId()));
        }
        return map;
    }
}
