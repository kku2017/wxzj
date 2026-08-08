package com.wxzj.controller;

import com.wxzj.common.Result;
import com.wxzj.dto.ApproveRequest;
import com.wxzj.dto.UseApplyCreateRequest;
import com.wxzj.entity.UseApply;
import com.wxzj.entity.UseItem;
import com.wxzj.security.LoginUser;
import com.wxzj.service.UseService;
import com.wxzj.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/use")
@RequiredArgsConstructor
public class UseController {

    private final UseService useService;
    private final WorkflowService workflowService;

    @GetMapping
    public Result<List<UseApply>> list(@RequestParam(required = false) Long communityId,
                                       @RequestParam(required = false) String status) {
        return Result.ok(useService.list(communityId, status));
    }

    @GetMapping("/{id}/items")
    public Result<List<UseItem>> items(@PathVariable Long id) {
        return Result.ok(useService.items(id));
    }

    @GetMapping("/{id}/process")
    public Result<WorkflowService.ProcessView> process(@PathVariable Long id,
                                                       @AuthenticationPrincipal LoginUser user) {
        return Result.ok(workflowService.view(WorkflowService.BIZ_USE, id, user));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY')")
    public Result<UseApply> create(@Valid @RequestBody UseApplyCreateRequest req,
                                   @AuthenticationPrincipal LoginUser user) {
        return Result.ok(useService.create(req, user));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY')")
    public Result<UseApply> submit(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        return Result.ok(useService.submit(id, user));
    }

    @PostMapping("/{id}/approve")
    public Result<WorkflowService.ApprovalResult> approve(@PathVariable Long id,
                                                          @Valid @RequestBody ApproveRequest req,
                                                          @AuthenticationPrincipal LoginUser user) {
        return Result.ok(useService.approve(id, req.getAction(), req.getOpinion(), user));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UseApply> pay(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        return Result.ok(useService.pay(id, user));
    }
}
