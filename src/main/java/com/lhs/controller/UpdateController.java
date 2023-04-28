package com.lhs.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lhs.common.annotation.TakeCount;
import com.lhs.common.config.FileConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.Result;
import com.lhs.mapper.ItemMapper;
import com.lhs.entity.Item;
import com.lhs.service.*;
import com.lhs.service.vo.StageResultActVo;
import com.lhs.service.vo.StageResultVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@Api(tags = "更新数据和文件导入导出")
@RequestMapping(value = "/")
@CrossOrigin()
@Slf4j
public class UpdateController {


    @Resource
    private StoreService storeService;
    @Resource
    private StageService stageService;
    @Resource
    private ToolService toolService;
    @Resource
    private APIService apiService;
    @Resource
    private ItemService itemService;
    @Resource
    private ItemMapper itemMapper;
    @Resource
    private StageResultService stageResultService;



    @ApiOperation(value = "更新礼包商店性价比")
    @PostMapping("store/import/pack")
    public Result updateStorePack(@RequestBody String packStr) {
        storeService.updateStorePackByJson(packStr);
        return Result.success("成功更新");
    }

    @ApiOperation(value = "关卡信息导出")
    @GetMapping("stage/import")
    public void exportStageData(HttpServletResponse response) {
        stageService.exportStageData(response);
    }

    @GetMapping("tool/visits")
    public Result updateVisits(@RequestParam String path) {
        toolService.updateVisits(path);
        return Result.success();
    }

    @TakeCount(method = "更新关卡推荐数据")
    @ApiOperation("更新关卡推荐数据")
    @GetMapping("stage/update")
    @ApiImplicitParams({@ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625", required = false),
            @ApiImplicitParam(name = "sampleSize", value = "样本大小", dataType = "Integer", paramType = "query", defaultValue = "300", required = false)})
    public Result updateStageData(@RequestParam Double expCoefficient,@RequestParam Integer sampleSize) {
        List<Item> items = itemMapper.selectList(null);   //找出该经验书价值系数版本的材料价值表Vn
        JSONObject itemNameAndBestStageEffJson = JSONObject.parseObject(FileUtil.read(FileConfig.Item + "itemAndBestStageEff.json")); //读取上次关卡效率计算的结果中蓝材料对应的常驻最高关卡效率En
        items = itemService.ItemValueCalculation(items, itemNameAndBestStageEffJson,expCoefficient);  //用上面蓝材料对应的常驻最高关卡效率En计算新的新材料价值表Vn+1
        stageResultService.stageResultCal(items,expCoefficient,sampleSize);      //用新材料价值表Vn+1再次计算新关卡效率En+1
        return Result.success();
    }

    @TakeCount(method = "保存关卡推荐结果")
    @ApiOperation("保存关卡推荐结果")
    @GetMapping("stage/save")
    @ApiImplicitParams({@ApiImplicitParam(name = "expCoefficient", value = "经验书的价值系数", dataType = "Double", paramType = "query", defaultValue = "0.625", required = false),
            @ApiImplicitParam(name = "sampleSize", value = "样本大小", dataType = "Integer", paramType = "query", defaultValue = "200", required = false)})
    public Result saveStageResults(@RequestParam Double expCoefficient,@RequestParam Integer sampleSize) {
        String saveDate = new SimpleDateFormat("yyyy-MM-dd HH-mm").format(new Date());
        List<List<StageResultVo>> stageResultVoList_t3 = apiService.queryStageResultData_t3(expCoefficient, sampleSize);
        FileUtil.save(FileConfig.Backup,"stageResult "+saveDate +" "+expCoefficient+" t3.json", JSON.toJSONString(stageResultVoList_t3));
        List<List<StageResultActVo>> stageResultVoList_closed = apiService.queryStageResultData_closedActivities(expCoefficient, sampleSize);
        FileUtil.save(FileConfig.Backup,"stageResult "+saveDate +" "+expCoefficient+" closed.json",JSON.toJSONString(stageResultVoList_closed));
        return Result.success();
    }

    @TakeCount(method = "保存企鹅物流数据")
    @ApiOperation("保存企鹅物流数据")
    @GetMapping("save/penguinData")
    @ApiImplicitParams({@ApiImplicitParam(name = "dataType", value = "自动或全局数据", dataType = "String", paramType = "query", defaultValue = "global", required = false),
            @ApiImplicitParam(name = "url", value = "企鹅物流API", dataType = "String", paramType = "query", defaultValue = "https://penguin-stats.io/PenguinStats/api/v2/result/matrix?show_closed_zones=true", required = false)})
    public Result savePenguinData(@RequestParam String dataType,@RequestParam String url) {
        apiService.savePenguinData(dataType,url);
        return Result.success();
    }

    @TakeCount(method = "更新常驻商店")
    @ApiOperation("更新常驻商店")
    @GetMapping("update/store/perm")
    public Result updateStorePerm() {
        storeService.updateStorePerm();
        return Result.success();
    }

}
