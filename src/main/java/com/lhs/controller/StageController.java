package com.lhs.controller;


import com.lhs.common.entity.Result;
import com.lhs.entity.po.dev.HoneyCake;
import com.lhs.entity.po.stage.Item;
import com.lhs.entity.po.stage.Stage;
import com.lhs.entity.po.stage.StageResult;
import com.lhs.entity.po.stage.StorePerm;
import com.lhs.entity.vo.stage.StageResultVo;
import com.lhs.service.stage.*;
import com.lhs.entity.vo.stage.OrundumPerApResultVo;
import com.lhs.entity.vo.stage.StoreActVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@Tag(name ="API—关卡效率、材料价值、商店性价比")
@CrossOrigin()
@Slf4j
public class StageController {


    private final ItemService itemService;
    private final StageService stageService;
    private final StoreService storeService;
    private final StageResultService stageResultService;

    private final RedisTemplate<String,Object> redisTemplate;


    public StageController(ItemService itemService, StageService stageService, StoreService storeService, StageResultService stageResultService, RedisTemplate<String, Object> redisTemplate) {
        this.itemService = itemService;
        this.stageService = stageService;
        this.storeService = storeService;
        this.stageResultService = stageResultService;
        this.redisTemplate = redisTemplate;
    }

    @Operation(summary = "更新stage表")
    @PostMapping("auth/stage/update")
    public Result<HashMap<Object, Object>> authUpdateStageInfo(@RequestBody List<Stage> stageList) {
        HashMap<Object, Object> hashMap = stageService.updateStageList(stageList);
        return Result.success(hashMap);
    }


    //    @TakeCount(name = "物品价值表")
    @Operation(summary ="获取物品价值表")
    @GetMapping("/item/value")
    public Result<List<Item>> getItemValue(@RequestParam Double expCoefficient) {
        List<Item> items = itemService.getItemListCache("public."+expCoefficient);
        return Result.success(items);
    }

    //    @TakeCount(name = "关卡信息表")
    @Operation(summary ="获取关卡信息表")
    @GetMapping("/stage")
    public Result<Map<String, List<Stage>>> getStageV2() {
        Map<String, List<Stage>> stringListMap = stageService.getStageMap();

        return Result.success(stringListMap);
    }

    @Operation(summary ="获取关卡目录表")
    @GetMapping("/stage/menu")
    public Result<Map<String, List<Stage>>> getStageMenu() {
        Map<String, List<Stage>> stringListMap = stageService.getStageMenu();

        return Result.success(stringListMap);
    }

    //    @TakeCount(name = "蓝材料推荐关卡")
    @Operation(summary ="获取蓝材料推荐关卡按效率倒序")
    @GetMapping("/stage/t3")
    public Result<List<List<StageResultVo>>> queryStageResult_t3(@RequestParam Double expCoefficient) {

        String version = "public." + expCoefficient;

        List<List<StageResultVo>> stageResultVoList =
                stageResultService.queryStageResultDataT3V2(version);

        return Result.success(stageResultVoList);
    }

    //    @TakeCount(name = "绿材料推荐关卡")
    @Operation(summary ="获取绿材料推荐关卡按期望正序")
    @GetMapping("/stage/t2")
    public Result<List<List<StageResult>>> queryStageResult_t2(@RequestParam Double expCoefficient) {

        String version = "public." + expCoefficient;

        List<List<StageResult>> stageResultVoList =
                stageResultService.queryStageResultData_t2(version);

        return Result.success(stageResultVoList);
    }

    //    @TakeCount(name = "历史活动关卡")
    @Operation(summary ="获取历史活动关卡")
    @GetMapping("/stage/closed")
    public Result<List<List<StageResult>>> queryStageResult_closedActivities(@RequestParam Double expCoefficient) {

        String version = "public." + expCoefficient;

        List<List<StageResult>> stageResultVoList =
                stageResultService.queryStageResultData_closedActivities(version);

        return Result.success(stageResultVoList);
    }

    //    @TakeCount(name = "搓玉推荐关卡")
    @Operation(summary ="获取搓玉推荐关卡")
    @GetMapping("/stage/orundum")
    public Result<List<OrundumPerApResultVo>> queryStageResult_Orundum() {

        double expCoefficient = 0.625;
        String version = "public." + expCoefficient;

        List<OrundumPerApResultVo> OrundumPerApResultVoList = stageResultService
                .queryStageResultData_Orundum(version);

        return Result.success(OrundumPerApResultVoList);
    }



    //    @TakeCount(name = "查询关卡详细信息")
    @Operation(summary ="查询关卡详细信息")
    @GetMapping("/stage/detail")
    public Result<List<StageResult>> queryStageResult_detail(@RequestParam String stageId) {
        List<StageResult> stageResultVoList = stageResultService.queryStageResultDataDetailByStageId(stageId);
        return Result.success(stageResultVoList);
    }


    //    @TakeCount(name = "常驻商店性价比")
    @Operation(summary ="获取常驻商店性价比")
    @GetMapping("/store/perm")
    public Result<Map<String, List<StorePerm>>> getStorePermData() {
        Map<String, List<StorePerm>> storePerm = storeService.getStorePerm();
        return Result.success(storePerm);
    }

    //    @TakeCount(name = "活动商店性价比")
    @Operation(summary ="获取活动商店性价比")
    @GetMapping("/store/act")
    public Result<List<StoreActVo>> getStoreActData() {
        List<StoreActVo> storeActVoList = storeService.getStoreAct();
        return Result.success(storeActVoList);
    }

    @Operation(summary = "攒抽计算器活动排期")
    @GetMapping("/store/honeyCake")
    public Result<Map<String, HoneyCake>> getHoneyCake() {
        Map<String, HoneyCake> honeyCake = storeService.getHoneyCake();
        return Result.success(honeyCake);
    }

    @Operation(summary = "攒抽计算器活动排期")
    @GetMapping("/store/honeyCake/list")
    public Result<List<HoneyCake>> getHoneyCakeList() {
        List<HoneyCake> honeyCakeList = storeService.getHoneyCakeList();
        return Result.success(honeyCakeList);
    }

    @Operation(summary = "材料表导出（Excel格式）")
    @GetMapping("/item/export/excel")
    public void exportItemExcel(HttpServletResponse response) {
        itemService.exportItemExcel(response);
    }

    @Operation(summary = "材料表导出（Json格式）")
    @GetMapping("/item/export/json")
    public void exportItemJson(HttpServletResponse response) {
        itemService.exportItemJson(response);
    }


    @Operation(summary ="获取蓝材料推荐关卡按效率倒序")
    @GetMapping("/auth/stage/t3")
    public Result<List<List<StageResultVo>>> authQueryStageResult_t3(@RequestParam Double expCoefficient) {
        String version = "auth-" + expCoefficient;
        List<List<StageResultVo>> stageResultVoList =
                stageResultService.queryStageResultData_t3(version);

        return Result.success(stageResultVoList);
    }

    @GetMapping("/stage/resetCache")
    public Result<String> authResetCache() {
        String message = stageResultService.resetCache();
        return Result.success(message);
    }

    @GetMapping("/visits/resetCache")
    public Result<String> authVisitsResetCache() {
        Set<String> keys = redisTemplate.keys("visits:2023*");
        if(keys==null)return Result.success("失败了");
        for(String key:keys){
            redisTemplate.delete(key);
        }


        return Result.success("删除了"+keys.size());
    }
}
