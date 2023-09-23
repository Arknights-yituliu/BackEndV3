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

    @ApiOperation("调查用户注册V2")
    @PostMapping("/register/v2")
    public Result<UserDataResponse> registerV2(HttpServletRequest httpServletRequest, @RequestBody SurveyRequestVo surveyRequestVo) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        return  Result.success(surveyUserService.registerV2(ipAddress, surveyRequestVo));
    }

    @ApiOperation("调查用户登录")
    @PostMapping("/login/v2")
    public Result<UserDataResponse> loginV2(HttpServletRequest httpServletRequest,@RequestBody SurveyRequestVo surveyRequestVo) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        UserDataResponse response = surveyUserService.loginV2(ipAddress, surveyRequestVo);
        return Result.success(response);
    }

    @ApiOperation("发送邮件验证码")
    @PostMapping("/user/emailCode")
    public Result<Object> sendEmailCodeBYRegister(@RequestParam String type,@RequestBody SurveyRequestVo surveyRequestVo) {
        System.out.println(type);
        surveyUserService.sendEmailCode(type,surveyRequestVo);

        return Result.success();
    }

    @ApiOperation("更新用户信息")
    @PostMapping("/user/update")
    public Result<UserDataResponse> updateEmail(@RequestParam String  property,@RequestBody SurveyRequestVo surveyRequestVo) {
        UserDataResponse response = null;
        if("email".equals(property)){
            response =  surveyUserService.updateOrBindEmail(surveyRequestVo);
        }

        if("passWord".equals(property)){
            response =  surveyUserService.updatePassWord(surveyRequestVo);
        }
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
        return   Result.success(surveyUserService.authentication(surveyRequestVo));
    }

    @ApiOperation("找回账号")
    @PostMapping("/user/retrieval")
    public Result<UserDataResponse> retrievalAccount(@RequestBody Map<String,Object> map) {
        String cred = String.valueOf(map.get("cred"));
        return surveyUserService.retrievalAccountByCRED(cred);
    }

    @ApiOperation("调查用户注册")
    @PostMapping("/register")
    public Result<UserDataResponse> register(HttpServletRequest httpServletRequest, @RequestBody SurveyRequestVo surveyRequestVo) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        UserDataResponse response = surveyUserService.register(ipAddress, surveyRequestVo);
        return Result.success(response);
    }

    @ApiOperation("调查用户登录")
    @PostMapping("/login")
    public Result<UserDataResponse> login(HttpServletRequest httpServletRequest,@RequestBody SurveyRequestVo surveyRequestVo) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        UserDataResponse response = surveyUserService.login(ipAddress, surveyRequestVo);
        return Result.success(response);
    }
}
