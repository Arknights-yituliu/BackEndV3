package com.lhs.controller;

import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.Result;
import com.lhs.service.survey.SurveyUserService;
import com.lhs.vo.survey.SurveyUserVo;
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
    public Result<UserDataResponse> register(HttpServletRequest httpServletRequest, @RequestBody SurveyUserVo surveyUserVo) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        UserDataResponse response = surveyUserService.register(ipAddress, surveyUserVo.getUserName());
        return Result.success(response);
    }

    @ApiOperation("调查用户登录")
    @PostMapping("/login")
    public Result<UserDataResponse> login(HttpServletRequest httpServletRequest,@RequestBody SurveyUserVo surveyUserVo) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ApplicationConfig.Secret);  //加密
        UserDataResponse response = surveyUserService.login(ipAddress, surveyUserVo);
        return Result.success(response);
    }

    @ApiOperation("升级用户账号")
    @PostMapping("/user/update")
    public Result<Object> updateUser(@RequestBody SurveyUserVo surveyUserVo) {
        return surveyUserService.updateAccountStatus(surveyUserVo);
    }

    @ApiOperation("找回账号")
    @PostMapping("/user/retrieval")
    public Result<Object> retrievalAccount(@RequestBody Map<String,Object> map) {
        String cred = String.valueOf(map.get("cred"));
        return surveyUserService.retrievalAccountByCRED(cred);
    }
}
