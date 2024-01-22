package com.lhs.service.survey;


import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.EmailRequestDTO;
import com.lhs.entity.dto.survey.LoginDataDTO;
import com.lhs.entity.dto.survey.SklandDTO;
import com.lhs.entity.dto.survey.UpdateUserDataDTO;
import com.lhs.entity.po.survey.SurveyUser;
import com.lhs.entity.vo.survey.UserDataVO;
import org.springframework.stereotype.Service;

import java.util.*;


public interface SurveyUserService {

    /**
     * 调查站用户注册
     * @param ipAddress 用户ip
     * @param loginDataDTO 注册信息
     * @return
     */
    UserDataVO registerV2(String ipAddress, LoginDataDTO loginDataDTO);

    /**
     * 调查站登录
     * @param ipAddress       ip
     * @param loginDataDto 用户修改的信息
     * @return 用户状态信息
     */
    UserDataVO loginV2(String ipAddress, LoginDataDTO loginDataDto);

    /**
     * 通过密码登录
     * @param loginDataDto 前端传来的用户名密码
     * @return 用户数据
     */
    SurveyUser loginByPassWord(LoginDataDTO loginDataDto);

    /**
     * 发送邮件验证码
     * @param emailRequestDto
     */
    void sendEmail(EmailRequestDTO emailRequestDto);


    UserDataVO updateUserData(UpdateUserDataDTO updateUserDataDto);

    /**
     * 通过用户凭证查找用户信息
     * @param token 用户凭证
     * @return 用户信息
     */
    SurveyUser getSurveyUserByToken(String token);

    /**
     * 通过游戏内的玩家uid查找用户信息
     *
     * @param uid 游戏内的玩家uid
     * @return 用户信息
     */
    SurveyUser getSurveyUserByAkUid(String uid);

    /**
     * 备份更新用户信息
     * @param surveyUser 用户信息
     */
    void backupSurveyUser(SurveyUser surveyUser);

    /**
     * 拿到表名序号
     * @param id  一图流id
     * @return 表名序号
     */
    Integer getTableIndex(Long id) ;

    /**
     * 通过森空岛cred登录
     *
     * @param ipAddress       ip地址
     * @param sklandDto 用户修改的信息
     * @return 用户状态信息
     */
    Result<UserDataVO> loginByCRED(String ipAddress, SklandDTO sklandDto);

    /**
     * 通过森空岛CRED找回账号
     *
     * @param CRED 森空岛CRED
     * @return 用户状态信息
     */
    Result<UserDataVO> retrievalAccountByCRED(String CRED);

    void migrateLog();

    List<SurveyUser> getAllUserData();
}
