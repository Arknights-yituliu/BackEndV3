package com.lhs.discard;


import com.alibaba.fastjson.JSONObject;
import com.lhs.common.util.Result;
import com.lhs.service.RecruitSurveyService;
import com.lhs.service.ScheduleService;
import com.lhs.service.dto.MaaRecruitVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

@RestController
@Api(tags = "MAA接口")
@RequestMapping(value = "/tool")
@CrossOrigin(maxAge = 86400)
public class OldController {

    @Resource
    private ScheduleService scheduleService;

    @Resource
    private RecruitSurveyService recruitSurveyService;

    @ApiOperation("MAA公招记录上传")
    @PostMapping("/recruitUpload")
    public Result MaaTagResult(@RequestBody MaaRecruitVo maaTagRequestVo) {

//        MaaRecruitData maaRecruitData = new MaaRecruitData(maaTagRequestVo.getUuid(),JSON.toJSONString(maaTagRequestVo.getTags()),
//                maaTagRequestVo.getLevel(),new Date(),maaTagRequestVo.getServer(), maaTagRequestVo.getSource()
//                , maaTagRequestVo.getVersion());
//        maaRecruitData.init();
//        maaRecruitData.setTag(JSON.toJSONString(maaTagRequestVo.getTags()));
//
//        String string = recruitSurveyService.saveMaaRecruitData(maaRecruitData);


        return Result.success();
    }


    @ApiOperation("各类公招统计结果计算")
    @GetMapping("/recruit/cal")
    public Result saveMaaRecruitStatistical() {
//        recruitSurveyService.maaRecruitDataCalculation();
        return Result.success();
    }

    @ApiOperation("各类公招统计结果")
    @GetMapping("/recruit/statistical")
    public Result queryMaaRecruitStatistical() {
        HashMap<String, Object> result = recruitSurveyService.statisticalResult();

        return Result.success(result);
    }

    @ApiOperation("生成基建排班协议文件")
    @PostMapping("/building/schedule/save")
    public Result saveMaaScheduleJson( @RequestBody String scheduleJson,@RequestParam Long schedule_id) {

        schedule_id = new Date().getTime() * 1000 +new Random().nextInt(1000);   //id为时间戳后加0001至999

        scheduleService.saveScheduleJson(scheduleJson,schedule_id);
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
        scheduleService.exportScheduleFile(response, schedule_id);
    }

    @ApiOperation("找回基建排班协议文件")
    @GetMapping("/building/schedule/retrieve")
    public Result retrieveMaaScheduleJson(@RequestParam Long schedule_id) {
        String str = scheduleService.retrieveScheduleJson(schedule_id);
        JSONObject jsonObject = JSONObject.parseObject(str);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("schedule",jsonObject);
        hashMap.put("message","导入成功");
        return Result.success(hashMap);
    }

    @ApiOperation(value = "保存bot图片")
    @PostMapping("/bot/image")
    public Result<Object> upload(MultipartFile file, @RequestParam String fileName, @RequestParam String fileType) throws IOException {
        System.out.println(file.getSize());
        System.out.println(fileName);
        String saveUri = "E:\\BOT_img\\autoImage\\111.png";

        if("char".equals(fileType))   saveUri  = "E:\\BOT_img\\bot-pic\\char\\" + fileName + ".png";        //拼接保存图片的真实地址
        if("mod".equals(fileType))  saveUri = "E:\\BOT_img\\bot-pic\\mod\\" + fileName + ".png";        //拼接保存图片的真实地址
        if("skill".equals(fileType))  saveUri = "E:\\BOT_img\\bot-pic\\skill\\" + fileName + ".png";        //拼接保存图片的真实地址

        File saveFile = new File(saveUri);
        //判断是否存在文件夹，不存在就创建，但其实可以直接手动确定创建好，这样不用每次保存都检测
//        if (!saveFile.exists()) { saveFile.mkdirs();}
        try {
            file.transferTo(saveFile);  //保存文件到真实存储路径下
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.success("保存成功" + fileName);
    }




}
