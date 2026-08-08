package com.wxzj.controller;

import com.wxzj.common.Result;
import com.wxzj.dto.DepositCreateRequest;
import com.wxzj.entity.Deposit;
import com.wxzj.security.LoginUser;
import com.wxzj.service.DepositService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deposit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','PROPERTY')")
public class DepositController {

    private final DepositService service;

    @GetMapping
    public Result<List<Deposit>> list(@RequestParam(required = false) Long communityId,
                                      @RequestParam(required = false) Long houseId,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String type) {
        return Result.ok(service.list(communityId, houseId, status, type));
    }

    @PostMapping
    public Result<Deposit> create(@Valid @RequestBody DepositCreateRequest req,
                                  @AuthenticationPrincipal LoginUser user) {
        return Result.ok(service.create(req.getHouseId(), req.getStandardId(), req.getType(),
                req.getUnitPrice(), req.getRemark(), user));
    }

    @PostMapping("/{id}/confirm")
    public Result<Deposit> confirm(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        return Result.ok(service.confirm(id, user));
    }

    @PostMapping("/{id}/cancel")
    public Result<Deposit> cancel(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        return Result.ok(service.cancel(id, user));
    }
}
