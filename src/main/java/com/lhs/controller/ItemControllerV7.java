package com.lhs.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.lhs.common.util.Result;

import com.lhs.entity.dto.material.ItemInfoDTO;
import com.lhs.entity.dto.material.ItemValueConfigDTO;

import com.lhs.service.material.CustomItemService;
import com.lhs.service.material.StageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "材料相关API-v7")
public class ItemControllerV7 {

    private final CustomItemService customItemService;

    private final StageService stageService;
    
    public ItemControllerV7(CustomItemService customItemService, StageService stageService) {
        this.customItemService = customItemService;
        this.stageService = stageService;
    }

    @Operation(summary = "获取物品价值表")
    @PostMapping("/item/v7/value")
    public Result<List<ItemInfoDTO>> getItemValue(@RequestBody ItemValueConfigDTO itemValueConfigDTO) {
        List<ItemInfoDTO> items = customItemService.getCustomItemList(itemValueConfigDTO);
        return Result.success(items);
    }

    @Operation(summary = "获取关卡信息")
    @GetMapping("/stage/info")
    public Result< List<Map<String, Object>>> getStageInfo() {
        List<Map<String, Object>> stageInfo = stageService.getStageInfo();
        return Result.success(stageInfo);
    }
}
