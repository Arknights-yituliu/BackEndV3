package com.lhs.controller;

import com.lhs.common.context.UserContext;
import com.lhs.common.util.Result;
import com.lhs.common.util.SignUtil;
import com.lhs.entity.dto.user.UcTokenVO;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.service.user.UcTokenMigrateService;
import com.lhs.service.user.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 一图流用户系统控制器
 * <p>
 * 用户中心迁移后：注册/登录/找回/改密/邮箱绑定等接口已下线，
 * 登录统一走直连登录（见 /direct-session 与 /complete-login）。
 * <p>
 * UC 令牌相关的两个接口（兑换 / 刷新）只负责把 UC access_token 交给前端，
 * 它们不参与 BackEndV3 自身的鉴权——鉴权始终依据本地 loginToken
 */
@RestController
@Tag(name = "一图流用户系统")
public class UserController {

    /** 响应携带令牌，禁止中间代理与浏览器缓存 */
    private static final String CACHE_CONTROL_NO_STORE = "no-store";

    private final UserSessionService userSessionService;

    private final UcTokenMigrateService ucTokenMigrateService;

    public UserController(UserSessionService userSessionService, UcTokenMigrateService ucTokenMigrateService) {
        this.userSessionService = userSessionService;
        this.ucTokenMigrateService = ucTokenMigrateService;
    }

    @Operation(summary = "根据token检查用户登录状态吗，返回用户信息")
    @GetMapping("/user/info")
    public Result<UserInfoVO> getUserInfo(@RequestParam String token) {
        UserInfoVO response = userSessionService.getUserInfoVOByToken(token);
        return Result.success(response);
    }

    @Operation(summary = "用户登出，使当前登录token失效")
    @PostMapping("/auth/user/logout")
    public Result<Object> logout(HttpServletRequest httpServletRequest) {
        userSessionService.logout(httpServletRequest);
        return Result.success();
    }

    @Operation(summary = "修改当前登录用户的昵称")
    @PostMapping("/auth/user/nickname")
    public Result<Object> updateNickname(HttpServletRequest httpServletRequest,
            @RequestBody Map<String, String> body) {
        userSessionService.updateNickname(httpServletRequest, body.get("nickname"));
        return Result.success();
    }

    @Operation(summary = "修改当前登录用户的头像")
    @PostMapping("/auth/user/avatar")
    public Result<Object> updateAvatar(HttpServletRequest httpServletRequest,
            @RequestBody Map<String, String> body) {
        userSessionService.updateAvatar(httpServletRequest, body.get("avatar"));
        return Result.success();
    }

    /**
     * 兑换 UC 令牌（前端主动触发）
     * <p>
     * 前端在应用启动 / 进入业务页时发现本地无 UC_ACCESS_TOKEN 才调用本接口；
     * 请求携带自签 token（拦截器已校验并写入上下文），
     * 内部按 uid 查兑换缓存，未命中才签名调用 UC 兑换。兑换失败不影响业务请求
     *
     * @param httpServletResponse HTTP 响应（写入禁止缓存头）
     * @return UC 令牌（已剥离 refresh_token，不含用户资料）
     */
    @Operation(summary = "兑换 UC 令牌（前端启动时本地无 UC_ACCESS_TOKEN 才调用）")
    @PostMapping("/auth/uc-token/issue")
    public Result<UcTokenVO> issueUcToken(HttpServletResponse httpServletResponse) {
        // 响应携带令牌：确保中间代理与浏览器不缓存
        httpServletResponse.setHeader("Cache-Control", CACHE_CONTROL_NO_STORE);
        // legacy_token_hash 仅作 UC 侧审计，不参与鉴权与幂等判断
        String legacyTokenHash = SignUtil.sha256(UserContext.getToken());
        UcTokenVO token = ucTokenMigrateService.issueByUid(UserContext.getUid(), legacyTokenHash);
        return Result.success(toClientToken(token));
    }

    /**
     * 刷新 UC access_token
     * <p>
     * 前端调 UC 接口遇 401 时带自签 token 调用：按 uid 取服务端代持的 refresh_token
     * 向 UC 换取新 access_token。刷新失败（如 90009）时前端删除本地 UC_ACCESS_TOKEN，
     * 下次启动重新触发兑换自愈，用户不会掉登录
     *
     * @param httpServletResponse HTTP 响应（写入禁止缓存头）
     * @return 新的 UC 令牌（已剥离 refresh_token）
     */
    @Operation(summary = "刷新 UC access_token（前端调 UC 接口遇 401 时带自签 token 调用）")
    @PostMapping("/auth/token/refresh")
    public Result<UcTokenVO> refreshUcToken(HttpServletResponse httpServletResponse) {
        // 响应携带令牌：确保中间代理与浏览器不缓存
        httpServletResponse.setHeader("Cache-Control", CACHE_CONTROL_NO_STORE);
        UcTokenVO token = ucTokenMigrateService.refreshByUid(UserContext.getUid());
        return Result.success(toClientToken(token));
    }

    /**
     * 转换为下发前端的响应对象
     * <p>
     * 只保留 access_token / token_type / expires_in / scope：refresh_token 与 uid
     * 只留在服务端，不下发浏览器，也不回带用户资料
     *
     * @param token UC 令牌对
     * @return 可下发前端的令牌对象
     */
    private static UcTokenVO toClientToken(UcTokenVO token) {
        token.setRefreshToken(null);
        token.setUid(null);
        return token;
    }

}
