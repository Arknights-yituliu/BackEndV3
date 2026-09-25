package com.lhs.entity.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 直连登录-兑换用户信息响应 VO
 * <p>
 * 对应 UC /oauth2/direct-user 接口 data 部分：uid、昵称、头像、脱敏邮箱、状态，
 * 以及 UC 一并签发的 OAuth 令牌（access_token + refresh_token）。
 * 令牌为新增能力：refresh_token 只留在服务端，access_token 随登录响应回带前端调 UC 接口
 */
@Data
public class DirectLoginUserVO {

    /** 用户 uid（本地资料缓存表主键/账号打通 key） */
    private Long uid;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 邮箱（脱敏，仅供展示） */
    private String email;

    /** 状态：1=正常；-1=封禁 */
    private Integer status;

    /** UC access_token，前端调 UC 接口时携带 */
    @JsonProperty("access_token")
    private String accessToken;

    /** 令牌类型，固定 Bearer */
    @JsonProperty("token_type")
    private String tokenType;

    /** access_token 有效期（秒） */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /** UC refresh_token：仅服务端落库用于刷新，绝不下发浏览器 */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /** 授权范围（逗号分隔） */
    private String scope;
}
