package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.OperatorCarryRate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OperatorCarryRateMapper extends BaseMapper<OperatorCarryRate> {

    Integer insertBatch(@Param("list")List<OperatorCarryRate> operatorCarryRateList);
}
