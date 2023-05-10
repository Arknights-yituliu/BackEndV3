package com.lhs.controller;


import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.FileConfig;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.Result;
import com.lhs.entity.SurveyDataChar;
import com.lhs.service.SurveyService;
import com.lhs.service.dto.MaaOperBoxVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Api(tags = "调查接口")
@RequestMapping(value = "/survey")
@CrossOrigin(maxAge = 86400)
public class SurveyController {

    @Resource
    private SurveyService surveyService;

    @ApiOperation("调查用户注册")
    @PostMapping("/register")
    public Result register(HttpServletRequest httpServletRequest,@RequestBody String userName) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), FileConfig.Secret);  //加密
        HashMap<Object, Object> register = surveyService.register(ipAddress, userName);
        return Result.success(register);
    }

    @ApiOperation("上传干员练度表")
    @PostMapping("/character")
    public Result uploadCharacterTable(@RequestParam String userName, @RequestBody List<SurveyDataChar> surveyDataCharList) {
        HashMap<Object, Object> hashMap = surveyService.uploadCharacterTable(userName, surveyDataCharList);
        return Result.success(hashMap);
    }




}
