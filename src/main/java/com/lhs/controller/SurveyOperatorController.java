package com.lhs.controller;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileType;
import com.lhs.common.util.FileUtil;
import com.lhs.common.entity.Result;
import com.lhs.common.entity.ResultCode;
import com.lhs.entity.po.survey.OperatorPlan;
import com.lhs.entity.po.survey.SurveyOperator;
import com.lhs.entity.po.survey.SurveyOperatorVo;
import com.lhs.service.survey.*;
import com.lhs.entity.vo.survey.OperatorPlanVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name ="干员练度调查")
@RequestMapping(value = "/survey")
@CrossOrigin()
public class SurveyOperatorController {
    private final SurveyOperatorService surveyOperatorService;

    private final OperatorPlanService operatorPlanService;

    private final OperatorBaseDataService operatorBaseDataService;

    private final SurveyStatisticsOperatorService surveyStatisticsOperatorService;

    public SurveyOperatorController(SurveyOperatorService surveyOperatorService, OperatorPlanService operatorPlanService, OperatorBaseDataService operatorBaseDataService, SurveyStatisticsOperatorService surveyStatisticsOperatorService) {
        this.surveyOperatorService = surveyOperatorService;
        this.operatorPlanService = operatorPlanService;
        this.operatorBaseDataService = operatorBaseDataService;
        this.surveyStatisticsOperatorService = surveyStatisticsOperatorService;
    }

    @Operation(summary ="上传干员练度调查表")
    @PostMapping("/character/upload")
    public Result<Object> uploadCharacterForm(@RequestParam String token, @RequestBody List<SurveyOperator> surveyOperatorList) {
        Map<String, Object> hashMap = surveyOperatorService.uploadCharForm(token, surveyOperatorList);
        return Result.success(hashMap);
    }

    @Operation(summary ="通过uid找回森空岛练度")
    @PostMapping("/operator/retrieval/uid")
    public Result<Object> retrievalCharacterForm(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        String uid = params.get("uid");
        List<SurveyOperatorVo> surveyDataCharList = surveyOperatorService.retrievalCharacterForm(token,uid);
        surveyDataCharList.sort(Comparator.comparing(SurveyOperatorVo::getRarity).reversed());
        return Result.success(surveyDataCharList);
    }

    @Operation(summary ="通过森空岛导入干员练度V2")
    @PostMapping("/operator/import/skland/v2")
    public Result<Object> importSurveyCharacterFormBySKLandV2(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        String data = params.get("data");
        return surveyOperatorService.importSKLandPlayerInfoV2(token, data);
    }

    @Operation(summary ="用户干员练度重置")
    @PostMapping("/operator/reset")
    public Result<Object> operatorDataReset(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        return surveyOperatorService.operatorDataReset(token);
    }


    @Operation(summary ="导入干员练度调查表")
    @PostMapping("/operator/import/excel")
    public Result<Object> importSurveyCharacterFormByExcel(MultipartFile file, @RequestParam String token) {

        boolean checkFileType = FileUtil.checkFileType(file, FileType.ZIP.getValue());
        if(!checkFileType) throw new ServiceException(ResultCode.FILE_NOT_IN_EXCEL_FORMAT);
        long size = file.getSize();
        if(size/1024>100) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        Map<String, Object> hashMap = surveyOperatorService.importExcel(file, token);
        return Result.success(hashMap);
    }

    @Operation(summary ="找回干员练度调查表")
    @PostMapping("/operator/retrieval")
    public Result<Object> findCharacterForm(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        List<SurveyOperatorVo> surveyDataCharList = surveyOperatorService.getOperatorForm(token);
        surveyDataCharList.sort(Comparator.comparing(SurveyOperatorVo::getRarity).reversed());
        return Result.success(surveyDataCharList);
    }

    @Operation(summary ="干员练度调查表统计结果")
    @GetMapping("/operator/result")
    public Result<Object> characterStatisticsResult() {
        HashMap<Object, Object> hashMap = surveyStatisticsOperatorService.getCharStatisticsResult();
        return Result.success(hashMap);
    }

    @Operation(summary ="导出干员练度调查表")
    @GetMapping("/operator/export")
    public void exportSurveyCharacterForm(HttpServletResponse response, @RequestParam String token) {
        surveyOperatorService.exportSurveyOperatorForm(response,token);
    }

    @Operation(summary ="上传训练干员计划")
    @PostMapping("/operator/plan/save")
    public Result<Object> saveOperatorPlan(@RequestBody OperatorPlanVO OperatorPlanVo) {

        return operatorPlanService.savePlan(OperatorPlanVo);
    }

    @Operation(summary ="获取训练干员计划")
    @PostMapping("/operator/plan")
    public Result<List<OperatorPlan>> getOperatorPlan(@RequestBody OperatorPlanVO OperatorPlanVo) {

        return operatorPlanService.getPlan(OperatorPlanVo);
    }
}
