package com.lhs.service.maa;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileUtil;

import com.lhs.common.enums.ResultCode;
import com.lhs.entity.po.maa.Schedule;
import com.lhs.mapper.ScheduleMapper;
import com.lhs.service.util.OSSService;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Date;

@Service
public class ScheduleService {


    private final ScheduleMapper scheduleMapper;
    private final OSSService ossService;

    public ScheduleService(ScheduleMapper scheduleMapper, OSSService ossService) {
        this.scheduleMapper = scheduleMapper;
        this.ossService = ossService;
    }


    public void saveScheduleJson(String scheduleJson, Long scheduleId) {
//        ossService.upload(scheduleJson,"schedule/"+scheduleId+".json");
        Schedule schedule = new Schedule();
        schedule.setUid(scheduleId);
        schedule.setScheduleId(scheduleId);
        JSONObject jsonObject = JSONObject.parseObject(scheduleJson);
        jsonObject.put("id", scheduleId);
        schedule.setSchedule(JSON.toJSONString(jsonObject));
        schedule.setCreateTime(new Date());
        scheduleMapper.insert(schedule);
    }


    public void exportScheduleFile(HttpServletResponse response, Long scheduleId) {

        Schedule schedule = scheduleMapper.selectOne(new QueryWrapper<Schedule>().eq("schedule_id", scheduleId));

        String jsonForMat = JSON.toJSONString(JSONObject.parseObject(schedule.getSchedule()), SerializerFeature.PrettyFormat,
                SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullListAsEmpty);

        FileUtil.save(response, ConfigUtil.Schedule, scheduleId.toString()+".json", jsonForMat);
    }


    public String retrieveScheduleJson(Long scheduleId) {
        Schedule schedule = scheduleMapper
                .selectOne(new QueryWrapper<Schedule>().eq("schedule_id",scheduleId));


        if(schedule==null){
            File file = new File(ConfigUtil.Schedule + scheduleId + ".json");
            if(file.exists()){
                return FileUtil.read(ConfigUtil.Schedule + scheduleId + ".json");
            }
             throw new ServiceException(ResultCode.DATA_NONE);

        }

        return schedule.getSchedule();
    }
}
