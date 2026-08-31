package com.lhs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.user.UserSchedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户排班表 Mapper
 */
@Mapper
public interface UserScheduleMapper extends BaseMapper<UserSchedule> {
}
