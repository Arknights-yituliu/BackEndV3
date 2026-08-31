package com.lhs.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.entity.po.user.RiicSchedule;
import com.lhs.mapper.RiicScheduleMapper;
import com.lhs.service.user.RiicScheduleService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 基建排班表服务实现
 */
@Service
public class RiicScheduleServiceImpl implements RiicScheduleService {

    /** 每个用户最多保存的排班表数量 */
    private static final int MAX_SCHEDULE_COUNT = 10;

    private final RiicScheduleMapper riicScheduleMapper;

    public RiicScheduleServiceImpl(RiicScheduleMapper riicScheduleMapper) {
        this.riicScheduleMapper = riicScheduleMapper;
    }

    @Override
    public Long saveSchedule(Long uid, Long id, String scheduleJson) {
        // 排班内容不能为空
        if (scheduleJson == null || scheduleJson.isEmpty()) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }

        // 传了 id：覆盖更新，需校验记录存在且属于当前用户
        if (id != null) {
            RiicSchedule exist = riicScheduleMapper.selectById(id);
            if (exist == null || !exist.getUid().equals(uid)) {
                throw new ServiceException(ResultCode.USER_SCHEDULE_NOT_FOUND);
            }
            exist.setSchedule(scheduleJson);
            exist.setUpdateTime(new Date());
            riicScheduleMapper.updateById(exist);
            return id;
        }

        // 未传 id：新增，校验每用户数量上限
        Long count = riicScheduleMapper.selectCount(
                new LambdaQueryWrapper<RiicSchedule>().eq(RiicSchedule::getUid, uid));
        if (count != null && count >= MAX_SCHEDULE_COUNT) {
            throw new ServiceException(ResultCode.USER_SCHEDULE_LIMIT_EXCEEDED);
        }

        RiicSchedule schedule = new RiicSchedule();
        schedule.setUid(uid);
        schedule.setSchedule(scheduleJson);
        schedule.setCreateTime(new Date());
        schedule.setUpdateTime(new Date());
        riicScheduleMapper.insert(schedule);
        return schedule.getId();
    }

    @Override
    public List<RiicSchedule> listByUid(Long uid) {
        return riicScheduleMapper.selectList(new LambdaQueryWrapper<RiicSchedule>()
                .eq(RiicSchedule::getUid, uid)
                .orderByDesc(RiicSchedule::getUpdateTime));
    }

    @Override
    public RiicSchedule getSchedule(Long id) {
        RiicSchedule schedule = riicScheduleMapper.selectById(id);
        if (schedule == null) {
            throw new ServiceException(ResultCode.USER_SCHEDULE_NOT_FOUND);
        }
        return schedule;
    }
}
