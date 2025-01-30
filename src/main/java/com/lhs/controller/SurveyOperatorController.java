package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.PlayerInfoDTO;
import com.lhs.entity.dto.survey.WarehouseInventoryAPIParams;
import com.lhs.entity.po.survey.OperatorData;
import com.lhs.entity.po.survey.OperatorDataVo;
import com.lhs.service.survey.*;

import com.lhs.service.util.ArknightsGameDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name ="干员练度调查")

public class SurveyOperatorController {
    private final OperatorDataService operatorDataService;
    private final ArknightsGameDataService arknightsGameDataService;

    private final HypergryphService HypergryphService;

    private final OperatorProgressionStatisticsService operatorProgressionStatisticsService;

    private final WarehouseInfoService warehouseInfoService;

    public SurveyOperatorController(OperatorDataService operatorDataService,
                                    ArknightsGameDataService arknightsGameDataService,
                                    OperatorProgressionStatisticsService operatorProgressionStatisticsService,
                                    HypergryphService HypergryphService, WarehouseInfoService warehouseInfoService) {
        this.operatorDataService = operatorDataService;
        this.arknightsGameDataService = arknightsGameDataService;
        this.operatorProgressionStatisticsService = operatorProgressionStatisticsService;
        this.HypergryphService = HypergryphService;
        this.warehouseInfoService = warehouseInfoService;
    }

    @Operation(summary ="上传干员练度调查表")
    @PostMapping("/auth/survey/operator/upload")
    public Result<Object> uploadCharacterForm(HttpServletRequest httpServletRequest,@RequestBody List<OperatorData> operatorDataList) {
        Map<String, Object> hashMap = operatorDataService.manualUploadOperator(httpServletRequest, operatorDataList);
        return Result.success(hashMap);
    }

    @Operation(summary ="手动统计")
    @GetMapping("/survey/statistic")
    public Result<Object> statistic() {
        operatorProgressionStatisticsService.statisticsOperatorProgressionData();
        return Result.success();
    }

    @Operation(summary = "通过鹰角网络通行证获取凭证、密匙、玩家绑定数据")
    @PostMapping("/survey/hg/player-binding")
    public Result<Map<String, Object>> getCredAndTokenAndPlayerBindingsByHgToken(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        return Result.success(HypergryphService.getCredAndTokenAndPlayerBindingsByHgToken(token));
    }


    @Operation(summary ="通过森空岛导入干员练度V3")
    @PostMapping("/auth/survey/operator/import/skland/v3")
    public Result<Object> importSurveyCharacterFormBySKLandV3(HttpServletRequest httpServletRequest,@RequestBody PlayerInfoDTO playerInfoDTO) {

        return Result.success(operatorDataService.importSKLandPlayerInfoV3(httpServletRequest,playerInfoDTO));
    }



    @Operation(summary ="通过森空岛导入仓库材料")
    @PostMapping("/survey/warehouse-info/import/skland")
    public Result<Object> importWarehouseInfoBySKLand(@RequestBody WarehouseInventoryAPIParams warehouseInventoryAPIParams) {
        return Result.success(warehouseInfoService.saveWarehouseInventoryInfo(warehouseInventoryAPIParams));
    }

    @Operation(summary ="用户干员练度重置")
    @PostMapping("/survey/operator/reset")
    public Result<Object> operatorDataReset(@RequestBody Map<String,String> params) {
        String token = params.get("token");
        return operatorDataService.operatorDataReset(token);
    }


    @Operation(summary ="获取干员数据")
    @GetMapping("/survey/operator/info")
    public Result<Object> getOperatorTable(@RequestParam("token")String token) {
        List<OperatorDataVo> surveyDataCharList = operatorDataService.getUserOperatorInfo(token);
        surveyDataCharList.sort(Comparator.comparing(OperatorDataVo::getRarity).reversed());
        return Result.success(surveyDataCharList);
    }



    @Operation(summary ="干员练度调查表统计结果")
    @GetMapping("/survey/operator/result")
    public Result<Object> characterStatisticsResult() {
        HashMap<Object, Object> hashMap = operatorProgressionStatisticsService.getOperatorProgressionStatisticsResult();
        return Result.success(hashMap);
    }








}
