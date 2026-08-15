package com.lhs.service.user;

import com.lhs.entity.dto.user.LoginDataDTO;
import com.lhs.entity.dto.user.UpdateUserDataDTO;
import com.lhs.entity.vo.survey.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;

/**
 * 遗留用户服务（仅保留下线前的注册/登录/找回/改密等逻辑）
 * <p>
 * 用户中心迁移后，这些接口已下线、不再对外使用。
 * 仍被业务使用的用户逻辑已迁移至 {@link OAuthUserService}
 */
public interface UserService {

    /**
     * 用户注册（已下线，保留历史逻辑）
     *
     * @param httpServletRequest HTTP请求对象
     * @param loginDataDTO       用户修改的信息
     * @return 用户状态信息
     */
    HashMap<String, Object> registerV3(HttpServletRequest httpServletRequest, LoginDataDTO loginDataDTO);

    /**
     * 用户登录（已下线，保留历史逻辑）
     *
     * @param httpServletRequest HTTP请求对象
     * @param loginDataDTO       用户修改的信息
     * @return 用户状态信息
     */
    HashMap<String, Object> loginV3(HttpServletRequest httpServletRequest, LoginDataDTO loginDataDTO);

    /**
     * 更新用户信息（已下线，保留历史逻辑）
     *
     * @param httpServletRequest HTTP请求对象
     * @param updateUserDataDto  要更新的内容
     * @return 用户信息
     */
    UserInfoVO updateUserData(HttpServletRequest httpServletRequest, UpdateUserDataDTO updateUserDataDto);

    /**
     * 找回账号（已下线，保留历史逻辑）
     *
     * @param loginDataDTO 找回所需的内容
     * @return 临时凭证
     */
    HashMap<String, String> retrieveAccount(LoginDataDTO loginDataDTO);

    /**
     * 重设密码（已下线，保留历史逻辑）
     *
     * @param httpServletRequest HTTP请求对象
     * @param loginDataDTO       找回所需的内容
     * @return 用户凭证
     */
    HashMap<String, String> resetPassword(HttpServletRequest httpServletRequest, LoginDataDTO loginDataDTO);
}
