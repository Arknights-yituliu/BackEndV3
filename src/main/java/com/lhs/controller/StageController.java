package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.admin.HoneyCake;
import com.lhs.entity.vo.material.*;
import com.lhs.service.material.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "材料相关API-v2")
public class StageController {

    private final ItemService itemService;
    private final StageService stageService;
    private final StoreService storeService;
    private final StageResultService stageResultService;

    private final RedisTemplate<String, Object> redisTemplate;




    public StageController(ItemService itemService,
                           StageService stageService,
                           StoreService storeService,
                           StageResultService stageResultService,
                           RedisTemplate<String, Object> redisTemplate) {
        this.itemService = itemService;
        this.stageService = stageService;
        this.storeService = storeService;
        this.stageResultService = stageResultService;
        this.redisTemplate = redisTemplate;
    }





    //    @TakeCount(name = "蓝材料推荐关卡")
    @Operation(summary = "获取蓝材料推荐关卡按效率倒序")
    @GetMapping("/stage/t3/v2")
    public Result<Map<String, Object>> getStageResultT3(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                             @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setSampleSize(sampleSize);
        stageConfigDTO.setExpCoefficient(expCoefficient);
        Map<String, Object> hashMap = stageResultService.getT3RecommendedStageV2(stageConfigDTO);
        return Result.success(hashMap);
    }






    //    @TakeCount(name = "绿材料推荐关卡")
    @Operation(summary = "获取绿材料推荐关卡按期望倒序")
    @GetMapping("/stage/t2/v2")
    public Result<List<RecommendedStageVO>> getStageResultT2(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                             @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setSampleSize(sampleSize);
        stageConfigDTO.setExpCoefficient(expCoefficient);
        List<RecommendedStageVO> recommendedStageVOList = stageResultService.getT2RecommendedStage(stageConfigDTO);

        return Result.success(recommendedStageVOList);
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




    @GetMapping("/stage/resetCache")
    public Result<List<String>> authResetCache() {
        List<String> message = stageResultService.resetCache();
        return Result.success(message);
    }





    //    @TakeCount(name = "蓝材料推荐关卡")
    @Operation(summary = "获取蓝材料推荐关卡按效率倒序v1")
    @GetMapping("/stage/t3")
    public Result<List<List<StageResultVO>>> getStageResultT3V1(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                              @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setSampleSize(sampleSize);
        stageConfigDTO.setExpCoefficient(expCoefficient);
        return Result.success(stageResultService.getT3RecommendedStageV1(stageConfigDTO.getVersionCode()));
    }

    //    @TakeCount(name = "绿材料推荐关卡")
    @Operation(summary = "获取绿材料推荐关卡按期望倒序v1")
    @GetMapping("/stage/t2")
    public Result<List<List<StageResultVO>>> getStageResultT2V1(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                              @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setSampleSize(sampleSize);
        stageConfigDTO.setExpCoefficient(expCoefficient);

        return Result.success(stageResultService.getT2RecommendedStageV1(stageConfigDTO));
    }

    @Operation(summary = "获取历史活动关卡v1")
    @GetMapping("/stage/closed")
    public Result<List<ActStageVO>> getStageResultClosedV1(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                                @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setSampleSize(sampleSize);
        stageConfigDTO.setExpCoefficient(expCoefficient);

        return Result.success(stageResultService.getHistoryActStage(stageConfigDTO));
    }

}
