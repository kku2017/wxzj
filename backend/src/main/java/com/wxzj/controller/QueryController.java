package com.wxzj.controller;

import com.wxzj.common.Result;
import com.wxzj.entity.FundFlow;
import com.wxzj.security.LoginUser;
import com.wxzj.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @GetMapping("/flow")
    public Result<List<FundFlow>> flows(@RequestParam(required = false) Long accountId,
                                        @RequestParam(required = false) String type,
                                        @RequestParam(required = false) String direction,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
                                        @AuthenticationPrincipal LoginUser user) {
        return Result.ok(queryService.flows(accountId, type, direction, start, end, user));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY','COMMITTEE')")
    public Result<List<Map<String, Object>>> statistics() {
        return Result.ok(queryService.statistics());
    }
}
