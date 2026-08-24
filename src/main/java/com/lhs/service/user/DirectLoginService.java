package com.lhs.service.user;

import com.lhs.entity.dto.user.DirectLoginSessionVO;
import com.lhs.entity.vo.user.LoginSessionVO;

/**
 * UC 直连登录接入服务（方案 B：保持旧系统登录页）
 * <p>
 * 后端用 client_secret 调 UC 换 channel 下发前端，前端将登录/注册凭证直接提交 UC
 * 换取一次性 ticket，后端凭 ticket 兑换用户信息并发自家会话
 */
public interface DirectLoginService {

    /**
     * 发起直连登录会话：调用 UC /oauth2/direct-session 换取 channel
     *
     * @return 发起会话凭证及有效期
     */
    DirectLoginSessionVO createDirectSession();

    /**
     * 凭一次性 ticket 兑换用户信息并建立本地会话
     *
     * @param ticket 前端直连登录/注册后返回的一次性票据
     * @return 本地会话（含 token、uid）
     */
    LoginSessionVO completeLogin(String ticket);
}
