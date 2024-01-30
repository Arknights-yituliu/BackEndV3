package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.po.item.PackItem;
import com.lhs.entity.po.item.Stage;
import com.lhs.entity.vo.dev.PageVisitsVo;
import com.lhs.entity.vo.dev.VisitsTimeVo;
import com.lhs.entity.vo.item.PackPromotionRatioVO;
import com.lhs.entity.vo.item.StoreActVO;
import com.lhs.service.dev.UserService;
import com.lhs.service.dev.VisitsService;
import com.lhs.service.item.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "一图流后台")

public class AdminController {

    private final StoreService storeService;

    private final UserService userService;

    private final VisitsService visitsService;

    public AdminController(StoreService storeService, UserService userService, VisitsService visitsService) {
        this.storeService = storeService;
        this.userService = userService;
        this.visitsService = visitsService;
    }

    @Operation(summary = "更新商店礼包")
    @PostMapping("/admin/store/pack/update")
    public Result<PackPromotionRatioVO> updateStageResult(@RequestBody PackPromotionRatioVO packPromotionRatioVO) {
        PackPromotionRatioVO pack = storeService.updateStorePackById(packPromotionRatioVO);
        return Result.success(pack);
    }

    @Operation(summary = "根据id获取礼包")
    @GetMapping("/admin/store/pack")
    public Result<PackPromotionRatioVO> updateStageResult(@RequestParam(required = false, defaultValue = "1") String id) {
        PackPromotionRatioVO pack = storeService.getPackById(id);
        return Result.success(pack);
    }

    @Operation(summary = "获取礼包材料表")
    @GetMapping("/item/list")
    public Result<List<PackItem>> getItemList() {
        List<PackItem> packItemList = storeService.getItemList();
        return Result.success(packItemList);
    }

    @Operation(summary = "更新礼包材料表")
    @PostMapping("/admin/item/update")
    public Result<PackItem> saveOrUpdatePackItem(@RequestBody PackItem newPackItem){
        PackItem packItem = storeService.saveOrUpdatePackItem(newPackItem);
        return Result.success(packItem);
    }

    @Operation(summary = "删除礼包材料")
    @GetMapping("/admin/item/delete")
    public Result<Object> deletePackItem(@RequestParam String id){
        storeService.deletePackItemById(id);
        return Result.success();
    }

    @Operation(summary = "更新材料礼包状态")
    @GetMapping("/admin/store/pack/update/state")
    public Result<Object> updatePackState(@RequestParam String id,@RequestParam Integer state){
        storeService.updatePackState(id,state);
        return Result.success();
    }

    @PostMapping("/admin/statistics")
    public Result<List<PageVisitsVo>> queryVisits(@RequestBody VisitsTimeVo visitsTimeVo) {
        List<PageVisitsVo> pageVisitsVos = visitsService.getVisits(visitsTimeVo);
        return Result.success(pageVisitsVos);
    }


    @Operation(summary = "更新活动商店性价比(新")
    @PostMapping("/admin/act/update")
    public Result<Object> updateActStoreByActName(HttpServletRequest request, @RequestBody StoreActVO storeActVo) {
        Boolean level = userService.developerLevel(request);
        String message = storeService.updateActStoreByActName(storeActVo, level);
        return Result.success(message);
    }
}
