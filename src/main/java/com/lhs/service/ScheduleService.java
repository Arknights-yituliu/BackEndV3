package com.lhs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.util.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.maa.Schedule;
import com.lhs.mapper.ScheduleMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Service
public class ScheduleService {

    @Resource
    private ScheduleMapper scheduleMapper;

    public void saveScheduleJson(String scheduleJson, Long scheduleId) {
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
//        System.out.println(FileConfig.Schedule);

        FileUtil.save(response, ConfigUtil.Schedule, scheduleId.toString()+".json", jsonForMat);
    }


    public String retrieveScheduleJson(Long scheduleId) {
        Schedule schedule = scheduleMapper.selectOne(new QueryWrapper<Schedule>().eq("schedule_id",scheduleId));

        if(schedule==null){
            String read = FileUtil.read(ConfigUtil.Schedule + scheduleId + ".json");
            if (read == null) throw new ServiceException(ResultCode.DATA_NONE);
            return read;
        }

        return schedule.getSchedule();
    }
}
