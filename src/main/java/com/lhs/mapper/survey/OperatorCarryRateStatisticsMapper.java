package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.OperatorCarryRateStatistics;
import com.lhs.entity.vo.survey.OperatorCarryRateStatisticsVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OperatorCarryRateStatisticsMapper extends BaseMapper<OperatorCarryRateStatistics> {

    void expireOldData();

    Integer insertBatch(@Param("list") List<OperatorCarryRateStatistics> list);

    List<OperatorCarryRateStatisticsVO> getOperatorCarryRateResult();

}
