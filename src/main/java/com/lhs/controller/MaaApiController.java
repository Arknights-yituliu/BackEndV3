package com.lhs.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lhs.common.util.Result;
import com.lhs.entity.MaaRecruitData;
import com.lhs.service.request.MaaRecruitRequestVo;
import com.lhs.service.MaaService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

@RestController
@Api(tags = "MAA接口")
@RequestMapping(value = "/tool")
@CrossOrigin(maxAge = 86400)
public class MaaApiController {

    @Autowired
    private MaaService maaService;

    @ApiOperation("MAA公招记录上传")
    @PostMapping("/recruitUpload")
    public Result MaaTagResult(@RequestBody MaaRecruitRequestVo maaTagRequestVo) {

        MaaRecruitData maaRecruitData = new MaaRecruitData(maaTagRequestVo.getUuid(),JSON.toJSONString(maaTagRequestVo.getTags()),
                maaTagRequestVo.getLevel(),new Date(),maaTagRequestVo.getServer(), maaTagRequestVo.getSource()
                , maaTagRequestVo.getVersion());
        maaRecruitData.init();
        maaRecruitData.setTag(JSON.toJSONString(maaTagRequestVo.getTags()));

        String string = maaService.saveMaaRecruitData(maaRecruitData);
        return Result.success(string);
    }

    @ApiOperation("各类公招统计结果计算")
    @GetMapping("/recruit/cal")
    public Result saveMaaRecruitStatistical() {
         maaService.maaRecruitDataCalculation();
        return Result.success();
    }

    @ApiOperation("各类公招统计结果")
    @GetMapping("/recruit/statistical")
    public Result queryMaaRecruitStatistical() {
        String result = maaService.maaRecruitStatistical();
        JSONObject jsonObject = JSONObject.parseObject(result);
        return Result.success(jsonObject);
    }

    @ApiOperation("生成基建排班协议文件")
    @PostMapping("/building/schedule/save")
    public Result saveMaaScheduleJson( @RequestBody String scheduleJson,@RequestParam Long schedule_id) {

        schedule_id = new Date().getTime() * 1000 +new Random().nextInt(1000);   //id为时间戳后加0001至999

        maaService.saveScheduleJson(scheduleJson,schedule_id);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("uid",schedule_id);
        hashMap.put("message","生成成功");
        return Result.success(hashMap);
    }
//
//
    @ApiOperation("导出基建排班协议文件")
    @GetMapping("/building/schedule/export")
    public void exportMaaScheduleJson(HttpServletResponse response,@RequestParam Long schedule_id) {
         maaService.exportScheduleFile(response, schedule_id);
    }

    @ApiOperation("找回基建排班协议文件")
    @GetMapping("/building/schedule/retrieve")
    public Result retrieveMaaScheduleJson(@RequestParam Long schedule_id) {
        String str = maaService.exportScheduleJson(schedule_id);
        JSONObject jsonObject = JSONObject.parseObject(str);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("schedule",jsonObject);
        hashMap.put("message","导入成功");
        return Result.success(hashMap);
    }






}
