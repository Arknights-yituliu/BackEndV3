package com.lhs.controller;

import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.EmailRequestDTO;
import com.lhs.entity.dto.survey.UpdateUserDataDTO;
import com.lhs.entity.dto.survey.LoginDataDTO;
import com.lhs.entity.dto.survey.SklandDTO;
import com.lhs.service.survey.SurveyUserService;

import com.lhs.entity.vo.survey.UserInfoVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@Tag(name ="一图流用户系统")
@RequestMapping(value = "/survey")

public class SurveyUserController {

    private final SurveyUserService surveyUserService;

    public SurveyUserController(SurveyUserService surveyUserService) {
        this.surveyUserService = surveyUserService;
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

    @Operation(summary ="获取用户信息")
    @GetMapping("/user/info")
    public Result<UserInfoVO> getUserInfo(HttpServletRequest httpServletRequest, @RequestParam String token) {
        UserInfoVO response = surveyUserService.getUserInfo(token);
        return Result.success(response);
    }

    @Operation(summary ="发送注册邮件验证码")
    @PostMapping("/user/emailCode")
    public Result<Object> sendEmailCodeForRegister(@RequestBody EmailRequestDTO emailRequestDto) {
         surveyUserService.sendEmail(emailRequestDto);
        return Result.success();
    }


    @Operation(summary ="更新用户信息")
    @PostMapping("/user/update")
    public Result<UserInfoVO> updateEmail(@RequestBody UpdateUserDataDTO updateUserDataDto) {

        UserInfoVO userInfoVO = surveyUserService.updateUserData(updateUserDataDto);
        return Result.success(userInfoVO);
    }

    @Operation(summary ="通过森空岛CRED直接登录")
    @PostMapping("/user/login/cred")
    public Result<UserInfoVO> loginByCRED(HttpServletRequest httpServletRequest, @RequestBody SklandDTO sklandDto) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);  //加密
        return surveyUserService.loginByCRED(ipAddress, sklandDto);
    }


    @Operation(summary ="找回账号")
    @PostMapping("/user/retrieval")
    public Result<UserInfoVO> retrievalAccount(@RequestBody Map<String,Object> map) {
        String cred = String.valueOf(map.get("cred"));
        return surveyUserService.retrievalAccountByCRED(cred);
    }



}
