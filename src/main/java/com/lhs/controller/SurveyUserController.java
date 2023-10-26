package com.lhs.controller;

import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.EmailRequestDTO;
import com.lhs.entity.dto.survey.UpdateUserDataDTO;
import com.lhs.entity.dto.survey.LoginDataDTO;
import com.lhs.entity.dto.survey.SklandDTO;
import com.lhs.service.survey.SurveyUserService;

import com.lhs.entity.vo.survey.UserDataVO;

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
    public Result<UserDataVO> registerV2(HttpServletRequest httpServletRequest, @RequestBody LoginDataDTO loginDataDto) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        return  Result.success(surveyUserService.registerV2(ipAddress, loginDataDto));
    }

    @Operation(summary ="调查用户登录")
    @PostMapping("/login/v2")
    public Result<UserDataVO> loginV2(HttpServletRequest httpServletRequest, @RequestBody LoginDataDTO loginDataDto) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        UserDataVO response = surveyUserService.loginV2(ipAddress, loginDataDto);
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
    public Result<UserDataVO> updateEmail(@RequestBody UpdateUserDataDTO updateUserDataDto) {

        UserDataVO userDataVO = surveyUserService.updateUserData(updateUserDataDto);
        return Result.success(userDataVO);
    }

    @Operation(summary ="通过森空岛CRED直接登录")
    @PostMapping("/user/login/cred")
    public Result<UserDataVO> loginByCRED(HttpServletRequest httpServletRequest, @RequestBody SklandDTO sklandDto) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        return surveyUserService.loginByCRED(ipAddress, sklandDto);
    }


    @Operation(summary ="找回账号")
    @PostMapping("/user/retrieval")
    public Result<UserDataVO> retrievalAccount(@RequestBody Map<String,Object> map) {
        String cred = String.valueOf(map.get("cred"));
        return surveyUserService.retrievalAccountByCRED(cred);
    }



}
