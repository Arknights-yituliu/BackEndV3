package com.lhs.controller;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileType;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.Result;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.survey.OperatorPlan;
import com.lhs.entity.survey.SurveyOperator;
import com.lhs.entity.survey.SurveyOperatorVo;
import com.lhs.service.survey.*;
import com.lhs.vo.survey.OperatorPlanVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Api(tags = "干员练度调查")
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

    @ApiOperation("上传干员练度调查表")
    @PostMapping("/character/upload")
    public Result<Object> uploadCharacterForm(@RequestParam String token, @RequestBody List<SurveyOperator> surveyOperatorList) {
        Map<String, Object> hashMap = surveyOperatorService.uploadCharForm(token, surveyOperatorList);
        return Result.success(hashMap);
    }


    @ApiOperation("通过森空岛导入干员练度V2")
    @PostMapping("/operator/import/skland/v2")
    public Result<Object> importSurveyCharacterFormBySKLandV2(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        String data = params.get("data");
        return surveyOperatorService.importSKLandPlayerInfoV2(token, data);
    }

    @ApiOperation("用户干员练度重置")
    @PostMapping("/operator/reset")
    public Result<Object> operatorDataReset(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        return surveyOperatorService.operatorDataReset(token);
    }


    @ApiOperation("导入干员练度调查表")
    @PostMapping("/operator/import/excel")
    public Result<Object> importSurveyCharacterFormByExcel(MultipartFile file, @RequestParam String token) {

        boolean checkFileType = FileUtil.checkFileType(file, FileType.ZIP.getValue());
        if(!checkFileType) throw new ServiceException(ResultCode.FILE_NOT_IN_EXCEL_FORMAT);
        long size = file.getSize();
        if(size/1024>100) throw new ServiceException(ResultCode.USER_ACCOUNT_NOT_EXIST);
        Map<String, Object> hashMap = surveyOperatorService.importExcel(file, token);
        return Result.success(hashMap);
    }

    @ApiOperation("找回干员练度调查表")
    @PostMapping("/operator/retrieval")
    public Result<Object> findCharacterForm(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        List<SurveyOperatorVo> surveyDataCharList = surveyOperatorService.getOperatorForm(token);
        surveyDataCharList.sort(Comparator.comparing(SurveyOperatorVo::getRarity).reversed());
        return Result.success(surveyDataCharList);
    }

    @ApiOperation("干员练度调查表统计结果")
    @GetMapping("/operator/result")
    public Result<Object> characterStatisticsResult() {
        HashMap<Object, Object> hashMap = surveyStatisticsOperatorService.getCharStatisticsResult();
        return Result.success(hashMap);
    }

    @ApiOperation("导出干员练度调查表")
    @GetMapping("/operator/export")
    public void exportSurveyCharacterForm(HttpServletResponse response, @RequestParam String token) {
        surveyOperatorService.exportSurveyOperatorForm(response,token);
    }

    @ApiOperation("上传训练干员计划")
    @PostMapping("/operator/plan/save")
    public Result<Object> saveOperatorPlan(@RequestBody OperatorPlanVo OperatorPlanVo) {

        return operatorPlanService.savePlan(OperatorPlanVo);
    }

    @ApiOperation("获取训练干员计划")
    @PostMapping("/operator/plan")
    public Result<List<OperatorPlan>> getOperatorPlan(@RequestBody OperatorPlanVo OperatorPlanVo) {

        return operatorPlanService.getPlan(OperatorPlanVo);
    }
}