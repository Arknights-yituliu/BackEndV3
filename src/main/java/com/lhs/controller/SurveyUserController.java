package com.lhs.controller;

import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.EmailRequestDTO;
import com.lhs.entity.dto.survey.UpdateUserDataDTO;
import com.lhs.entity.dto.survey.LoginDataDTO;
import com.lhs.service.survey.SurveyUserService;

import com.lhs.entity.vo.survey.UserInfoVO;

import com.lhs.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;

@RestController
@Tag(name ="一图流用户系统")
@RequestMapping(value = "/survey")

public class SurveyUserController {

    private final SurveyUserService surveyUserService;

    private final UserService userService;

    public SurveyUserController(SurveyUserService surveyUserService, UserService userService) {
        this.surveyUserService = surveyUserService;
        this.userService = userService;
    }

    @Operation(summary ="调查站用户注册")
    @PostMapping("/register/v3")
    public Result<HashMap<String, Object>> registerV3(HttpServletRequest httpServletRequest,@RequestBody LoginDataDTO loginDataDTO){
        HashMap<String, Object> response = userService.registerV3(httpServletRequest,loginDataDTO);
        return Result.success(response);
    }

    @Operation(summary ="调查站用户登录")
    @PostMapping("/login/v3")
    public Result<HashMap<String, Object>> loginV3(HttpServletRequest httpServletRequest,@RequestBody LoginDataDTO loginDataDTO){
        HashMap<String, Object> response = userService.loginV3(httpServletRequest,loginDataDTO);
        return Result.success(response);
    }

    @Operation(summary ="根据token检查用户登录状态吗，返回用户信息")
    @GetMapping("/user/info")
    public Result<UserInfoVO> getUserInfo(@RequestParam String token) {

        UserInfoVO response = userService.getUserInfoByToken(token);
        return Result.success(response);
    }

    @Operation(summary ="发送注册邮件验证码")
    @PostMapping("/verificationCode")
    public Result<Object> sendVerificationCode(@RequestBody EmailRequestDTO emailRequestDto) {
        userService.sendVerificationCode(emailRequestDto);
        return Result.success();
    }

    @Operation(summary ="更新用户信息")
    @PostMapping("/user/update/v2")
    public Result<UserInfoVO> updateUserInfo(@RequestBody UpdateUserDataDTO updateUserDataDto) {
        UserInfoVO userInfoVO = userService.updateUserData(updateUserDataDto);
        return Result.success(userInfoVO);
    }


    @Operation(summary ="找回账号")
    @PostMapping("/user/retrieve")
    public Result<String> retrieveAccount(@RequestBody LoginDataDTO loginDataDTO) {
        userService.retrieveAccount(loginDataDTO);
        return Result.success("请在10分钟内修改您的密码，10分钟后失效");
    }
















    @Operation(summary ="发送注册邮件验证码")
    @PostMapping("/user/emailCode")
    public Result<Object> sendEmailCodeForRegister(@RequestBody EmailRequestDTO emailRequestDto) {
         surveyUserService.sendVerificationCode(emailRequestDto);
        return Result.success();
    }


    @Operation(summary ="更新用户信息")
    @PostMapping("/user/update")
    public Result<UserInfoVO> updateEmail(@RequestBody UpdateUserDataDTO updateUserDataDto) {

        UserInfoVO userInfoVO = surveyUserService.updateUserData(updateUserDataDto);
        return Result.success(userInfoVO);
    }






    @Operation(summary ="调查站用户注册")
    @PostMapping("/register/v2")
    public Result<UserInfoVO> registerV2(HttpServletRequest httpServletRequest, @RequestBody LoginDataDTO loginDataDto) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);  //加密
        return  Result.success(surveyUserService.registerV2(ipAddress, loginDataDto));
    }

    @Operation(summary ="调查站用户登录")
    @PostMapping("/login/v2")
    public Result<UserInfoVO> loginV2(HttpServletRequest httpServletRequest, @RequestBody LoginDataDTO loginDataDto) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);  //加密
        UserInfoVO response = surveyUserService.loginV2(ipAddress, loginDataDto);
        return Result.success(response);
    }

}
