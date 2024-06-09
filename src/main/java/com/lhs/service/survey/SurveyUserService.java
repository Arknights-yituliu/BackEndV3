package com.lhs.service.survey;


import com.lhs.entity.dto.survey.EmailRequestDTO;
import com.lhs.entity.dto.survey.LoginDataDTO;
import com.lhs.entity.dto.survey.UpdateUserDataDTO;
import com.lhs.entity.po.survey.UserInfo;
import com.lhs.entity.vo.survey.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;


public interface SurveyUserService {

    /**
     * 调查站用户注册
     * @param ipAddress 用户ip
     * @param loginDataDTO 注册信息
     * @return
     */
    UserInfoVO registerV2(String ipAddress, LoginDataDTO loginDataDTO);

    /**
     * 调查站登录
     * @param ipAddress       ip
     * @param loginDataDto 用户修改的信息
     * @return 用户状态信息
     */
    UserInfoVO loginV2(String ipAddress, LoginDataDTO loginDataDto);

    /**
     * 通过token获取用户信息
     * @param token 用户登录后获得的凭证
     * @return 用户信息
     */
    UserInfoVO getUserInfo(String token);

    /**
     * 发送邮件验证码
     * @param emailRequestDto 邮件请求数据
     */
    void sendVerificationCode(EmailRequestDTO emailRequestDto);


    UserInfoVO updateUserData(UpdateUserDataDTO updateUserDataDto);

    /**
     * 通过用户凭证查找用户信息
     * @param token 用户凭证
     * @return 用户信息
     */
    UserInfo getSurveyUserByToken(String token);



    /**
     * 备份更新用户信息
     * @param userInfo 用户信息
     */
    void backupSurveyUser(UserInfo userInfo);

    /**
     * 拿到表名序号
     * @param id  一图流id
     * @return 表名序号
     */
    Integer getTableIndex(Long id) ;

    HashMap<String, Object> loginV3(HttpServletRequest httpServletRequest, LoginDataDTO loginDataDTO);

    HashMap<String,Object> registerV3(HttpServletRequest httpServletRequest,LoginDataDTO loginDataDTO);


}
