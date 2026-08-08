package com.wxzj.controller;

import com.wxzj.common.Result;
import com.wxzj.entity.*;
import com.wxzj.security.LoginUser;
import com.wxzj.service.BasicDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/basic")
@RequiredArgsConstructor
public class BasicDataController {

    private final BasicDataService service;

    // ---------- 小区 ----------
    @GetMapping("/community")
    public Result<List<Community>> communities(@RequestParam(required = false) String name,
                                               @RequestParam(required = false) String status) {
        return Result.ok(service.listCommunities(name, status));
    }

    @PostMapping("/community")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Community> saveCommunity(@RequestBody Community c) {
        return Result.ok(service.saveCommunity(c));
    }

    @DeleteMapping("/community/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteCommunity(@PathVariable Long id) {
        service.deleteCommunity(id);
        return Result.ok();
    }

    // ---------- 楼栋 ----------
    @GetMapping("/building")
    public Result<List<Building>> buildings(@RequestParam(required = false) Long communityId,
                                            @RequestParam(required = false) String buildingNo) {
        return Result.ok(service.listBuildings(communityId, buildingNo));
    }

    @PostMapping("/building")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Building> saveBuilding(@RequestBody Building b) {
        return Result.ok(service.saveBuilding(b));
    }

    @DeleteMapping("/building/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteBuilding(@PathVariable Long id) {
        service.deleteBuilding(id);
        return Result.ok();
    }

    // ---------- 房屋 ----------
    @GetMapping("/house")
    public Result<List<House>> houses(@RequestParam(required = false) Long communityId,
                                      @RequestParam(required = false) Long buildingId,
                                      @RequestParam(required = false) String houseNo,
                                      @AuthenticationPrincipal LoginUser user) {
        return Result.ok(service.listHouses(communityId, buildingId, houseNo, user));
    }

    @PostMapping("/house")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<House> saveHouse(@RequestBody House h) {
        return Result.ok(service.saveHouse(h));
    }

    @DeleteMapping("/house/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteHouse(@PathVariable Long id) {
        service.deleteHouse(id);
        return Result.ok();
    }

    @PostMapping("/house/{id}/bind-owner")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> bindOwner(@PathVariable Long id, @RequestParam Long ownerId) {
        service.bindOwner(id, ownerId);
        return Result.ok();
    }

    // ---------- 业主 ----------
    @GetMapping("/owner")
    public Result<List<Owner>> owners(@RequestParam(required = false) String name,
                                      @RequestParam(required = false) String phone) {
        return Result.ok(service.listOwners(name, phone));
    }

    @PostMapping("/owner")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Owner> saveOwner(@RequestBody Owner o) {
        return Result.ok(service.saveOwner(o));
    }

    // ---------- 缴存标准 ----------
    @GetMapping("/standard")
    public Result<List<DepositStandard>> standards(@RequestParam(required = false) Long communityId,
                                                   @RequestParam(required = false) String type,
                                                   @RequestParam(required = false) String status) {
        return Result.ok(service.listStandards(communityId, type, status));
    }

    @PostMapping("/standard")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DepositStandard> saveStandard(@RequestBody DepositStandard s) {
        return Result.ok(service.saveStandard(s));
    }

    // ---------- 账户 ----------
    @GetMapping("/account")
    public Result<List<FundAccount>> accounts(@RequestParam(required = false) Long communityId,
                                              @RequestParam(required = false) Long buildingId,
                                              @RequestParam(required = false) String houseNo,
                                              @RequestParam(required = false) Long ownerId,
                                              @RequestParam(required = false) String accountNo,
                                              @AuthenticationPrincipal LoginUser user) {
        return Result.ok(service.listAccounts(communityId, buildingId, houseNo, ownerId, accountNo, user));
    }
}
