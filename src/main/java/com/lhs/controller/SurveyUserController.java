package com.lhs.controller;

import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.IpUtil;
import com.lhs.common.entity.Result;
import com.lhs.entity.dto.survey.EmailDto;
import com.lhs.entity.dto.survey.UpdateUserDataDto;
import com.lhs.entity.dto.survey.UserDataDto;
import com.lhs.entity.dto.survey.SklandDto;
import com.lhs.service.survey.SurveyUserService;

import com.lhs.entity.vo.survey.UserDataResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@Tag(name ="一图流用户系统")
@RequestMapping(value = "/survey")
@CrossOrigin()
public class SurveyUserController {

    private final SurveyUserService surveyUserService;

    public SurveyUserController(SurveyUserService surveyUserService) {
        this.surveyUserService = surveyUserService;
    }

    @Operation(summary ="调查用户注册V2")
    @PostMapping("/register/v2")
    public Result<UserDataResponse> registerV2(HttpServletRequest httpServletRequest, @RequestBody UserDataDto userDataDto) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        return  Result.success(surveyUserService.registerV2(ipAddress, userDataDto));
    }

    @Operation(summary ="调查用户登录")
    @PostMapping("/login/v2")
    public Result<UserDataResponse> loginV2(HttpServletRequest httpServletRequest,@RequestBody UserDataDto userDataDto) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        UserDataResponse response = surveyUserService.loginV2(ipAddress, userDataDto);
        return Result.success(response);
    }

    @Operation(summary ="发送注册邮件验证码")
    @PostMapping("/user/emailCode")
    public Result<Object> sendEmailCodeForRegister(@RequestBody EmailDto emailDto) {
        String mailUsage = emailDto.getMailUsage();
        if("register".equals(mailUsage)){
            surveyUserService.sendEmailForRegister(emailDto);
        }

        if("login".equals(mailUsage)){
            surveyUserService.sendEmailForLogin(emailDto);
        }

        if("changeEmail".equals(mailUsage)){
            surveyUserService.sendEmailForChangeEmail(emailDto);
        }

        return Result.success();
    }


    @Operation(summary ="更新用户信息")
    @PostMapping("/user/update")
    public Result<UserDataResponse> updateEmail(@RequestBody UpdateUserDataDto updateUserDataDto) {
        String property = updateUserDataDto.getProperty();
        UserDataResponse response = null;
        if("email".equals(property)){
            response =  surveyUserService.updateOrBindEmail(updateUserDataDto);
        }

        if("passWord".equals(property)){
            response =  surveyUserService.updatePassWord(updateUserDataDto);
        }

        if("userName".equals(property)){
            response = surveyUserService.updateUserName(updateUserDataDto);
        }

        return Result.success(response);
    }

    @Operation(summary ="通过森空岛CRED直接登录")
    @PostMapping("/user/login/cred")
    public Result<UserDataResponse> loginByCRED(HttpServletRequest httpServletRequest, @RequestBody SklandDto sklandDto) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        return surveyUserService.loginByCRED(ipAddress, sklandDto);
    }

    @Operation(summary ="身份验证")
    @PostMapping("/user/authentication")
    public Result<UserDataResponse> authentication(HttpServletRequest httpServletRequest,@RequestBody SklandDto sklandDto){
        return   Result.success(surveyUserService.authentication(sklandDto));
    }

    @Operation(summary ="找回账号")
    @PostMapping("/user/retrieval")
    public Result<UserDataResponse> retrievalAccount(@RequestBody Map<String,Object> map) {
        String cred = String.valueOf(map.get("cred"));
        return surveyUserService.retrievalAccountByCRED(cred);
    }

//    @Operation(summary ="调查用户注册")
//    @PostMapping("/register")
//    public Result<UserDataResponse> register(HttpServletRequest httpServletRequest, @RequestBody  RegisterOrLoginDto registerOrLoginDto) {
//        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
//        UserDataResponse response = surveyUserService.register(ipAddress, registerOrLoginDto);
//        return Result.success(response);
//    }
//
//    @Operation(summary ="调查用户登录")
//    @PostMapping("/login")
//    public Result<UserDataResponse> login(HttpServletRequest httpServletRequest,@RequestBody RegisterOrLoginDto registerOrLoginDto) {
//        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
//        UserDataResponse response = surveyUserService.login(ipAddress, registerOrLoginDto);
//        return Result.success(response);
//    }


}
