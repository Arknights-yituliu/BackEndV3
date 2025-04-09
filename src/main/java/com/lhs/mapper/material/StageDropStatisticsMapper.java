package com.lhs.mapper.material;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.material.StageDropStatisticalTaskLog;
import com.lhs.entity.po.material.StageDropStatistics;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface StageDropStatisticsMapper extends BaseMapper<StageDropStatistics> {

    Integer insertTaskLog(@Param("log")StageDropStatisticalTaskLog stageDropStatisticalTaskLog);

    Integer updateTaskLog(@Param("log")StageDropStatisticalTaskLog stageDropStatisticalTaskLog);

    StageDropStatisticalTaskLog getTaskLog(@Param("timeGranularity") Integer timeGranularity,
                                           @Param("start") Date start, @Param("end") Date end);

    Integer insertBatch(@Param("list") List<StageDropStatistics> stageDropList);

    List<StageDropStatistics>  listByDate(@Param("timeGranularity") Integer timeGranularity,
                                                             @Param("start") Date start, @Param("end") Date end);


    List<StageDropStatistics> listByStageId(@Param("stageId") String stageId, @Param("timeGranularity") Integer timeGranularity,
                                                               @Param("start") Date start, @Param("end") Date end);
}
