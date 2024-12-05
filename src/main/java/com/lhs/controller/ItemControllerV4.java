package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.material.Item;
import com.lhs.entity.po.material.Stage;
import com.lhs.entity.po.material.StorePerm;
import com.lhs.entity.vo.material.*;
import com.lhs.service.material.*;
import com.lhs.task.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "材料相关API-v4")
public class ItemControllerV4 {

    private final ItemService itemService;

    private final StageService stageService;

    private final StoreService storeService;
    private final PackInfoService packInfoService;

    private final TaskService taskService;

    private final StageResultService stageResultService;

    public ItemControllerV4(ItemService itemService, StageService stageService, StoreService storeService,
                            TaskService taskService, StageResultService stageResultService,
                            PackInfoService packInfoService) {
        this.itemService = itemService;
        this.stageService = stageService;
        this.storeService = storeService;
        this.taskService = taskService;
        this.stageResultService = stageResultService;
        this.packInfoService = packInfoService;
    }

    @Operation(summary = "手动更新")
    @GetMapping("/stage/update")
    public Result<Map<String, List<Stage>>> updateStageResult() {
        taskService.updateStageResult();
        return Result.success();
    }


    @Operation(summary = "获取每种材料系列的关卡计算结果")
    @PostMapping("/stage/result/custom")
    public Result<Map<String, Object>> getStageResultOld(@RequestBody StageConfigDTO stageConfigDTO) {
        Map<String, Object> t3RecommendedStageV3 = stageResultService.getT3RecommendedStageV3(stageConfigDTO);
        return Result.success(t3RecommendedStageV3);
    }




}
