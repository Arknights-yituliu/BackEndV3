package com.lhs.entity.dto.user;

import lombok.Data;

/**
 * 直连登录-发起会话响应 VO
 * <p>
 * 对应 UC /oauth2/direct-session 接口 data 部分：channel（发起会话凭证）与有效期
 */
@Data
public class DirectLoginSessionVO {

    /** 发起会话凭证（前端提交登录/注册凭证时需携带） */
    private String channel;

    /** 有效期（秒），默认 300 */
    private Long expiresIn;
}
