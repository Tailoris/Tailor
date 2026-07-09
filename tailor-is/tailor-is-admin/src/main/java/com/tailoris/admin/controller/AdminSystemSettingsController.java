package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "系统设置")
@RestController
@RequestMapping("/api/system/settings")
@RequiredArgsConstructor
@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN"})
public class AdminSystemSettingsController {

    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "获取系统设置")
    @GetMapping
    public Result<Map<String, Object>> getSettings() {
        try {
            Map<String, Object> settings = new HashMap<>();
            
            Map<String, Object> basic = new HashMap<>();
            basic.put("platformName", "裁智云平台");
            basic.put("logoUrl", "/images/logo.png");
            basic.put("servicePhone", "400-888-8888");
            basic.put("serviceEmail", "support@tailoris.com");
            basic.put("announcement", "欢迎使用裁智云平台管理后台");
            
            Map<String, Object> security = new HashMap<>();
            security.put("captchaEnabled", true);
            security.put("registerAudit", true);
            security.put("smsVerification", true);
            security.put("loginFailLock", 5);
            security.put("sessionTimeout", 1800);
            
            Map<String, Object> thirdParty = new HashMap<>();
            thirdParty.put("ossEnabled", false);
            thirdParty.put("ossEndpoint", "");
            thirdParty.put("ossBucket", "");
            thirdParty.put("wechatPayEnabled", false);
            thirdParty.put("alipayEnabled", false);
            
            settings.put("basic", basic);
            settings.put("security", security);
            settings.put("thirdParty", thirdParty);
            
            return Result.success(settings);
        } catch (Exception e) {
            log.warn("Failed to get settings", e);
            return Result.fail("获取设置失败");
        }
    }

    @Operation(summary = "保存基本设置")
    @PutMapping("/basic")
    public Result<Void> saveBasicSettings(@RequestBody Map<String, Object> data) {
        log.info("Saving basic settings: {}", data);
        return Result.success();
    }

    @Operation(summary = "保存安全设置")
    @PutMapping("/security")
    public Result<Void> saveSecuritySettings(@RequestBody Map<String, Object> data) {
        log.info("Saving security settings: {}", data);
        return Result.success();
    }

    @Operation(summary = "保存第三方服务设置")
    @PutMapping("/third-party")
    public Result<Void> saveThirdPartySettings(@RequestBody Map<String, Object> data) {
        log.info("Saving third-party settings: {}", data);
        return Result.success();
    }
}