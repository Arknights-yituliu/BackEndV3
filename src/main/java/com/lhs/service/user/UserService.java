package com.lhs.service.user;

import com.lhs.entity.dto.user.AkPlayerBindInfoDTO;
import com.lhs.entity.dto.user.EmailRequestDTO;
import com.lhs.entity.dto.user.LoginDataDTO;
import com.lhs.entity.dto.user.UpdateUserDataDTO;
import com.lhs.entity.po.user.AkPlayerBindInfo;
import com.lhs.entity.po.user.UserExternalAccountBinding;
import com.lhs.entity.po.user.UserInfo;
import com.lhs.entity.vo.survey.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;

public interface UserService {

    /**
     * 调查站登录
     * @param httpServletRequest  请求体
     * @param loginDataDTO 用户修改的信息
     * @return 用户状态信息
     */
    HashMap<String, Object> loginV3(HttpServletRequest httpServletRequest, LoginDataDTO loginDataDTO);

    String extractToken(HttpServletRequest request);

    /**
     * 调查站注册
     * @param httpServletRequest  请求体
     * @param loginDataDTO 用户修改的信息
     * @return 用户状态信息
     */
    HashMap<String,Object> registerV3(HttpServletRequest httpServletRequest,LoginDataDTO loginDataDTO);

    /**
     * 发送邮件验证码
     * @param emailRequestDto 邮件请求数据
     */
    void sendVerificationCode(EmailRequestDTO emailRequestDto);

    /**
     * 通过token获取用户信息
     * @param token 用户登录后获得的凭证
     * @return 用户信息
     */
    UserInfoVO getUserInfoVOByToken(String token);

    /**
     * 通过token获取用户数据内的信息
     * @param token 用户登录后获得的凭证
     * @return 用户信息
     */
    UserInfo getUserInfoPOByToken(String token);

    /**
     * 更新用户信息
     * @param updateUserDataDto 要更新的内容
     * @return 用户信息
     */
    UserInfoVO updateUserData(UpdateUserDataDTO updateUserDataDto);

    void backupSurveyUser(UserInfo userInfo);

    /**
     * 找回账号
     * @param loginDataDTO  找回所需的内容
     * @return 临时凭证
     */
    HashMap<String, String> retrieveAccount(LoginDataDTO loginDataDTO);

    /**
     * 找回账号
     * @param loginDataDTO  找回所需的内容
     * @return 临时凭证
     */
    HashMap<String, String> resetAccount(LoginDataDTO loginDataDTO);

    Boolean saveUserExternalAccountBinding(UserExternalAccountBinding accountBinding);

    AkPlayerBindInfo getAkPlayerBindInfo(String akUid, Long uid);

    void saveAkPlayerBindInfo(AkPlayerBindInfo akPlayerBindInfo);


    void saveBindInfo(UserInfoVO userInfoVO, AkPlayerBindInfoDTO akPlayerBindInfoDTO);



}
