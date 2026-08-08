package com.wxzj.controller;

import com.wxzj.common.Result;
import com.wxzj.dto.MigratePreview;
import com.wxzj.dto.MigrateReport;
import com.wxzj.security.LoginUser;
import com.wxzj.service.MigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/migrate")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','PROPERTY')")
public class MigrationController {

    private final MigrationService service;

    @PostMapping("/preview")
    public Result<MigratePreview> preview(@RequestParam("file") MultipartFile file) {
        return Result.ok(service.preview(file));
    }

    @PostMapping("/execute")
    public Result<MigrateReport> execute(@RequestParam("file") MultipartFile file,
                                         @AuthenticationPrincipal LoginUser user) {
        return Result.ok(service.migrate(file, user));
    }
}
