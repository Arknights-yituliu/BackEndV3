package com.lhs.service.user;

import com.lhs.entity.dto.survey.EmailRequestDTO;
import com.lhs.entity.dto.survey.LoginDataDTO;
import com.lhs.entity.dto.survey.UpdateUserDataDTO;
import com.lhs.entity.po.survey.UserInfo;
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
    UserInfoVO getUserInfo(String token);

    UserInfo getSurveyUserByToken(String token);


    UserInfoVO updateUserData(UpdateUserDataDTO updateUserDataDto);

    void backupSurveyUser(UserInfo userInfo);

    HashMap<String, String> retrieveAccount(LoginDataDTO loginDataDTO);

    HashMap<String, String> resetAccount(LoginDataDTO loginDataDTO);
}
