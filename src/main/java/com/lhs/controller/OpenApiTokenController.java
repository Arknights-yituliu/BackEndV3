package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.user.OpenApiPermission;
import com.lhs.entity.dto.user.OpenApiTokenRequestDTO;
import com.lhs.service.user.OpenApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三方 API Token 控制器
 * <p>
 * 承载 OpenAPI 第三方令牌的生成、删除、查询与权限列表接口，
 * 接口路径与原 UserController 保持一致，避免破坏既有调用方
 */
@RestController
@Tag(name = "第三方 API Token")
public class OpenApiTokenController {

    private final OpenApiService openApiService;

    public OpenApiTokenController(OpenApiService openApiService) {
        this.openApiService = openApiService;
    }

    @Operation(summary = "生成第三方API Token，scope参数传入权限code数组，如[10001]")
    @PostMapping("/auth/user/open-api/token")
    public Result<HashMap<String, Object>> generateOpenApiToken(
            @RequestBody OpenApiTokenRequestDTO request) {
        String token = openApiService.generateOpenApiToken( request.getScope(), request.getRemark());
        HashMap<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("scope", request.getScope());
        return Result.success(result);
    }

    @Operation(summary = "获取所有可用的 OpenAPI 权限列表")
    @GetMapping("/user/open-api/permissions")
    public Result<List<Map<String, Object>>> listPermissions() {
        return Result.success(OpenApiPermission.listAll());
    }

    @Operation(summary = "删除第三方API Token")
    @PostMapping("/auth/user/open-api/token/delete")
    public Result<Object> deleteOpenApiToken(
            @RequestBody Map<String, String> body) {
        openApiService.deleteOpenApiToken(body.get("token"));
        return Result.success();
    }

    @Operation(summary = "获取当前用户所有第三方API Token")
    @GetMapping("/auth/user/open-api/tokens")
    public Result<List<Map<String, Object>>> listOpenApiTokens() {
        return Result.success(openApiService.listUserTokens());
    }

}
