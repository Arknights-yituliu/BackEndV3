package com.lhs.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.util.ConfigUtil;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.Result;
import com.lhs.service.*;
import com.lhs.service.dto.MaaOperBoxVo;
import com.lhs.service.dto.MaaRecruitVo;
import com.lhs.service.vo.StageResultVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.StringReader;
import java.util.*;

@RestController
@Api(tags = "MaaAPI—新")
@RequestMapping(value = "/maa")
@CrossOrigin(maxAge = 86400)
public class MaaController {


    @Resource
    private ScheduleService scheduleService;
    @Resource
    private SurveyService surveyService;
    @Resource
    private StageResultService stageResultService;
    @Resource
    private RecruitSurveyService recruitSurveyService;

    @ApiOperation("MAA公招记录上传")
    @PostMapping("/upload/recruit")
    public Result<Object> MaaTagResult(@RequestBody MaaRecruitVo maaTagRequestVo) {
        String string = recruitSurveyService.saveMaaRecruitDataNew(maaTagRequestVo);
        return Result.success(string);
    }

    @ApiOperation("MAA干员信息上传")
    @PostMapping("/upload/operBox")
    public Result<Object> MaaOperatorBoxUpload(HttpServletRequest httpServletRequest, @RequestBody MaaOperBoxVo maaOperBoxVo) {

        String ipAddress = IpUtil.getIpAddress(httpServletRequest);
        ipAddress = AES.encrypt(ipAddress, ConfigUtil.Secret);  //加密
        HashMap<Object, Object> result = surveyService.saveMaaCharData(maaOperBoxVo, ipAddress);

        return Result.success(result);
    }



    @ApiOperation("公招统计")
    @GetMapping("/recruit/statistics")
    public Result<Object> saveMaaRecruitStatistical() {
        Map<String, Integer> result = recruitSurveyService.recruitStatistics();
        return Result.success(result);
    }


    @ApiOperation("公招统计结果")
    @GetMapping("/recruit/result")
    public Result<Object> queryMaaRecruitStatistical() {
        HashMap<String, Object> result = recruitSurveyService.statisticalResult();

        return Result.success(result);
    }

    @ApiOperation("生成基建排班协议文件")
    @PostMapping("/schedule/save")
    public Result<Object> saveMaaScheduleJson( @RequestBody String scheduleJson,@RequestParam Long schedule_id) {
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
    public Result<Object> retrieveMaaScheduleJson(@RequestParam Long schedule_id) {
        String str = scheduleService.retrieveScheduleJson(schedule_id);
        JSONObject jsonObject = JSONObject.parseObject(str);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("schedule",jsonObject);
        hashMap.put("message","导入成功");
        return Result.success(hashMap);
    }

    @ApiOperation("查询某个材料的最优关卡(会返回理智转化效率在80%以上的至多8个关卡)")
    @GetMapping()
    @ApiImplicitParam(name = "itemName", value = "材料名称", dataType = "String", paramType = "query")
    public Result<List<StageResultVo>> selectStageResultByItemName(@RequestParam String itemName){
        List<StageResultVo> stageResultVoList =  stageResultService.selectStageResultByItemName(itemName);
        return Result.success(stageResultVoList);
    }
}
