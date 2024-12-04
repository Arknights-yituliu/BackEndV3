package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.user.EmailRequestDTO;
import com.lhs.entity.dto.user.LoginDataDTO;
import com.lhs.entity.dto.user.UpdateUserDataDTO;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name ="一图流用户系统")
@RequestMapping(value = "/user")
public class UserController {


    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @Operation(summary ="调查站用户注册")
    @PostMapping("/register/v3")
    public Result<HashMap<String, Object>> registerV3(HttpServletRequest httpServletRequest, @RequestBody LoginDataDTO loginDataDTO){
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
    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo(@RequestParam String token) {
        UserInfoVO response = userService.getUserInfoVOByToken(token);
        return Result.success(response);
    }

    @Operation(summary ="发送注册邮件验证码")
    @PostMapping("/verificationCode")
    public Result<Object> sendVerificationCode(@RequestBody EmailRequestDTO emailRequestDto) {
        userService.sendVerificationCode(emailRequestDto);
        return Result.success();
    }

    @Operation(summary ="更新用户信息")
    @PostMapping("/update/v2")
    public Result<UserInfoVO> updateUserInfo(@RequestBody UpdateUserDataDTO updateUserDataDto) {
        UserInfoVO userInfoVO = userService.updateUserData(updateUserDataDto);
        return Result.success(userInfoVO);
    }

    @Operation(summary ="更新用户配置")
    @GetMapping("/update/config")
    public Result<String> updateUserConfig(HttpServletRequest httpServletRequest,@RequestBody Map<String,Object> config) {
         userService.updateUserConfig(config,httpServletRequest);
        return Result.success();
    }


    @Operation(summary ="通过验证找回账号")
    @PostMapping("/retrieve/auth")
    public Result<HashMap<String,String>> retrieveAccount(@RequestBody LoginDataDTO loginDataDTO) {
        return Result.success(userService.retrieveAccount(loginDataDTO));
    }

    @Operation(summary ="重设密码")
    @PostMapping("/reset/password")
    public Result<HashMap<String,String>> resetPassword(@RequestBody LoginDataDTO loginDataDTO) {
        return Result.success(userService.resetAccount(loginDataDTO));
    }




}
