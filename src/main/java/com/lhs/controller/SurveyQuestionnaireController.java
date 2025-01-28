package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.QuestionnaireSubmitInfoDTO;
import com.lhs.service.survey.QuestionnaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name ="干员调查问卷")
public class SurveyQuestionnaireController {
    private final QuestionnaireService questionnaireService;
    public SurveyQuestionnaireController(QuestionnaireService questionnaireService) {
        this.questionnaireService = questionnaireService;
    }

    @Operation(summary ="上传干员调查问卷信息")
    @PostMapping("/survey/questionnaire/upload")
    public Result<Object> uploadQuestionnaireInfo(HttpServletRequest httpServletRequest, @RequestBody QuestionnaireSubmitInfoDTO questionnaireSubmitInfoDTO) {
        questionnaireService.uploadQuestionnaireResult(httpServletRequest,questionnaireSubmitInfoDTO);
        return Result.success();
    }

    @Operation(summary ="获取干员调查问卷信息结果")
    @PostMapping("/survey/questionnaire/result")
    public Result<Object> uploadQuestionnaireResult(@RequestParam("QuestionnaireType") Integer questionnaireType) {
        return Result.success(questionnaireService.getQuestionnaireResultByType(questionnaireType));
    }
}
