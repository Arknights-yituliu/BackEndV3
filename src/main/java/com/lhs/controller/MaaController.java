package com.lhs.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.FileConfig;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.Result;
import com.lhs.service.OperatorSurveyService;
import com.lhs.service.RecruitSurveyService;
import com.lhs.service.ScheduleService;
import com.lhs.service.dto.MaaOperBoxVo;
import com.lhs.service.dto.MaaRecruitVo;
import com.lhs.service.vo.OperatorStatisticsVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@Api(tags = "MAA接口-新")
@RequestMapping(value = "/maa")
@CrossOrigin(maxAge = 86400)
public class MaaController {
    @Resource
    private OperatorSurveyService operatorSurveyService;

    @Resource
    private ScheduleService scheduleService;

    @Resource
    private RecruitSurveyService recruitSurveyService;

    @ApiOperation("MAA公招记录上传")
    @PostMapping("/upload/recruit")
    public Result MaaTagResult(@RequestBody MaaRecruitVo maaTagRequestVo) {
        String string = recruitSurveyService.saveMaaRecruitDataNew(maaTagRequestVo);
        return Result.success(string);
    }

    @ApiOperation("MAA干员信息上传")
    @PostMapping("/upload/operBox")
    public Result MaaOperatorBoxUpload(HttpServletRequest httpServletRequest, @RequestBody MaaOperBoxVo maaOperBoxVo) {

        String ipAddress = IpUtil.getIpAddress(httpServletRequest);
        ipAddress = AES.encrypt(ipAddress, FileConfig.Secret);  //加密
        HashMap<String, Long> result = operatorSurveyService.saveMaaOperatorBoxData(maaOperBoxVo, ipAddress);

        return Result.success(result);
    }

    @ApiOperation("MAA干员信息上传")
    @PostMapping(value = "/upload/operBox/manual",produces = "application/json;charset=UTF-8")
    public Result MaaOperatorBoxUpload(HttpServletRequest httpServletRequest, @RequestBody JSONArray operBox) {

        String ipAddress = IpUtil.getIpAddress(httpServletRequest);
        ipAddress = AES.encrypt(ipAddress, FileConfig.Secret);  //加密
        MaaOperBoxVo maaOperBoxVo = new MaaOperBoxVo();
        maaOperBoxVo.setServer("manual");
        maaOperBoxVo.setSource("manual");
        maaOperBoxVo.setVersion("manual_v1");
        maaOperBoxVo.setOperBox(operBox);
        HashMap<String, Long> result = operatorSurveyService.saveMaaOperatorBoxData(maaOperBoxVo, ipAddress);
//        System.out.println(result);
        return Result.success(result);
    }

    @ApiOperation("干员统计结果")
    @GetMapping("/operator/result")
    public Result MaaOperatorDataResult() {
        HashMap<String, Object> result = operatorSurveyService.operatorBoxResult();
        return Result.success(result);
    }



    @ApiOperation("公招统计")
    @GetMapping("/recruit/statistics")
    public Result saveMaaRecruitStatistical() {
        Map<String, Integer> result = recruitSurveyService.recruitStatistics();
        return Result.success(result);
    }


    @ApiOperation("公招统计结果")
    @GetMapping("/recruit/result")
    public Result queryMaaRecruitStatistical() {
        HashMap<String, Object> result = recruitSurveyService.statisticalResult();

        return Result.success(result);
    }

    @ApiOperation("生成基建排班协议文件")
    @PostMapping("/schedule/save")
    public Result saveMaaScheduleJson( @RequestBody String scheduleJson,@RequestParam Long schedule_id) {

        schedule_id = new Date().getTime() * 1000 +new Random().nextInt(1000);   //id为时间戳后加0001至999

        scheduleService.saveScheduleJson(scheduleJson,schedule_id);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("uid",schedule_id);
        hashMap.put("message","生成成功");
        return Result.success(hashMap);
    }


    @ApiOperation("导出基建排班协议文件")
    @GetMapping("/schedule/export")
    public void exportMaaScheduleJson(HttpServletResponse response, @RequestParam Long schedule_id) {
        scheduleService.exportScheduleFile(response, schedule_id);
    }

    @ApiOperation("找回基建排班协议文件")
    @GetMapping("/schedule/retrieve")
    public Result retrieveMaaScheduleJson(@RequestParam Long schedule_id) {
        String str = scheduleService.retrieveScheduleJson(schedule_id);
        JSONObject jsonObject = JSONObject.parseObject(str);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("schedule",jsonObject);
        hashMap.put("message","导入成功");
        return Result.success(hashMap);
    }

}
