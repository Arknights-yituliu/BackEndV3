package com.lhs.controller;


import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.util.ConfigUtil;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.Result;
import com.lhs.entity.survey.SurveyCharacter;
import com.lhs.entity.survey.SurveyScore;
import com.lhs.service.SurveyScoreService;
import com.lhs.service.SurveyUserService;
import com.lhs.service.ToolService;
import com.lhs.service.vo.SurveyCharacterVo;
import com.lhs.service.SurveyCharacterService;
import com.lhs.service.vo.SurveyStatisticsScoreVo;
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
    private SurveyCharacterService surveyCharacterService;
    @Resource
    private SurveyUserService surveyUserService;
    @Resource
    private SurveyScoreService surveyScoreService;

    @Resource
    private ToolService toolService;

    @ApiOperation("调查用户注册")
    @PostMapping("/register")
    public Result<Object> register(HttpServletRequest httpServletRequest,@RequestBody SurveyUserVo surveyUserVo) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);  //加密
        HashMap<Object, Object> register = surveyUserService.register(ipAddress, surveyUserVo.getUserName());
        return Result.success(register);
    }

    @ApiOperation("调查用户登录")
    @PostMapping("/login")
    public Result<Object> login(HttpServletRequest httpServletRequest,@RequestBody SurveyUserVo surveyUserVo) {
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);  //加密
        HashMap<Object, Object> register = surveyUserService.login(ipAddress, surveyUserVo.getUserName());
        return Result.success(register);
    }

    @ApiOperation("上传干员练度表")
    @PostMapping("/character/upload")
    public Result<Object> uploadCharacterForm(@RequestParam String userName, @RequestBody List<SurveyCharacter> surveyCharacterList) {
        HashMap<Object, Object> hashMap = surveyCharacterService.uploadCharForm(userName, surveyCharacterList);
        return Result.success(hashMap);
    }

    @ApiOperation("找回干员练度表")
    @GetMapping("/character/retrieval")
    public Result<Object> findCharacterForm(@RequestParam String userName) {
        List<SurveyCharacterVo> surveyDataCharList = surveyCharacterService.findCharacterForm(userName);
        return Result.success(surveyDataCharList);
    }

    @ApiOperation("干员练度表统计结果")
    @GetMapping("/character/result")
    public Result<Object> characterStatisticsResult() {
        HashMap<Object, Object> hashMap = surveyCharacterService.charStatisticsResult();
        return Result.success(hashMap);
    }

    @ApiOperation("上传干员风评表")
    @PostMapping("/score/upload")
    public Result<Object> uploadScoreForm(@RequestParam String userName, @RequestBody List<SurveyScore> surveyScoreList) {
        HashMap<Object, Object> hashMap = surveyScoreService.uploadScoreForm(userName, surveyScoreList);
        return Result.success(hashMap);
    }

    @ApiOperation("干员风评表统计结果")
    @GetMapping("/score/result")
    public Result<List<SurveyStatisticsScoreVo>> scoreStatisticsResult() {
        List<SurveyStatisticsScoreVo> surveyScoreServiceScoreStatisticsResult = surveyScoreService.getScoreStatisticsResult();
        return Result.success(surveyScoreServiceScoreStatisticsResult);
    }

    @ApiOperation("干员基础信息")
    @GetMapping("/character/data")
    public Result<Object> getCharacterData() {
        HashMap<String, Object> characterData = toolService.getCharacterData();
        return Result.success(characterData);
    }




}
