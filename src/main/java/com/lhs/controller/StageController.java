package com.lhs.controller;

import com.lhs.common.annotation.TakeCount;
import com.lhs.common.util.Result;
import com.lhs.entity.stage.Item;
import com.lhs.entity.stage.Stage;
import com.lhs.entity.stage.StageResult;
import com.lhs.entity.stage.StorePerm;
import com.lhs.service.SelectStageResultService;
import com.lhs.service.ItemService;
import com.lhs.service.StageService;
import com.lhs.service.StoreService;
import com.lhs.service.vo.OrundumPerApResultVo;
import com.lhs.service.vo.StageResultActVo;
import com.lhs.service.vo.StageResultVo;
import com.lhs.service.vo.StoreActVo;
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


    @TakeCount(method = "物品价值表")
    @ApiOperation("获取物品价值表")
    @GetMapping("/item/value")
    @ApiImplicitParams({@ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625")})
    public Result<List<Item>> queryItemValue(@RequestParam Double expCoefficient) {
        List<Item> items = itemService.queryItemListById(expCoefficient,1000);
        return Result.success(items);
    }

    @TakeCount(method = "关卡信息表")
    @ApiOperation("获取关卡信息表")
    @GetMapping("/stage")
    public Result<LinkedHashMap<String, List<Stage>>> queryStage() {
        LinkedHashMap<String, List<Stage>> stringListMap = stageService.queryStageTable();

        return Result.success(stringListMap);
    }

    @TakeCount(method = "蓝材料推荐关卡")
    @ApiOperation("获取蓝材料推荐关卡按效率倒序")
    @GetMapping("/stage/t3")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625")
    public Result<List<List<StageResultVo>>> queryStageResult_t3(@RequestParam Double expCoefficient) {

        List<List<StageResultVo>> stageResultVoList = selectStageResultService.queryStageResultData_t3(expCoefficient, 100);

        return Result.success(stageResultVoList);
    }

    @TakeCount(method = "绿材料推荐关卡")
    @ApiOperation("获取绿材料推荐关卡按期望正序")
    @GetMapping("/stage/t2")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625")
    public Result<List<List<StageResultVo>>> queryStageResult_t2(@RequestParam Double expCoefficient) {
        List<List<StageResultVo>> stageResultVoList = selectStageResultService.queryStageResultData_t2(expCoefficient, 100);

        return Result.success(stageResultVoList);
    }

    @TakeCount(method = "历史活动关卡")
    @ApiOperation("获取历史活动关卡")
    @GetMapping("/stage/closed")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625")
    public Result<List<List<StageResultActVo>>> queryStageResult_closedActivities(@RequestParam Double expCoefficient) {
        List<List<StageResultActVo>> stageResultVoList = selectStageResultService.queryStageResultData_closedActivities(expCoefficient, 100);

        return Result.success(stageResultVoList);
    }

    @TakeCount(method = "搓玉推荐关卡")
    @ApiOperation("获取搓玉推荐关卡")
    @GetMapping("/stage/orundum")
    public Result<List<OrundumPerApResultVo>> queryStageResult_Orundum() {
        List<OrundumPerApResultVo> OrundumPerApResultVoList = selectStageResultService.queryStageResultData_Orundum(0.625, 100);

        return Result.success(OrundumPerApResultVoList);
    }

    @TakeCount(method = "新章关卡置信度情况")
    @ApiOperation("新章关卡置信度情况")
    @GetMapping("/stage/newChapter")
    @ApiImplicitParam(name = "zone", value = "章节名称", dataType = "String", paramType = "query", defaultValue = "11-")
    public Result<List<StageResultVo>> queryStageResult_newChapter(@RequestParam String zone) {
        List<StageResultVo> stageResultVoList = selectStageResultService.queryStageResultDataByZoneName(zone);

        return Result.success(stageResultVoList);
    }

    @TakeCount(method = "查询关卡详细信息")
    @ApiOperation("查询关卡详细信息")
    @GetMapping("/stage/detail")
    @ApiImplicitParam(name = "stageCode", value = "关卡名称", dataType = "String", paramType = "query", defaultValue = "11-")
    public Result<List<StageResult>> queryStageResult_detail(@RequestParam String stageCode) {
        List<StageResult> stageResultVoList = selectStageResultService.queryStageResultDataDetailByStageCode(stageCode);
        return Result.success(stageResultVoList);
    }

    @TakeCount(method = "查询关卡详细信息")
    @ApiOperation("查询关卡详细信息")
    @GetMapping("/stage/detail/{stageId}")
    public Result<List<StageResult>> queryStageResult_detailByStageId(@PathVariable String stageId) {
        List<StageResult> stageResultVoList = selectStageResultService.queryStageResultDataDetailByStageId(stageId);
        return Result.success(stageResultVoList);
    }


    @TakeCount(method = "常驻商店性价比")
    @ApiOperation("获取常驻商店性价比")
    @GetMapping("/store/perm")
    public Result<Map<String, List<StorePerm>>> getStorePermData() {
        Map<String, List<StorePerm>> storePerm = storeService.getStorePerm();
        return Result.success(storePerm);
    }

    @TakeCount(method = "活动商店性价比")
    @ApiOperation("获取活动商店性价比")
    @GetMapping("/store/act")
    public Result<List<StoreActVo>> getStoreActData() {
        List<StoreActVo> storeActVoList = storeService.getStoreAct();
        return Result.success(storeActVoList);
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
