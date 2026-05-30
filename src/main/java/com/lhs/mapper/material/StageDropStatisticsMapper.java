package com.lhs.mapper.material;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.material.StageDropStatisticsTaskLog;
import com.lhs.entity.po.material.StageDropStatistics;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface StageDropStatisticsMapper extends BaseMapper<StageDropStatistics> {

    void insertTaskLog(@Param("log") StageDropStatisticsTaskLog stageDropStatisticsTaskLog);

    void updateTaskLog(@Param("log") StageDropStatisticsTaskLog stageDropStatisticsTaskLog);

    StageDropStatisticsTaskLog getTaskLogByTimeGranularityAndDate(@Param("timeGranularity") Integer timeGranularity,
                                                                  @Param("start") Date start, @Param("end") Date end);

    Integer insertBatch(@Param("list") List<StageDropStatistics> stageDropList);

    List<StageDropStatistics> selectListByDate(@Param("timeGranularity") Integer timeGranularity,
                                               @Param("start") Date start, @Param("end") Date end);


    List<StageDropStatistics> listByStageId(@Param("stageId") String stageId, @Param("timeGranularity") Integer timeGranularity,
                                                               @Param("start") Date start, @Param("end") Date end);

    Integer expireByTimeRange(@Param("recordCode") Integer recordCode, @Param("timeGranularity") Integer timeGranularity,
                              @Param("start") Date start, @Param("end") Date end);
}
