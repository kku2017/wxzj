package com.wxzj.controller;

import com.wxzj.common.Result;
import com.wxzj.dto.ApproveRequest;
import com.wxzj.dto.RefundApplyCreateRequest;
import com.wxzj.entity.RefundApply;
import com.wxzj.security.LoginUser;
import com.wxzj.service.RefundService;
import com.wxzj.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/refund")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final WorkflowService workflowService;

    @GetMapping
    public Result<List<RefundApply>> list(@RequestParam(required = false) Long communityId,
                                          @RequestParam(required = false) String status,
                                          @AuthenticationPrincipal LoginUser user) {
        return Result.ok(refundService.list(communityId, status, user));
    }

    @GetMapping("/{id}/process")
    public Result<WorkflowService.ProcessView> process(@PathVariable Long id,
                                                       @AuthenticationPrincipal LoginUser user) {
        return Result.ok(workflowService.view(WorkflowService.BIZ_REFUND, id, user));
    }

    @PostMapping
    public Result<RefundApply> create(@Valid @RequestBody RefundApplyCreateRequest req,
                                      @AuthenticationPrincipal LoginUser user) {
        return Result.ok(refundService.create(req, user));
    }

    @PostMapping("/{id}/approve")
    public Result<WorkflowService.ApprovalResult> approve(@PathVariable Long id,
                                                          @Valid @RequestBody ApproveRequest req,
                                                          @AuthenticationPrincipal LoginUser user) {
        return Result.ok(refundService.approve(id, req.getAction(), req.getOpinion(), user));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<RefundApply> confirm(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        return Result.ok(refundService.confirm(id, user));
    }
}
