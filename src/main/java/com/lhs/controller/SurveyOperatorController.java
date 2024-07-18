package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.WarehouseInventoryAPIParams;
import com.lhs.entity.po.survey.OperatorData;
import com.lhs.entity.po.survey.OperatorPlan;
import com.lhs.entity.po.survey.OperatorDataVo;
import com.lhs.service.survey.*;
import com.lhs.entity.vo.survey.OperatorPlanVO;

import com.lhs.service.util.ArknightsGameDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

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



    private final ArknightsGameDataService arknightsGameDataService;

    private final HypergryphService HypergryphService;

    private final OperatorStatisticsService operatorStatisticsService;

    private final WarehouseInfoService warehouseInfoService;

    public SurveyOperatorController(OperatorDataService operatorDataService,
                                    ArknightsGameDataService arknightsGameDataService,
                                    OperatorStatisticsService operatorStatisticsService,
                                    HypergryphService HypergryphService, WarehouseInfoService warehouseInfoService) {
        this.operatorDataService = operatorDataService;
        this.arknightsGameDataService = arknightsGameDataService;
        this.operatorStatisticsService = operatorStatisticsService;
        this.HypergryphService = HypergryphService;
        this.warehouseInfoService = warehouseInfoService;
    }

    @Operation(summary ="上传干员练度调查表")
    @PostMapping("/character/upload")
    public Result<Object> uploadCharacterForm(@RequestParam String token, @RequestBody List<OperatorData> operatorDataList) {
        Map<String, Object> hashMap = operatorDataService.manualUploadOperator(token, operatorDataList);
        return Result.success(hashMap);
    }

    @Operation(summary ="手动统计")
    @GetMapping("/test")
    public Result<Object> test() {
        operatorStatisticsService.statisticsOperatorData();
        return Result.success();
    }

    @Operation(summary = "通过鹰角网络通行证获取凭证、密匙、玩家绑定数据")
    @PostMapping("/hg/player-binding")
    public Result<Map<String, Object>> getCredAndTokenAndPlayerBindingsByHgToken(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        return Result.success(HypergryphService.getCredAndTokenAndPlayerBindingsByHgToken(token));
    }

    @Operation(summary ="通过森空岛导入干员练度V2")
    @PostMapping("/operator/import/skland/v2")
    public Result<Object> importSurveyCharacterFormBySKLandV2(@RequestBody Map<String,String> params) {

        String token = params.get("token");
        String data = params.get("data");
        return Result.success(operatorDataService.importSKLandPlayerInfoV2(token, data));
    }

    @Operation(summary ="通过森空岛导入仓库材料")
    @PostMapping("/warehouse-info/import/skland")
    public Result<Object> importWarehouseInfoBySKLand(@RequestBody WarehouseInventoryAPIParams warehouseInventoryAPIParams) {
        return Result.success(warehouseInfoService.saveWarehouseInventoryInfo(warehouseInventoryAPIParams));
    }

    @Operation(summary ="用户干员练度重置")
    @PostMapping("/operator/reset")
    public Result<Object> operatorDataReset(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        return operatorDataService.operatorDataReset(token);
    }


    @Operation(summary ="获取干员数据")
    @PostMapping("/operator/info")
    public Result<Object> getOperatorTable(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        List<OperatorDataVo> surveyDataCharList = operatorDataService.getOperatorInfoByToken(token);
        surveyDataCharList.sort(Comparator.comparing(OperatorDataVo::getRarity).reversed());
        return Result.success(surveyDataCharList);
    }



    @Operation(summary ="干员练度调查表统计结果")
    @GetMapping("/operator/result")
    public Result<Object> characterStatisticsResult() {
        HashMap<Object, Object> hashMap = operatorStatisticsService.getCharStatisticsResult();
        return Result.success(hashMap);
    }








}
