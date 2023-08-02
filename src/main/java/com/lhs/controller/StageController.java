package com.lhs.controller;

import com.alibaba.fastjson.JSON;
import com.lhs.common.util.Log;
import com.lhs.common.util.Result;
import com.lhs.entity.other.HoneyCake;
import com.lhs.entity.stage.*;
import com.lhs.service.dev.OSSService;
import com.lhs.service.stage.*;
import com.lhs.vo.stage.OrundumPerApResultVo;
import com.lhs.vo.stage.StageAndItemCoefficient;
import com.lhs.vo.stage.StoreActVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Api(tags = "API—关卡效率、材料价值、商店性价比")
@CrossOrigin()
@Slf4j
public class StageController {


    private final ItemService itemService;
    private final StageService stageService;
    private final StoreService storeService;
    private final StageResultService stageResultService;

    public StageController(ItemService itemService,
                           StageResultService stageResultService,
                           StageService stageService,
                           StoreService storeService
                          ) {
        this.itemService = itemService;
        this.stageResultService = stageResultService;
        this.stageService = stageService;
        this.storeService = storeService;

    }

    @ApiOperation(value = "更新stage表")
    @PostMapping("auth/stage/update")
    public Result<HashMap<Object, Object>> authUpdateStageInfo(@RequestBody List<Stage> stageList) {
        HashMap<Object, Object> hashMap = stageService.updateStageList(stageList);
        return Result.success(hashMap);
    }


    //    @TakeCount(name = "物品价值表")
    @ApiOperation("获取物品价值表")
    @GetMapping("/item/value")
    @ApiImplicitParams({@ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625")})
    public Result<List<Item>> getItemValue(@RequestParam Double expCoefficient) {
        List<Item> items = itemService.getItemListCache("public-"+expCoefficient);
        return Result.success(items);
    }

    //    @TakeCount(name = "关卡信息表")
    @ApiOperation("获取关卡信息表")
    @GetMapping("/stage")
    public Result<Map<String, List<Stage>>> getStageV2() {
        Map<String, List<Stage>> stringListMap = stageService.getStageMap();

        return Result.success(stringListMap);
    }

    @ApiOperation("获取关卡目录表")
    @GetMapping("/stage/menu")
    public Result<Map<String, List<Stage>>> getStageMenu() {
        Map<String, List<Stage>> stringListMap = stageService.getStageMenu();

        return Result.success(stringListMap);
    }

    //    @TakeCount(name = "蓝材料推荐关卡")
    @ApiOperation("获取蓝材料推荐关卡按效率倒序")
    @GetMapping("/stage/t3")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625")
    public Result<List<List<StageResult>>> queryStageResult_t3(@RequestParam Double expCoefficient) {

        String version = "public-" + expCoefficient;

        List<List<StageResult>> stageResultVoList =
                stageResultService.queryStageResultData_t3(version);

        return Result.success(stageResultVoList);
    }

    //    @TakeCount(name = "绿材料推荐关卡")
    @ApiOperation("获取绿材料推荐关卡按期望正序")
    @GetMapping("/stage/t2")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625")
    public Result<List<List<StageResult>>> queryStageResult_t2(@RequestParam Double expCoefficient) {

        String version = "public-" + expCoefficient;

        List<List<StageResult>> stageResultVoList =
                stageResultService.queryStageResultData_t2(version);

        return Result.success(stageResultVoList);
    }

    //    @TakeCount(name = "历史活动关卡")
    @ApiOperation("获取历史活动关卡")
    @GetMapping("/stage/closed")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625")
    public Result<List<List<StageResult>>> queryStageResult_closedActivities(@RequestParam Double expCoefficient) {

        String version = "public-" + expCoefficient;

        List<List<StageResult>> stageResultVoList =
                stageResultService.queryStageResultData_closedActivities(version);

        return Result.success(stageResultVoList);
    }

    //    @TakeCount(name = "搓玉推荐关卡")
    @ApiOperation("获取搓玉推荐关卡")
    @GetMapping("/stage/orundum")
    public Result<List<OrundumPerApResultVo>> queryStageResult_Orundum() {

        double expCoefficient = 0.625;
        String version = "public-" + expCoefficient;

        List<OrundumPerApResultVo> OrundumPerApResultVoList = stageResultService
                .queryStageResultData_Orundum(version);

        return Result.success(OrundumPerApResultVoList);
    }



    //    @TakeCount(name = "查询关卡详细信息")
    @ApiOperation("查询关卡详细信息")
    @GetMapping("/stage/detail")
    @ApiImplicitParam(name = "stageId", value = "关卡名称", dataType = "String", paramType = "query", defaultValue = "1-7")
    public Result<List<StageResult>> queryStageResult_detail(@RequestParam String stageId) {
        List<StageResult> stageResultVoList = stageResultService.queryStageResultDataDetailByStageId(stageId);
        return Result.success(stageResultVoList);
    }


    //    @TakeCount(name = "常驻商店性价比")
    @ApiOperation("获取常驻商店性价比")
    @GetMapping("/store/perm")
    public Result<Map<String, List<StorePerm>>> getStorePermData() {
        Map<String, List<StorePerm>> storePerm = storeService.getStorePerm();
        return Result.success(storePerm);
    }

    //    @TakeCount(name = "活动商店性价比")
    @ApiOperation("获取活动商店性价比")
    @GetMapping("/store/act")
    public Result<List<StoreActVo>> getStoreActData() {
        List<StoreActVo> storeActVoList = storeService.getStoreAct();
        return Result.success(storeActVoList);
    }

    @ApiOperation(value = "攒抽计算器活动排期")
    @GetMapping("/store/honeyCake")
    public Result<Map<String, HoneyCake>> getHoneyCake() {
        Map<String, HoneyCake> honeyCake = storeService.getHoneyCake();
        return Result.success(honeyCake);
    }

    @ApiOperation(value = "攒抽计算器活动排期")
    @GetMapping("/store/honeyCake/list")
    public Result<List<HoneyCake>> getHoneyCakeList() {
        List<HoneyCake> honeyCakeList = storeService.getHoneyCakeList();
        return Result.success(honeyCakeList);
    }

    @ApiOperation(value = "材料表导出（Excel格式）")
    @GetMapping("/item/export/excel")
    public void exportItemExcel(HttpServletResponse response) {
        itemService.exportItemExcel(response);
    }

    @ApiOperation(value = "材料表导出（Json格式）")
    @GetMapping("/item/export/json")
    public void exportItemJson(HttpServletResponse response) {
        itemService.exportItemJson(response);
    }


    @ApiOperation("获取蓝材料推荐关卡按效率倒序")
    @GetMapping("auth/stage/t3")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625")
    public Result<List<List<StageResult>>> authQueryStageResult_t3(@RequestParam Double expCoefficient) {
        String version = "auth-" + expCoefficient;
        List<List<StageResult>> stageResultVoList =
                stageResultService.queryStageResultData_t3(version);

        return Result.success(stageResultVoList);
    }

    @GetMapping("/stage/resetCache")
    public Result<String> authResetCache() {

        String message = stageResultService.resetCache();

        return Result.success(message);
    }
}
