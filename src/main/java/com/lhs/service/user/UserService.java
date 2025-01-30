package com.lhs.service.user;

import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.dto.user.*;
import com.lhs.entity.po.user.UserInfo;
import com.lhs.entity.vo.survey.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;

public interface UserService {


    /**
     * 用户注册
     *
     * @param httpServletRequest HTTP请求对象
     * @param loginDataDTO       用户修改的信息
     * @return 用户状态信息
     */
    HashMap<String, Object> registerV3(HttpServletRequest httpServletRequest, LoginDataDTO loginDataDTO);

    /**
     * 用户登录
     *
     * @param httpServletRequest HTTP请求对象
     * @param loginDataDTO       用户修改的信息
     * @return 用户状态信息
     */
    HashMap<String, Object> loginV3(HttpServletRequest httpServletRequest, LoginDataDTO loginDataDTO);



    String extractToken(HttpServletRequest request);

    /**
     * 检查用户登录状态
     * @param httpServletRequest HTTP请求对象
     * @return 是否登录
     */
    Boolean checkUserLoginStatus(HttpServletRequest httpServletRequest);

    /**
     * 发送邮件验证码
     *
     * @param emailRequestDto 邮件请求数据
     */
    void sendVerificationCode(EmailRequestDTO emailRequestDto);
    void sendUpdateEmailVerificationCode(HttpServletRequest httpServletRequest, EmailRequestDTO emailRequestDto);
    String checkVerificationCode(HttpServletRequest httpServletRequest,String verificationCode);
    void bindEmail(HttpServletRequest httpServletRequest, UpdateUserDataDTO updateUserDataDto);
    /**
     * 通过token获取用户信息
     *
     * @param token 用户登录后获得的凭证
     * @return 用户信息
     */
    UserInfoVO getUserInfoVOByToken(String token);

    /**
     * 通过token获取用户数据内的信息
     *
     * @param token 用户登录后获得的凭证
     * @return 用户信息
     */
    UserInfo getUserInfoPOByToken(String token);

    /**
     * 通过HttpServletRequest获取token，根据token拿到用户信息
     *
     * @param httpServletRequest HTTP请求对象
     * @return 用户信息
     */
    UserInfoVO getUserInfoVOByHttpServletRequest(HttpServletRequest httpServletRequest);

    /**
     * 通过HttpServletRequest获取token，根据token拿到用户id，如果没有token则查看请求头是否含有前端传来的临时uid，如果有则返回，没有则根据ip生成一个临时uid
     *
     * @param httpServletRequest HTTP请求对象
     * @return 用户信息
     */
    Long getUidByHttpServletRequest(HttpServletRequest httpServletRequest);

    /**
     * 通过HttpServletRequest获取token，根据token拿到用户信息
     *
     * @param httpServletRequest HTTP请求对象
     * @return 用户信息
     */
    UserInfo getUserInfoPOByHttpServletRequest(HttpServletRequest httpServletRequest);

    /**
     * 获取用户的各种自定义配置
     *
     * @param request 请求体
     * @return 用户配置
     */
    StageConfigDTO getUserStageConfig(HttpServletRequest request);

    /**
     * 更新用户的各种自定义配置
     * @param httpServletRequest HTTP请求对象
     * @param userConfigDTO 请求体
     */
    void updateUserConfig(HttpServletRequest httpServletRequest,UserConfigDTO userConfigDTO);

    /**
     * 更新用户信息
     * @param httpServletRequest HTTP请求对象
     * @param updateUserDataDto 要更新的内容
     * @return 用户信息
     */
    UserInfoVO updateUserData(HttpServletRequest httpServletRequest,UpdateUserDataDTO updateUserDataDto);

    void backupSurveyUser(UserInfo userInfo);

    /**
     * 找回账号
     *
     * @param loginDataDTO 找回所需的内容
     * @return 临时凭证
     */
    HashMap<String, String> retrieveAccount(LoginDataDTO loginDataDTO);

    /**
     * 重设密码
     * @param httpServletRequest HTTP请求对象
     * @param loginDataDTO 找回所需的内容
     * @return 用户凭证
     */
    HashMap<String, String> resetPassword(HttpServletRequest httpServletRequest,LoginDataDTO loginDataDTO);

    /**
     * 这个方法将会保存两个信息：
     * 1.一图流用户的唯一标识与第三方账号的唯一标识的对应关系
     * 2.第三方账号的信息
     * @param userInfoVO 一图流用户信息
     * @param akPlayerBindInfoDTO 第三方账号的信息
     */
    void saveExternalAccountBindingInfoAndAKPlayerBindInfo(UserInfoVO userInfoVO, AkPlayerBindInfoDTO akPlayerBindInfoDTO);



}
