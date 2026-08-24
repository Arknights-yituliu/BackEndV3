package com.lhs.service.user;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * OpenAPI Token 管理服务
 */
public interface OpenApiService {

    /**
     * 生成第三方API访问token，支持多权限范围，token存储在Redis中，30天过期
     *
     * @param httpServletRequest HTTP请求对象
     * @param scopeCodes         token权限 code 列表，如 [10001] 或 [10001, 10002]
     * @param remark             备注
     * @return API访问token
     */
    String generateOpenApiToken(HttpServletRequest httpServletRequest, List<Integer> scopeCodes, String remark);

    /**
     * 校验第三方API token并返回用户uid
     *
     * @param token        API token
     * @param requiredCode 需要的权限 code，如 10001
     * @return 用户uid
     */
    Long validateOpenApiToken(String token, int requiredCode);

    /**
     * 删除第三方API token
     *
     * @param httpServletRequest HTTP请求对象（用于登录鉴权）
     * @param token              要删除的第三方 token
     */
    void deleteOpenApiToken(HttpServletRequest httpServletRequest, String token);

    /**
     * 获取当前用户生成的所有第三方API token
     *
     * @param httpServletRequest HTTP请求对象
     * @return token列表，每项包含 token、scope、createTime
     */
    List<java.util.Map<String, Object>> listUserTokens(HttpServletRequest httpServletRequest);
}
