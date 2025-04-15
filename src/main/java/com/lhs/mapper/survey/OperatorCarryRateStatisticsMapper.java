package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.OperatorCarryRateStatistics;

import com.lhs.entity.tmp.QuestionnaireResultDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OperatorCarryRateStatisticsMapper extends BaseMapper<OperatorCarryRateStatistics> {

    List<QuestionnaireResultDTO> getOldData();

    Integer insertBatch(@Param("list") List<OperatorCarryRateStatistics> list);


}
