package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.OperatorCarryQuestionnaireDTO;
import com.lhs.entity.vo.survey.OperatorCarryStatisticsResultVO;
import com.lhs.service.survey.QuestionnaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name ="干员调查问卷")
public class QuestionnaireController {
    private final QuestionnaireService questionnaireService;
    public QuestionnaireController(QuestionnaireService questionnaireService) {
        this.questionnaireService = questionnaireService;
    }

    @Operation(summary ="上传干员调查问卷信息")
    @PostMapping("/questionnaire/upload")
    public Result<Object> uploadQuestionnaireInfo(HttpServletRequest httpServletRequest, @RequestBody OperatorCarryQuestionnaireDTO operatorCarryQuestionnaireDTO) {
        questionnaireService.uploadQuestionnaireResult(httpServletRequest,operatorCarryQuestionnaireDTO);
        return Result.success();
    }


    @Operation(summary ="获取干员调查问卷信息结果")
    @GetMapping("/questionnaire/operator-carry/v2")
    public Result<OperatorCarryStatisticsResultVO> getOperatorCarryStatisticsResult(@RequestParam("questionnaireType") Integer questionnaireType,@RequestParam("timeGranularity") Integer timeGranularity) {
        return Result.success(questionnaireService.getOperatorCarryStatisticsResultByTypeAndTime(questionnaireType,timeGranularity));
    }


}
