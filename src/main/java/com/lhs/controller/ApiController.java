package com.lhs.controller;



import com.lhs.common.annotation.TakeCount;
import com.lhs.common.util.Result;
import com.lhs.entity.Item;
import com.lhs.entity.StageResult;
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
@CrossOrigin()
@Slf4j
public class ApiController {

    @Resource
    private APIService apiService;
    @Resource
    private ItemService itemService ;


    @TakeCount(method = "物品价值")
    @ApiOperation("获取物品价值")
    @GetMapping("/item/value")
    @ApiImplicitParams({@ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625", required = false)})
    public Result queryItemValue(@RequestParam Double expCoefficient) {
        List<Item> items = itemService.queryItemList(expCoefficient,1000);
        return Result.success(items);
    }

    @TakeCount(method = "蓝材料推荐关卡")
    @ApiOperation("获取蓝材料推荐关卡按效率倒序")
    @GetMapping("/stage/t3")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625", required = false)
    public Result queryStageResult_t3(@RequestParam Double expCoefficient) {

        List<List<StageResultVo>> stageResultVoList = apiService.queryStageResultData_t3(expCoefficient, 200);

        return Result.success(stageResultVoList);
    }

    @TakeCount(method = "绿材料推荐关卡")
    @ApiOperation("获取绿材料推荐关卡按期望正序")
    @GetMapping("/stage/t2")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625", required = false)
    public Result queryStageResult_t2(@RequestParam Double expCoefficient) {
        List<List<StageResultVo>> stageResultVoList = apiService.queryStageResultData_t2(expCoefficient, 200);

        return Result.success(stageResultVoList);
    }

    @TakeCount(method = "历史活动关卡")
    @ApiOperation("获取历史活动关卡")
    @GetMapping("/stage/closed")
    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625", required = false)
    public Result queryStageResult_closedActivities(@RequestParam Double expCoefficient) {
        List<List<StageResultActVo>> stageResultVoList = apiService.queryStageResultData_closedActivities(expCoefficient, 200);

        return Result.success(stageResultVoList);
    }

    @TakeCount(method = "搓玉推荐关卡")
    @ApiOperation("获取搓玉推荐关卡")
    @GetMapping("/stage/orundum")
//    @ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625", required = false)
    public Result queryStageResult_Orundum() {
        List<OrundumPerApResultVo> OrundumPerApResultVoList = apiService.queryStageResultData_Orundum(0.625, 100);

        return Result.success(OrundumPerApResultVoList);
    }

    @TakeCount(method = "新章关卡置信度情况")
    @ApiOperation("新章关卡置信度情况")
    @GetMapping("/stage/newChapter")
    @ApiImplicitParam(name = "zone", value = "章节名称", dataType = "String", paramType = "query", defaultValue = "11-", required = false)
    public Result queryStageResult_newChapter(@RequestParam String zone) {
        List<StageResultVo> stageResultVoList = apiService.queryStageResultDataByZoneName(zone);

        return Result.success(stageResultVoList);
    }

    @TakeCount(method = "查询关卡详细信息")
    @ApiOperation("查询关卡详细信息")
    @GetMapping("/stage/detail")
    @ApiImplicitParam(name = "stageCode", value = "章节名称", dataType = "String", paramType = "query", defaultValue = "11-", required = false)
    public Result queryStageResult_detail(@RequestParam String stageCode) {
        List<StageResult> stageResultVoList = apiService.queryStageResultDataDetailByStageCode(stageCode);
        return Result.success(stageResultVoList);
    }


    @TakeCount(method = "常驻商店性价比")
    @ApiOperation("获取常驻商店性价比")
    @GetMapping("/store/perm")
    public Result getStorePermData() {
        Object resultVo = apiService.queryResultByApiPath("store/perm");
        return Result.success(resultVo);
    }

    @TakeCount(method = "活动商店性价比")
    @ApiOperation("获取活动商店性价比")
    @GetMapping("/store/act")
    public Result getStoreActData() {
        Object resultVo = apiService.queryResultByApiPath("store/act");
        return Result.success(resultVo);
    }

//    @TakeCount(method = "礼包商店性价比")
//    @ApiOperation("获取礼包商店性价比")
//    @GetMapping("/store/pack")
//    public Result getStorePackData() {
//        Object resultVo = apiService.queryResultByApiPath("store/pack");
//        return Result.success(resultVo);
//    }

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
