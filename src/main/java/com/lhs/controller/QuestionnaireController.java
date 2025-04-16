package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.OperatorCarryQuestionnaireDTO;
import com.lhs.entity.vo.survey.OperatorCarryRateStatisticsVO;
import com.lhs.service.survey.OperatorCarryRateService;
import com.lhs.service.survey.QuestionnaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@Tag(name ="干员调查问卷")
public class QuestionnaireController {
    private final QuestionnaireService questionnaireService;

    private final OperatorCarryRateService operatorCarryRateService;
    public QuestionnaireController(QuestionnaireService questionnaireService,
                                   OperatorCarryRateService operatorCarryRateService) {
        this.questionnaireService = questionnaireService;
        this.operatorCarryRateService = operatorCarryRateService;
    }

    @Operation(summary ="上传干员调查问卷信息")
    @PostMapping("/questionnaire/upload")
    public Result<Object> uploadQuestionnaireInfo(HttpServletRequest httpServletRequest, @RequestBody OperatorCarryQuestionnaireDTO operatorCarryQuestionnaireDTO) {
        questionnaireService.uploadQuestionnaireResult(httpServletRequest,operatorCarryQuestionnaireDTO);
        return Result.success();
    }


    @Operation(summary ="获取干员调查问卷信息结果")
    @GetMapping("/questionnaire/operator-carry/v2")
    public Result<OperatorCarryRateStatisticsVO> getOperatorCarryStatisticsResult(@RequestParam("questionnaireType") Integer questionnaireType,
                                                                                  @RequestParam("startTime") Long start,
                                                                                  @RequestParam("endTime") Long end) {

        return Result.success(operatorCarryRateService.getOperatorCarryRate(questionnaireType,new Date(start),new Date(end)));
    }

    @Operation(summary ="迁移数据")
    @GetMapping("/questionnaire/move")
    public Result<Object> getOperatorCarryStatisticsResult() {
        operatorCarryRateService.moveDate();
        return Result.success();
    }


}
