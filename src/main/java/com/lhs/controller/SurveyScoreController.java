package com.lhs.controller;


import com.lhs.common.util.Result;
import com.lhs.entity.po.survey.OperatorScore;
import com.lhs.service.survey.*;
import com.lhs.entity.vo.survey.OperatorScoreStatisticsVO;

import com.lhs.service.util.ArknightsGameDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@Tag(name ="干员调查站API")
@RequestMapping(value = "/survey")
public class SurveyScoreController {


    private final ArknightsGameDataService arknightsGameDataService;

    private final OperatorScoreService operatorScoreService;

    public SurveyScoreController(ArknightsGameDataService arknightsGameDataService, OperatorScoreService operatorScoreService) {
        this.arknightsGameDataService = arknightsGameDataService;
        this.operatorScoreService = operatorScoreService;
    }


    @Operation(summary ="上传干员风评表")
    @PostMapping("/score/upload")
    public Result<Object> uploadScoreForm(@RequestParam String token, @RequestBody List<OperatorScore> operatorScoreList) {
        HashMap<Object, Object> hashMap = operatorScoreService.uploadScoreForm(token, operatorScoreList);
        return Result.success(hashMap);
    }

    @Operation(summary ="干员风评表统计结果")
    @GetMapping("/score/result")
    public Result<List<OperatorScoreStatisticsVO>> scoreStatisticsResult() {
        List<OperatorScoreStatisticsVO> surveyScoreServiceScoreStatisticsResult = operatorScoreService.getScoreStatisticsResult();
        return Result.success(surveyScoreServiceScoreStatisticsResult);
    }






}
