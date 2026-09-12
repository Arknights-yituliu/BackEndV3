package com.lhs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.user.RiicSchedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 基建排班表 Mapper
 */
@Mapper
public interface RiicScheduleMapper extends BaseMapper<RiicSchedule> {
}
