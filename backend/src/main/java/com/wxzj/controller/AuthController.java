package com.wxzj.controller;

import com.wxzj.common.Result;
import com.wxzj.dto.LoginRequest;
import com.wxzj.dto.LoginResponse;
import com.wxzj.security.LoginUser;
import com.wxzj.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @GetMapping("/me")
    public Result<LoginUser> me(@AuthenticationPrincipal LoginUser user) {
        return Result.ok(user);
    }
}
