package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.EmailRequestDTO;
import com.lhs.entity.dto.survey.LoginDataDTO;
import com.lhs.entity.dto.survey.UpdateUserDataDTO;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.service.survey.SurveyUserService;
import com.lhs.service.survey.SurveyUserV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@Tag(name ="一图流用户系统")
@RequestMapping(value = "/user")
public class UserController {


    private final SurveyUserV2Service surveyUserV2Service;

    public UserController(SurveyUserV2Service surveyUserV2Service) {
        this.surveyUserV2Service = surveyUserV2Service;
    }


    @Operation(summary ="调查站用户注册")
    @PostMapping("/register/v3")
    public Result<HashMap<String, Object>> registerV3(HttpServletRequest httpServletRequest, @RequestBody LoginDataDTO loginDataDTO){
        HashMap<String, Object> response = surveyUserV2Service.registerV3(httpServletRequest,loginDataDTO);
        return Result.success(response);
    }

    @Operation(summary ="调查站用户登录")
    @PostMapping("/login/v3")
    public Result<HashMap<String, Object>> loginV3(HttpServletRequest httpServletRequest,@RequestBody LoginDataDTO loginDataDTO){
        HashMap<String, Object> response = surveyUserV2Service.loginV3(httpServletRequest,loginDataDTO);
        return Result.success(response);
    }

    @Operation(summary ="根据token检查用户登录状态吗，返回用户信息")
    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo(@RequestParam String token) {

        UserInfoVO response = surveyUserV2Service.getUserInfo(token);
        return Result.success(response);
    }

    @Operation(summary ="发送注册邮件验证码")
    @PostMapping("/verificationCode")
    public Result<Object> sendVerificationCode(@RequestBody EmailRequestDTO emailRequestDto) {
        surveyUserV2Service.sendVerificationCode(emailRequestDto);
        return Result.success();
    }

    @Operation(summary ="更新用户信息")
    @PostMapping("/update/v2")
    public Result<UserInfoVO> updateUserInfo(@RequestBody UpdateUserDataDTO updateUserDataDto) {
        UserInfoVO userInfoVO = surveyUserV2Service.updateUserData(updateUserDataDto);
        return Result.success(userInfoVO);
    }


    @Operation(summary ="找回账号")
    @PostMapping("/retrieve")
    public Result<String> retrieveAccount(@RequestBody LoginDataDTO loginDataDTO) {
        surveyUserV2Service.retrieveAccount(loginDataDTO);
        return Result.success("请在10分钟内修改您的密码，10分钟后失效");
    }



}
