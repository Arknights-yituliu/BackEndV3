package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.material.Item;
import com.lhs.entity.po.material.ItemCustom;
import com.lhs.entity.vo.material.*;
import com.lhs.service.material.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "材料相关API-v4")
public class ItemControllerV5 {

    private final ItemService itemService;

    private final StageService stageService;

    private final StoreService storeService;
    private final PackInfoService packInfoService;
    private final StageCalService stageCalService;
    private final StageResultService stageResultService;
    private final StageDropService stageDropService;

    public ItemControllerV5(ItemService itemService, StageService stageService, StoreService storeService,
                            StageResultService stageResultService,
                            PackInfoService packInfoService, StageCalService stageCalService, StageDropService stageDropService) {
        this.itemService = itemService;
        this.stageService = stageService;
        this.storeService = storeService;
        this.stageResultService = stageResultService;
        this.packInfoService = packInfoService;
        this.stageCalService = stageCalService;
        this.stageDropService = stageDropService;
    }



    @Operation(summary = "获取物品价值表")
    @PostMapping("/item/v5/value")
    public Result<List<Item>> getItemValue1(@RequestBody StageConfigDTO stageConfigDTO) {
        List<Item> items = itemService.getCustomItemList(stageConfigDTO);
        return Result.success(items);
    }

    @Operation(summary = "获取活动商店列表")
    @GetMapping("/item/v5/store/activity")
    public Result<List<ActivityStoreDataVO>> listActivityStoreData() {
        return Result.success(storeService.listActivityStoreData());
    }

    @Operation(summary = "获取礼包商店列表")
    @GetMapping("/item/v5/store/pack")
    public Result<List<PackInfoVOV5>> listPackInfo() {
        return Result.success(packInfoService.listPackInfo());
    }

    @Operation(summary = "获取礼包商店列表")
    @GetMapping("/item/v5/store/pack/version")
    public Result<String> getPackInfoVersion() {
        return Result.success(packInfoService.getPackInfoVersion());
    }

    @Operation(summary = "获取自定义材料价值表")
    @GetMapping("/item/v5/custom")
    public Result<List<ItemCustom>> listCustomItem() {
        return Result.success(packInfoService.listCustomItem());
    }

}
