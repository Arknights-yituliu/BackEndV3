package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.user.EmailRequestDTO;
import com.lhs.entity.dto.user.LoginDataDTO;
import com.lhs.entity.dto.user.UpdateUserDataDTO;
import com.lhs.entity.dto.user.UserConfigDTO;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@Tag(name ="一图流用户系统")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary ="调查站用户注册")
    @PostMapping("/user/register/v3")
    public Result<HashMap<String, Object>> registerV3(HttpServletRequest httpServletRequest, @RequestBody LoginDataDTO loginDataDTO){
        HashMap<String, Object> response = userService.registerV3(httpServletRequest,loginDataDTO);
        return Result.success(response);
    }

    @Operation(summary ="调查站用户登录")
    @PostMapping("/user/login/v3")
    public Result<HashMap<String, Object>> loginV3(HttpServletRequest httpServletRequest,@RequestBody LoginDataDTO loginDataDTO){
        HashMap<String, Object> response = userService.loginV3(httpServletRequest,loginDataDTO);
        return Result.success(response);
    }

    @Operation(summary ="根据token检查用户登录状态吗，返回用户信息")
    @GetMapping("/user/info")
    public Result<UserInfoVO> getUserInfo(@RequestParam String token) {
        UserInfoVO response = userService.getUserInfoVOByToken(token);
        return Result.success(response);
    }

    @Operation(summary ="发送邮件验证码")
    @PostMapping("/user/verificationCode")
    public Result<Object> sendVerificationCode(@RequestBody EmailRequestDTO emailRequestDto) {
        userService.sendVerificationCode(emailRequestDto);
        return Result.success();
    }

    @Operation(summary ="发送更新邮件验证码")
    @PostMapping("/auth/user/update-email/verificationCode")
    public Result<Object> sendUpdateEmailVerificationCode(HttpServletRequest httpServletRequest,@RequestBody EmailRequestDTO emailRequestDto) {
        userService.sendUpdateEmailVerificationCode(httpServletRequest,emailRequestDto);
        return Result.success();
    }

    @Operation(summary ="检查验证码")
    @GetMapping("/auth/user/check/verificationCode")
    public Result<Object> checkVerificationCode(HttpServletRequest httpServletRequest,@RequestParam("verificationCode") String verificationCode) {
        return Result.success(userService.checkVerificationCode(httpServletRequest,verificationCode));
    }

    @Operation(summary ="绑定邮箱")
    @PostMapping("/auth/user/bind-email")
    public Result<Object> bindEmail(HttpServletRequest httpServletRequest,@RequestBody UpdateUserDataDTO updateUserDataDto) {
        userService.bindEmail(httpServletRequest,updateUserDataDto);
        return Result.success();
    }

    @Operation(summary ="更新用户信息")
    @PostMapping("/auth/user/update/v2")
    public Result<UserInfoVO> updateUserInfo(HttpServletRequest httpServletRequest,@RequestBody UpdateUserDataDTO updateUserDataDto) {
        UserInfoVO userInfoVO = userService.updateUserData(httpServletRequest,updateUserDataDto);
        return Result.success(userInfoVO);
    }



    @Operation(summary ="更新用户配置")
    @GetMapping("/user/update/config")
    public Result<String> updateUserConfig(HttpServletRequest httpServletRequest,@RequestBody UserConfigDTO userConfigDTO) {
         userService.updateUserConfig(httpServletRequest,userConfigDTO);
        return Result.success();
    }


    @Operation(summary ="通过验证找回账号")
    @PostMapping("/user/retrieve/auth")
    public Result<HashMap<String,String>> retrieveAccount(@RequestBody LoginDataDTO loginDataDTO) {
        return Result.success(userService.retrieveAccount(loginDataDTO));
    }

    @Operation(summary ="重设密码")
    @PostMapping("/user/reset/password")
    public Result<HashMap<String,String>> resetPassword(HttpServletRequest httpServletRequest,@RequestBody LoginDataDTO loginDataDTO) {
        return Result.success(userService.resetPassword(httpServletRequest,loginDataDTO));
    }




}
