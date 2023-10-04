package com.lhs.controller;


import com.lhs.common.entity.Result;
import com.lhs.entity.dto.stage.StageParamDTO;
import com.lhs.entity.po.dev.HoneyCake;
import com.lhs.entity.po.stage.Item;
import com.lhs.entity.po.stage.Stage;
import com.lhs.entity.po.stage.StageResult;
import com.lhs.entity.po.stage.StorePerm;
import com.lhs.entity.vo.stage.StageResultVO;
import com.lhs.service.stage.*;
import com.lhs.entity.vo.stage.OrundumPerApResultVO;
import com.lhs.entity.vo.stage.StoreActVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Operation(summary ="查询新章的关卡效率")
    @GetMapping("/stage/chapter")
    public Result<List<StageResultVO>> getStageResultByZone(@RequestParam Double expCoefficient, @RequestParam String zone) {

        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setExpCoefficient(expCoefficient);
        String version = stageParamDTO.getVersion();

        List<StageResultVO> stageResultVOList =
                stageResultService.getStageResultDataByZone(version,zone);

        return Result.success(stageResultVOList);
    }

    //    @TakeCount(name = "蓝材料推荐关卡")
    @Operation(summary ="获取蓝材料推荐关卡按效率倒序")
    @GetMapping("/stage/t3")
    public Result<List<List<StageResultVO>>> getStageResultT3(@RequestParam Double expCoefficient) {

        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setExpCoefficient(expCoefficient);

        List<List<StageResultVO>> stageResultVoList =
                stageResultService.getStageResultDataT3V2(stageParamDTO);

        return Result.success(stageResultVoList);
    }

    //    @TakeCount(name = "绿材料推荐关卡")
    @Operation(summary ="获取绿材料推荐关卡按期望正序")
    @GetMapping("/stage/t2")
    public Result<List<List<StageResultVO>>> getStageResultT2(@RequestParam Double expCoefficient) {

        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setExpCoefficient(expCoefficient);

        List<List<StageResultVO>> stageResultVoList =
                stageResultService.getStageResultDataT2(stageParamDTO);

        return Result.success(stageResultVoList);
    }

    //    @TakeCount(name = "历史活动关卡")
    @Operation(summary ="获取历史活动关卡")
    @GetMapping("/stage/closed")
    public Result<List<List<StageResult>>> getStageResultClosedActivities(@RequestParam Double expCoefficient) {

        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setExpCoefficient(expCoefficient);
        List<List<StageResult>> stageResultVoList =
                stageResultService.getStageResultDataClosedStage(stageParamDTO);

        return Result.success(stageResultVoList);
    }

    //    @TakeCount(name = "搓玉推荐关卡")
    @Operation(summary ="获取搓玉推荐关卡")
    @GetMapping("/stage/orundum")
    public Result<List<OrundumPerApResultVO>> getStageResult_Orundum() {
        StageParamDTO stageParamDTO = new StageParamDTO();
        String version = stageParamDTO.getVersion();
        List<OrundumPerApResultVO> orundumPerApResultVOList = stageResultService
                .getStageResultDataOrundum(version);

        return Result.success(orundumPerApResultVOList);
    }



    //    @TakeCount(name = "查询关卡详细信息")
    @Operation(summary ="查询关卡详细信息")
    @GetMapping("/stage/detail")
    public Result<List<StageResult>> getStageResult_detail(@RequestParam String stageId) {
        List<StageResult> stageResultVoList = stageResultService.getStageResultDataDetailByStageId(stageId);
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
    public Result<List<StoreActVO>> getStoreActData() {
        List<StoreActVO> storeActVOList = storeService.getStoreAct();
        return Result.success(storeActVOList);
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
    public Result<List<List<StageResultVO>>> authQueryStageResult_t3(@RequestParam Double expCoefficient) {

        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setType("auth");
        List<List<StageResultVO>> stageResultVoList =
                stageResultService.getStageResultDataT3V2(stageParamDTO);

        return Result.success(stageResultVoList);
    }

    @GetMapping("/stage/resetCache")
    public Result<String> authResetCache() {
        String message = stageResultService.resetCache();
        return Result.success(message);
    }


}
