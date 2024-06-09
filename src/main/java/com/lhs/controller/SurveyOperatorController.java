package com.lhs.controller;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileType;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.Result;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.po.survey.OperatorPlan;
import com.lhs.entity.po.survey.OperatorData;
import com.lhs.entity.po.survey.OperatorDataVo;
import com.lhs.entity.vo.survey.AKPlayerBindingListVO;
import com.lhs.service.survey.*;
import com.lhs.entity.vo.survey.OperatorPlanVO;

import com.lhs.service.util.ArknightsGameDataService;
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
public class SurveyOperatorController {
    private final OperatorDataService operatorDataService;

    private final OperatorPlanService operatorPlanService;

    private final ArknightsGameDataService arknightsGameDataService;

    private final HypergryphService hypergryphService;

    private final OperatorStatisticsService operatorStatisticsService;

    public SurveyOperatorController(OperatorDataService operatorDataService,
                                    OperatorPlanService operatorPlanService,
                                    ArknightsGameDataService arknightsGameDataService,
                                    OperatorStatisticsService operatorStatisticsService,
                                    HypergryphService hypergryphService) {
        this.operatorDataService = operatorDataService;
        this.operatorPlanService = operatorPlanService;
        this.arknightsGameDataService = arknightsGameDataService;
        this.operatorStatisticsService = operatorStatisticsService;
        this.hypergryphService = hypergryphService;
    }

    @Operation(summary ="上传干员练度调查表")
    @PostMapping("/character/upload")
    public Result<Object> uploadCharacterForm(@RequestParam String token, @RequestBody List<OperatorData> operatorDataList) {
        Map<String, Object> hashMap = operatorDataService.manualUploadOperator(token, operatorDataList);
        return Result.success(hashMap);
    }

    @Operation(summary ="通过鹰角网络通行证获取玩家绑定数据")
    @PostMapping("/hg/player-binding")
    public Result<AKPlayerBindingListVO> getPlayerBindingsByHgToken(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        return Result.success(hypergryphService.getPlayerBindingsByHGToken(token));
    }

    @Operation(summary ="通过森空岛导入干员练度V2")
    @PostMapping("/operator/import/skland/v2")
    public Result<Object> importSurveyCharacterFormBySKLandV2(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        String data = params.get("data");
        return Result.success(operatorDataService.importSKLandPlayerInfoV2(token, data));
    }

    @Operation(summary ="用户干员练度重置")
    @PostMapping("/operator/reset")
    public Result<Object> operatorDataReset(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        return operatorDataService.operatorDataReset(token);
    }


    @Operation(summary ="导入干员练度调查表")
    @PostMapping("/operator/import/excel")
    public Result<Object> importSurveyCharacterFormByExcel(MultipartFile file, @RequestParam String token) {

        boolean checkFileType = FileUtil.checkFileType(file, FileType.ZIP.getValue());
        if(!checkFileType) throw new ServiceException(ResultCode.FILE_NOT_IN_EXCEL_FORMAT);
        long size = file.getSize();
        if(size/1024>100) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        Map<String, Object> hashMap = operatorDataService.importExcel(file, token);
        return Result.success(hashMap);
    }

    @Operation(summary ="获取干员数据")
    @PostMapping("/operator/table")
    public Result<Object> getOperatorTable(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        List<OperatorDataVo> surveyDataCharList = operatorDataService.getOperatorTable(token);
        surveyDataCharList.sort(Comparator.comparing(OperatorDataVo::getRarity).reversed());
        return Result.success(surveyDataCharList);
    }

    @Operation(summary ="干员练度调查表统计结果")
    @GetMapping("/operator/result")
    public Result<Object> characterStatisticsResult() {
        HashMap<Object, Object> hashMap = operatorStatisticsService.getCharStatisticsResult();
        return Result.success(hashMap);
    }































    @Operation(summary ="导出干员练度调查表")
    @GetMapping("/operator/export")
    public void exportSurveyCharacterForm(HttpServletResponse response, @RequestParam String token) {
        operatorDataService.exportSurveyOperatorForm(response,token);
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
