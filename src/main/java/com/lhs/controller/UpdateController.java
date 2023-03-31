package com.lhs.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.annotation.TakeCount;
import com.lhs.common.config.FileConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.Result;
import com.lhs.mapper.ItemMapper;
import com.lhs.mapper.VisitsMapper;
import com.lhs.entity.Item;
import com.lhs.entity.Visits;
import com.lhs.service.*;
import com.lhs.service.resultVo.StageResultActVo;
import com.lhs.service.resultVo.StageResultVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Api(tags = "更新数据和文件导入导出")
@RequestMapping(value = "/")
@CrossOrigin()
@Slf4j
public class UpdateController {

    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private StageResultService stageResultService;
    @Autowired
    private StoreService storeService;
    @Autowired
    private StageService stageService;
    @Autowired
    private VisitsMapper visitsMapper;
    @Autowired
    private ToolService toolService;
    @Autowired
    private APIService apiService;


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




}
