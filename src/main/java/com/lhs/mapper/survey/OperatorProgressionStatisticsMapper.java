package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.OperatorProgressionStatistics;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperatorProgressionStatisticsMapper extends BaseMapper<OperatorProgressionStatistics> {

    Integer expireOldData(@Param("targetRecordType") Integer targetRecordType,@Param("currentRecordType") Integer currentRecordType);
    Integer insertBatch(@Param("list")List<OperatorProgressionStatistics> statisticsOperatorList);
}
