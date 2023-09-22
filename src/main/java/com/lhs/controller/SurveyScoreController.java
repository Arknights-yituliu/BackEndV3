package com.lhs.controller;


import com.lhs.common.util.*;
import com.lhs.entity.survey.SurveyScore;
import com.lhs.service.survey.*;
import com.lhs.vo.survey.SurveyStatisticsScoreVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@Api(tags = "干员调查站API")
@RequestMapping(value = "/survey")
@CrossOrigin(maxAge = 86400)
public class SurveyScoreController {


    private final OperatorBaseDataService operatorBaseDataService;

    private final SurveyScoreService surveyScoreService;

    public SurveyScoreController(OperatorBaseDataService operatorBaseDataService, SurveyScoreService surveyScoreService) {
        this.operatorBaseDataService = operatorBaseDataService;
        this.surveyScoreService = surveyScoreService;
    }


    @ApiOperation("上传干员风评表")
    @PostMapping("/score/upload")
    public Result<Object> uploadScoreForm(@RequestParam String token, @RequestBody List<SurveyScore> surveyScoreList) {
        HashMap<Object, Object> hashMap = surveyScoreService.uploadScoreForm(token, surveyScoreList);
        return Result.success(hashMap);
    }

    @ApiOperation("干员风评表统计结果")
    @GetMapping("/score/result")
    public Result<List<SurveyStatisticsScoreVo>> scoreStatisticsResult() {
        List<SurveyStatisticsScoreVo> surveyScoreServiceScoreStatisticsResult = surveyScoreService.getScoreStatisticsResult();
        return Result.success(surveyScoreServiceScoreStatisticsResult);
    }






}