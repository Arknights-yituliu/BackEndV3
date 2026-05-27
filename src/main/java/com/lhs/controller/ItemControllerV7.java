package com.lhs.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.lhs.common.util.Result;

import com.lhs.entity.dto.item.custom.ItemInfoDTO;
import com.lhs.entity.dto.item.custom.ItemValueConfigDTO;

import com.lhs.service.material.CustomItemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "材料相关API-v6")
public class ItemControllerV7 {

    private final CustomItemService customItemService;

    public ItemControllerV7(CustomItemService customItemService) {
        this.customItemService = customItemService;
    }

    @Operation(summary = "获取物品价值表")
    @PostMapping("/item/v7/value")
    public Result<List<ItemInfoDTO>> getItemValue(@RequestBody ItemValueConfigDTO itemValueConfigDTO) {
        List<ItemInfoDTO> items = customItemService.getCustomItemList(itemValueConfigDTO);
        return Result.success(items);
    }

}
