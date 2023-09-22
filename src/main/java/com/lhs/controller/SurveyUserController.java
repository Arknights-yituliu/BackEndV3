package com.lhs.controller;

import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.Result;
import com.lhs.service.survey.SurveyUserService;
import com.lhs.vo.survey.SurveyRequestVo;
import com.lhs.vo.survey.UserDataResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@Api(tags = "一图流用户系统")
@RequestMapping(value = "/survey")
@CrossOrigin()
public class SurveyUserController {

    private final SurveyUserService surveyUserService;

    public SurveyUserController(SurveyUserService surveyUserService) {
        this.surveyUserService = surveyUserService;
    }

    @ApiOperation("调查用户注册")
    @PostMapping("/register")
    public Result<UserDataResponse> register(HttpServletRequest httpServletRequest, @RequestBody SurveyRequestVo surveyRequestVo) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        UserDataResponse response = surveyUserService.register(ipAddress, surveyRequestVo.getUserName());
        return Result.success(response);
    }

    @ApiOperation("通过森空岛CRED直接登录")
    @PostMapping("/user/login/cred")
    public Result<UserDataResponse> loginByCRED(HttpServletRequest httpServletRequest, @RequestBody SurveyRequestVo surveyRequestVo) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        return surveyUserService.loginByCRED(ipAddress, surveyRequestVo);
    }

    @ApiOperation("身份验证")
    @PostMapping("/user/authentication")
    public Result<UserDataResponse> authentication(HttpServletRequest httpServletRequest,@RequestBody SurveyRequestVo surveyRequestVo){
        return   surveyUserService.authentication(surveyRequestVo);
    }

    @ApiOperation("调查用户登录")
    @PostMapping("/login")
    public Result<UserDataResponse> login(HttpServletRequest httpServletRequest,@RequestBody SurveyRequestVo surveyRequestVo) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        UserDataResponse response = surveyUserService.login(ipAddress, surveyRequestVo);
        return Result.success(response);
    }

    @ApiOperation("更新密码")
    @PostMapping("/user/sendEmailCode")
    public Result<Object> sendEmailCode(@RequestBody SurveyRequestVo surveyRequestVo) {
        return surveyUserService.sendEmailCode(surveyRequestVo);
    }

    @ApiOperation("更新密码")
    @PostMapping("/user/updateEmail")
    public Result<Object> updateEmail(@RequestBody SurveyRequestVo surveyRequestVo) {
        return surveyUserService.updateOrBindEmail(surveyRequestVo);
    }

    @ApiOperation("更新密码")
    @PostMapping("/user/updatePassWord")
    public Result<Object> updatePassWord(@RequestBody SurveyRequestVo surveyRequestVo) {
        return surveyUserService.updatePassWord(surveyRequestVo);
    }

    @ApiOperation("找回账号")
    @PostMapping("/user/retrieval")
    public Result<UserDataResponse> retrievalAccount(@RequestBody Map<String,Object> map) {
        String cred = String.valueOf(map.get("cred"));
        return surveyUserService.retrievalAccountByCRED(cred);
    }
}
