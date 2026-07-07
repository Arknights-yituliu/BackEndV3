package com.lhs.service.user;

import com.lhs.entity.dto.user.AkPlayerBindInfoDTO;
import com.lhs.entity.dto.user.EmailRequestDTO;
import com.lhs.entity.dto.user.UpdateUserDataDTO;
import com.lhs.entity.vo.survey.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;

public interface BindService {

    /**
     * 发送邮件验证码
     *
     * @param httpServletRequest HTTP请求对象
     * @param emailRequestDto    邮件请求数据
     */
    void sendVerificationCode(HttpServletRequest httpServletRequest, EmailRequestDTO emailRequestDto);

    /**
     * 发送更新邮箱的验证码
     *
     * @param httpServletRequest HTTP请求对象
     * @param emailRequestDto    邮箱请求信息
     */
    void sendUpdateEmailVerificationCode(HttpServletRequest httpServletRequest, EmailRequestDTO emailRequestDto);

    /**
     * 检查验证码
     *
     * @param httpServletRequest HTTP请求对象
     * @param verificationCode   验证码
     * @return 成功消息
     */
    String checkVerificationCode(HttpServletRequest httpServletRequest, String verificationCode);

    /**
     * 绑定邮箱
     *
     * @param httpServletRequest HTTP请求对象
     * @param updateUserDataDto  用户信息
     */
    void bindEmail(HttpServletRequest httpServletRequest, UpdateUserDataDTO updateUserDataDto);

    /**
     * 备份用户外部账号绑定数据到腾讯云COS
     */
    void backupUserExternalAccountBinding();

    /**
     * 保存一图流用户与第三方游戏账号（明日方舟）的绑定关系
     *
     * @param userInfoVO          一图流用户信息
     * @param akPlayerBindInfoDTO 第三方账号的信息
     */
    void saveExternalAccountBindingInfoAndAKPlayerBindInfo(UserInfoVO userInfoVO, AkPlayerBindInfoDTO akPlayerBindInfoDTO);
}
