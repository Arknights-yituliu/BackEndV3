package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.OperatorStatistics;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperatorStatisticsMapper extends BaseMapper<OperatorStatistics> {

    void truncate();

    void insertBatch(@Param("list")List<OperatorStatistics> statisticsOperatorList);
}
