package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.other.HoneyCake;
import com.lhs.entity.stage.Item;
import com.lhs.entity.stage.Stage;
import com.lhs.entity.stage.StageResult;
import com.lhs.entity.stage.StorePerm;
import com.lhs.service.stage.SelectStageResultService;
import com.lhs.service.stage.ItemService;
import com.lhs.service.stage.StageService;
import com.lhs.service.stage.StoreService;
import com.lhs.vo.stage.OrundumPerApResultVo;
import com.lhs.vo.stage.StageResultClosedVo;
import com.lhs.vo.stage.StageResultVo;
import com.lhs.vo.stage.StoreActVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Api(tags = "API—关卡效率、材料价值、商店性价比")
@CrossOrigin()
@Slf4j
public class StageController {
    @Resource
    private SelectStageResultService selectStageResultService;
    @Resource
    private ItemService itemService ;
    @Resource
    private StageService stageService ;
    @Resource
    private StoreService storeService;


//    @TakeCount(name = "物品价值表")
    @ApiOperation("获取物品价值表")
    @GetMapping("/item/value")
    @ApiImplicitParams({@ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625")})
    public Result<List<Item>> queryItemValue(@RequestParam Double expCoefficient) {
        List<Item> items = itemService.queryItemListCache(expCoefficient);
        return Result.success(items);
    }

//    @TakeCount(name = "关卡信息表")
    @ApiOperation("获取关卡信息表")
    @GetMapping("/stage")
    public Result<LinkedHashMap<String, List<Stage>>> queryStage() {
        LinkedHashMap<String, List<Stage>> stringListMap = stageService.queryStageTable();

        return Result.success(stringListMap);
    }

//    @TakeCount(name = "蓝材料推荐关卡")
    @ApiOperation("获取蓝材料推荐关卡按效率倒序")
    @GetMapping("/stage/t3")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625")
    public Result<List<List<StageResultVo>>> queryStageResult_t3(@RequestParam Double expCoefficient) {

        List<List<StageResultVo>> stageResultVoList = selectStageResultService.queryStageResultData_t3(expCoefficient, 100);

        return Result.success(stageResultVoList);
    }

//    @TakeCount(name = "绿材料推荐关卡")
    @ApiOperation("获取绿材料推荐关卡按期望正序")
    @GetMapping("/stage/t2")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625")
    public Result<List<List<StageResultVo>>> queryStageResult_t2(@RequestParam Double expCoefficient) {
        List<List<StageResultVo>> stageResultVoList = selectStageResultService.queryStageResultData_t2(expCoefficient, 100);

        return Result.success(stageResultVoList);
    }

//    @TakeCount(name = "历史活动关卡")
    @ApiOperation("获取历史活动关卡")
    @GetMapping("/stage/closed")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625")
    public Result<List<List<StageResultClosedVo>>> queryStageResult_closedActivities(@RequestParam Double expCoefficient) {
        List<List<StageResultClosedVo>> stageResultVoList = selectStageResultService.queryStageResultData_closedActivities(expCoefficient, 100);

        return Result.success(stageResultVoList);
    }

//    @TakeCount(name = "搓玉推荐关卡")
    @ApiOperation("获取搓玉推荐关卡")
    @GetMapping("/stage/orundum")
    public Result<List<OrundumPerApResultVo>> queryStageResult_Orundum() {
        List<OrundumPerApResultVo> OrundumPerApResultVoList = selectStageResultService.queryStageResultData_Orundum(0.625, 100);

        return Result.success(OrundumPerApResultVoList);
    }

//    @TakeCount(name = "新章关卡置信度情况")
    @ApiOperation("新章关卡置信度情况")
    @GetMapping("/stage/newChapter")
    @ApiImplicitParam(name = "zone", value = "章节名称", dataType = "String", paramType = "query", defaultValue = "11-")
    public Result<List<StageResultVo>> queryStageResult_newChapter(@RequestParam String zone) {
        List<StageResultVo> stageResultVoList = selectStageResultService.queryStageResultDataByZoneName(zone);

        return Result.success(stageResultVoList);
    }

//    @TakeCount(name = "查询关卡详细信息")
    @ApiOperation("查询关卡详细信息")
    @GetMapping("/stage/detail")
    @ApiImplicitParam(name = "stageCode", value = "关卡名称", dataType = "String", paramType = "query", defaultValue = "11-")
    public Result<List<StageResult>> queryStageResult_detail(@RequestParam String stageCode) {
        List<StageResult> stageResultVoList = selectStageResultService.queryStageResultDataDetailByStageCode(stageCode);
        return Result.success(stageResultVoList);
    }

//    @TakeCount(name = "查询关卡详细信息")
    @ApiOperation("查询关卡详细信息")
    @GetMapping("/stage/detail/{stageId}")
    public Result<List<StageResult>> queryStageResult_detailByStageId(@PathVariable String stageId) {
        List<StageResult> stageResultVoList = selectStageResultService.queryStageResultDataDetailByStageId(stageId);
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



}
