package com.lhs.controller;


import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.util.ConfigUtil;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.Result;
import com.lhs.entity.survey.SurveyDataChar;
import com.lhs.entity.survey.SurveyDataCharVo;
import com.lhs.service.SurveyService;
import com.lhs.service.vo.SurveyUserVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@RestController
@Api(tags = "干员调查站API")
@RequestMapping(value = "/survey")
@CrossOrigin(maxAge = 86400)
public class SurveyController {

    @Resource
    private SurveyService surveyService;

    @ApiOperation("调查用户注册")
    @PostMapping("/register")
    public Result register(HttpServletRequest httpServletRequest,@RequestBody SurveyUserVo surveyUserVo) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);  //加密
        HashMap<Object, Object> register = surveyService.register(ipAddress, surveyUserVo.getUserName());
        return Result.success(register);
    }

    @ApiOperation("调查用户注册")
    @PostMapping("/login")
    public Result login(HttpServletRequest httpServletRequest,@RequestBody SurveyUserVo surveyUserVo) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);  //加密
        HashMap<Object, Object> register = surveyService.login(ipAddress, surveyUserVo.getUserName());
        return Result.success(register);
    }

    @ApiOperation("上传干员练度表")
    @PostMapping("/character")
    public Result uploadCharacterForm(@RequestParam String userName, @RequestBody List<SurveyDataChar> surveyDataCharList) {
        HashMap<Object, Object> hashMap = surveyService.uploadCharForm(userName, surveyDataCharList);
        return Result.success(hashMap);
    }

    @ApiOperation("找回干员练度表")
    @GetMapping("/find/character")
    public Result findCharacterForm(@RequestParam String userName) {
        List<SurveyDataCharVo> surveyDataCharList = surveyService.findCharacterForm(userName);
        return Result.success(surveyDataCharList);
    }

    @ApiOperation("干员练度表统计结果")
    @GetMapping("/result")
    public Result surveyDataCharStatisticsResult() {
        HashMap<Object, Object> hashMap = surveyService.charStatisticsResult();
        return Result.success(hashMap);
    }



}
