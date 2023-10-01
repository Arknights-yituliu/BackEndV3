package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.SurveyStatisticsOperator;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyStatisticsOperatorMapper extends BaseMapper<SurveyStatisticsOperator> {

    void truncate();

    void insertBatch(@Param("list")List<SurveyStatisticsOperator> statisticsOperatorList);
}
