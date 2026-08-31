package com.lhs.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.entity.po.user.UserSchedule;
import com.lhs.mapper.UserScheduleMapper;
import com.lhs.service.user.UserScheduleService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 用户排班表服务实现
 */
@Service
public class UserScheduleServiceImpl implements UserScheduleService {

    /** 每个用户最多保存的排班表数量 */
    private static final int MAX_SCHEDULE_COUNT = 10;

    private final UserScheduleMapper userScheduleMapper;

    public UserScheduleServiceImpl(UserScheduleMapper userScheduleMapper) {
        this.userScheduleMapper = userScheduleMapper;
    }

    @Override
    public Long saveSchedule(Long uid, Long id, String scheduleJson) {
        // 排班内容不能为空
        if (scheduleJson == null || scheduleJson.isEmpty()) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }

        // 传了 id：覆盖更新，需校验记录存在且属于当前用户
        if (id != null) {
            UserSchedule exist = userScheduleMapper.selectById(id);
            if (exist == null || !exist.getUid().equals(uid)) {
                throw new ServiceException(ResultCode.USER_SCHEDULE_NOT_FOUND);
            }
            exist.setSchedule(scheduleJson);
            exist.setUpdateTime(new Date());
            userScheduleMapper.updateById(exist);
            return id;
        }

        // 未传 id：新增，校验每用户数量上限
        Long count = userScheduleMapper.selectCount(
                new LambdaQueryWrapper<UserSchedule>().eq(UserSchedule::getUid, uid));
        if (count != null && count >= MAX_SCHEDULE_COUNT) {
            throw new ServiceException(ResultCode.USER_SCHEDULE_LIMIT_EXCEEDED);
        }

        UserSchedule schedule = new UserSchedule();
        schedule.setUid(uid);
        schedule.setSchedule(scheduleJson);
        schedule.setCreateTime(new Date());
        schedule.setUpdateTime(new Date());
        userScheduleMapper.insert(schedule);
        return schedule.getId();
    }

    @Override
    public List<UserSchedule> listByUid(Long uid) {
        return userScheduleMapper.selectList(new LambdaQueryWrapper<UserSchedule>()
                .eq(UserSchedule::getUid, uid)
                .orderByDesc(UserSchedule::getUpdateTime));
    }

    @Override
    public UserSchedule getSchedule(Long id) {
        UserSchedule schedule = userScheduleMapper.selectById(id);
        if (schedule == null) {
            throw new ServiceException(ResultCode.USER_SCHEDULE_NOT_FOUND);
        }
        return schedule;
    }
}
