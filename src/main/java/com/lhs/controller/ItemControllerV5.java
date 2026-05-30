package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.vo.material.*;
import com.lhs.service.material.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "材料相关API-v4")
public class ItemControllerV5 {


    private final StoreService storeService;
    private final PackInfoService packInfoService;



    public ItemControllerV5( StoreService storeService, PackInfoService packInfoService) {

        this.storeService = storeService;
        this.packInfoService = packInfoService;

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

    @Operation(summary = "获取礼包商店列表版本")
    @GetMapping("/item/v5/store/pack/version")
    public Result<String> getPackInfoVersion() {
        return Result.success(packInfoService.getPackInfoVersion());
    }



}
