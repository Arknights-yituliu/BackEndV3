package com.lhs.service.maa;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileUtil;

import com.lhs.common.enums.ResultCode;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.po.maa.Schedule;
import com.lhs.mapper.ScheduleMapper;
import org.springframework.stereotype.Service;


import java.io.File;
import java.util.Date;
import java.util.Map;

@Service
public class ScheduleService {


    private final ScheduleMapper scheduleMapper;


    public ScheduleService(ScheduleMapper scheduleMapper) {
        this.scheduleMapper = scheduleMapper;
    }


    public void saveScheduleJson(String scheduleJson, Long scheduleId) {
//        ossService.upload(scheduleJson,"schedule/"+scheduleId+".json");
        Schedule schedule = new Schedule();
        schedule.setUid(scheduleId);
        schedule.setScheduleId(scheduleId);
        Map<Object, Object> map = JsonMapper.parseObject(scheduleJson, new TypeReference<>() {

        });
        map.put("id", scheduleId);
        schedule.setSchedule(JsonMapper.toJSONString(map));
        schedule.setCreateTime(new Date());
        scheduleMapper.insert(schedule);
    }


    public String retrieveScheduleJson(Long scheduleId) {
        Schedule schedule = scheduleMapper
                .selectOne(new QueryWrapper<Schedule>().eq("schedule_id", scheduleId));

        if (schedule == null) {
            throw new ServiceException(ResultCode.DATA_NONE);
        }

        return schedule.getSchedule();
    }
}
