package com.lhs.controller;



import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.annotation.TakeCount;
import com.lhs.common.util.Result;
import com.lhs.entity.Item;
import com.lhs.service.*;

import com.lhs.service.resultVo.OrundumPerApResultVo;
import com.lhs.service.resultVo.StageResultActVo;
import com.lhs.service.resultVo.StageResultVo;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@Api(tags = "获取数据API")
@RequestMapping(value = "/api")
@CrossOrigin()
@Slf4j
public class ApiController {

    @Resource
    private APIService apiService;
    @Resource
    private ItemService itemService ;

    @Resource
    private StageResultService stageResultService;

    @TakeCount(method = "物品价值")
    @RedisCacheable(key = "item/value/#expCoefficient")
    @ApiOperation("获取物品价值")
    @GetMapping("/find/item/value")
    @ApiImplicitParams({@ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625", required = false)})
    public Result queryItemValue(@RequestParam Double expCoefficient) {
        List<Item> items = itemService.queryItemList(expCoefficient);
        return Result.success(items);
    }

    @TakeCount(method = "蓝材料推荐关卡")
    @RedisCacheable(key = "stage/t3/#expCoefficient")
    @ApiOperation("获取蓝材料推荐关卡按效率倒序")
    @GetMapping("/find/stage/t3")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625", required = false)
    public Result queryStageResult_t3(@RequestParam Double expCoefficient) {

        List<List<StageResultVo>> stageResultVoList = apiService.queryStageResultData_t3(expCoefficient, 200);
        return Result.success(stageResultVoList);
    }

    @TakeCount(method = "绿材料推荐关卡")
    @RedisCacheable(key = "stage/t2/#expCoefficient")
    @ApiOperation("获取绿材料推荐关卡按期望正序")
    @GetMapping("/find/stage/t2")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625", required = false)
    public Result queryStageResult_t2(@RequestParam Double expCoefficient) {
        List<List<StageResultVo>> stageResultVoList = apiService.queryStageResultData_t2(expCoefficient, 200);
        return Result.success(stageResultVoList);
    }

    @TakeCount(method = "历史活动关卡")
    @RedisCacheable(key = "stage/closed/#expCoefficient")
    @ApiOperation("获取历史活动关卡")
    @GetMapping("/find/stage/closed")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625", required = false)
    public Result queryStageResult_closedActivities(@RequestParam Double expCoefficient) {
        List<List<StageResultActVo>> stageResultVoMap = apiService.queryStageResultData_closedActivities(expCoefficient, 200);
        return Result.success(stageResultVoMap);
    }

    @TakeCount(method = "搓玉推荐关卡")
    @RedisCacheable(key = "stage/orundum")
    @ApiOperation("获取搓玉推荐关卡")
    @GetMapping("/find/stage/orundum")
//    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625", required = false)
    public Result queryStageResult_Orundum() {
        List<OrundumPerApResultVo> OrundumPerApResultVoList = apiService.queryStageResultData_Orundum(0.625, 100);
        return Result.success(OrundumPerApResultVoList);
    }

    @TakeCount(method = "新章关卡置信度情况")
    @RedisCacheable(key = "stage/newChapter")
    @ApiOperation("新章关卡置信度情况")
    @GetMapping("/find/stage/newChapter")
    @ApiImplicitParam(name = "zone", value = "章节名称", dataType = "String", paramType = "query", defaultValue = "11-", required = false)
    public Result queryStageResult_newChapter(@RequestParam String zone) {
        List<StageResultVo> stageResultVoList = apiService.queryStageResultData_newChapter(zone);
        return Result.success(stageResultVoList);
    }



    @TakeCount(method = "常驻商店性价比")
    @ApiOperation("获取常驻商店性价比")
    @GetMapping("/find/store/perm")
    public Result getStorePermData() {
        Object resultVo = apiService.queryResultByApiPath("store/perm");
        return Result.success(resultVo);
    }

    @TakeCount(method = "活动商店性价比")
    @ApiOperation("获取活动商店性价比")
    @GetMapping("/find/store/act")
    public Result getStoreActData() {
        Object resultVo = apiService.queryResultByApiPath("store/act");
        return Result.success(resultVo);
    }

    @TakeCount(method = "礼包商店性价比")
    @ApiOperation("获取礼包商店性价比")
    @GetMapping("/find/store/pack")
    public Result getStorePackData() {
        Object resultVo = apiService.queryResultByApiPath("store/pack");
        return Result.success(resultVo);
    }

    @ApiOperation(value = "材料表导出")
    @GetMapping("/item/export/excel")
    public void exportItemExcel(HttpServletResponse response) {
        itemService.exportItemExcel(response);
    }
    @ApiOperation(value = "材料表导出")
    @GetMapping("/item/export/json")
    public void exportItemJson(HttpServletResponse response) {
        itemService.exportItemJson(response);
    }


}
