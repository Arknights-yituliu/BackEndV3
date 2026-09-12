package com.lhs.controller;

import com.lhs.common.enums.ResultCode;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.RedisKeyUtil;
import com.lhs.common.util.RedisRateLimiter;
import com.lhs.common.util.Result;
import com.lhs.entity.dto.user.DirectLoginSessionVO;
import com.lhs.entity.vo.user.LoginSessionVO;
import com.lhs.service.user.DirectLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * UC 直连登录接口（方案 B：保持旧系统登录页）
 * <p>
 * 前端先调用 /legacy/direct-session 获取 channel，将登录/注册凭证直接提交 UC
 * （/oauth2/direct-login、/oauth2/direct-register）换取 ticket，
 * 再调用 /legacy/complete-login 由后端凭 ticket 兑换用户信息并发自家会话
 */
@RestController
@Tag(name = "直连登录")
public class DirectLoginController {

    private static final long DIRECT_SESSION_IP_LIMIT = 20;
    private static final long DIRECT_SESSION_FINGERPRINT_LIMIT = 8;
    private static final long DIRECT_SESSION_RATE_WINDOW_SECONDS = 60;

    private final DirectLoginService directLoginService;
    private final RedisTemplate<String, String> redisTemplate;

    public DirectLoginController(DirectLoginService directLoginService, RedisTemplate<String, String> redisTemplate) {
        this.directLoginService = directLoginService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 发起直连登录会话：后端以 client_secret 换 channel 下发前端（打开登录/注册页前调用一次）
     *
     * @return channel 及有效期
     */
    @Operation(summary = "直连登录-发起会话，返回 channel")
    @GetMapping("/direct-session")
    public Result<DirectLoginSessionVO> createDirectSession(HttpServletRequest request) {
        String sourceIp = IpUtil.getIpAddress(request);
        RedisRateLimiter.tryAcquire(redisTemplate, RedisKeyUtil.directSessionIpRate(sourceIp),
                DIRECT_SESSION_IP_LIMIT, DIRECT_SESSION_RATE_WINDOW_SECONDS, ResultCode.EXCESSIVE_IP_ACCESS_TIMES);
        RedisRateLimiter.tryAcquire(redisTemplate, RedisKeyUtil.directSessionFingerprintRate(resolveClientFingerprint(request)),
                DIRECT_SESSION_FINGERPRINT_LIMIT, DIRECT_SESSION_RATE_WINDOW_SECONDS, ResultCode.EXCESSIVE_IP_ACCESS_TIMES);
        return Result.success(directLoginService.createDirectSession(sourceIp));
    }

    /**
     * 凭一次性 ticket 兑换用户信息并发自家会话（登录/注册共用）
     *
     * @param body 请求体，含 ticket 字段
     * @return 本地会话（含 token、uid）
     */
    @Operation(summary = "直连登录-凭 ticket 兑换用户信息并发本地会话")
    @PostMapping("/complete-login")
    public Result<LoginSessionVO> completeLogin(@RequestBody Map<String, String> body) {
        return Result.success(directLoginService.completeLogin(body.get("ticket")));
    }

    private String resolveClientFingerprint(HttpServletRequest request) {
        String deviceId = request.getHeader("X-Device-Id");
        String source = deviceId == null || deviceId.isBlank() ? request.getHeader("User-Agent") : deviceId;
        if (source == null || source.isBlank()) {
            source = "unknown";
        }
        return Integer.toUnsignedString(source.hashCode(), 16);
    }
}
