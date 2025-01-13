package com.lhs.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.LogUtils;
import com.lhs.common.util.Result;
import com.lhs.entity.dto.material.StageDropDTO;
import com.lhs.service.maa.StageDropUploadService;
import com.lhs.service.maa.RecruitTagUploadService;
import com.lhs.service.maa.ScheduleService;
import com.lhs.entity.vo.maa.MaaRecruitVo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.util.*;

@RestController
@Tag(name = "MaaAPI—新")
@RequestMapping(value = "/maa")
public class MaaController {

    private final ScheduleService scheduleService;

    private final RecruitTagUploadService recruitTagUploadService;

    private final StageDropUploadService stageDropUploadService;

    public MaaController(ScheduleService scheduleService,
                         RecruitTagUploadService recruitTagUploadService,
                         StageDropUploadService stageDropUploadService) {
        this.scheduleService = scheduleService;
        this.recruitTagUploadService = recruitTagUploadService;
        this.stageDropUploadService = stageDropUploadService;
    }

    @Operation(summary = "MAA公招记录上传")
    @PostMapping("/upload/recruit")
    public Result<Object> MaaTagResult(@RequestBody MaaRecruitVo maaTagRequestVo) {
//          return Result.success();
        return Result.success(recruitTagUploadService.saveMaaRecruitDataNew(maaTagRequestVo));
    }


    @Operation(summary = "公招统计")
    @GetMapping("/recruit/statistics")
    public Result<Object> saveMaaRecruitStatistical() {
        Map<String, Integer> result = recruitTagUploadService.recruitStatistics();
        return Result.success(result);
    }


    @Operation(summary = "公招统计结果")
    @GetMapping("/recruit/result")
    public Result<Object> queryMaaRecruitStatistical() {
        HashMap<String, Object> result = recruitTagUploadService.statisticalResult();

        return Result.success(result);
    }

    @Operation(summary = "MAA关卡掉落上传")
    @PostMapping("/upload/stageDrop")
    public Result<Object> stageDropUpload(HttpServletRequest httpServletRequest, @RequestBody StageDropDTO stageDropDTO) {
        ;
        return Result.success(stageDropUploadService.saveStageDrop(httpServletRequest, stageDropDTO));
    }

    @Operation(summary = "生成基建排班协议文件")
    @PostMapping("/schedule/save")
    public Result<Object> saveMaaScheduleJson(@RequestBody String scheduleJson, @RequestParam Long schedule_id) {
        schedule_id = new Date().getTime() * 1000 + new Random().nextInt(1000);   //id为时间戳后加0001至999
        scheduleService.saveScheduleJson(scheduleJson, schedule_id);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("scheduleId", schedule_id);
        hashMap.put("message", "生成成功");
        return Result.success(hashMap);
    }


    @Operation(summary = "导出基建排班协议文件")
    @GetMapping("/schedule/export")
    public void exportMaaScheduleJson(HttpServletResponse response, @RequestParam Long schedule_id) {
        LogUtils.info("导出的排班id是：" + schedule_id);
        scheduleService.exportScheduleFile(response, schedule_id);
    }

    @Operation(summary = "找回基建排班协议文件")
    @GetMapping("/schedule/retrieve")
    public Result<Object> retrieveMaaScheduleJson(@RequestParam Long schedule_id) {
        String str = scheduleService.retrieveScheduleJson(schedule_id);
        JsonNode jsonObject = JsonMapper.parseJSONObject(str);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("schedule", jsonObject);
        hashMap.put("message", "导入成功");
        return Result.success(hashMap);
    }


//    @Operation(summary ="查询某个材料的最优关卡(会返回理智转化效率在80%以上的至多8个关卡)")
//    @GetMapping("/stage")
//    @ApiImplicitParam(name = "itemName", value = "材料名称", dataType = "String", paramType = "query")
//    public Result<List<StageResultVo>> selectStageResultByItemName(@RequestParam String itemName){
//        List<StageResultVo> stageResultVoList =  stageResultService.selectStageResultByItemName(itemName);
//        return Result.success(stageResultVoList);
//    }
}
