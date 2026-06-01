package com.lhs.mapper.material;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.material.StageDropStatisticsTaskLog;
import com.lhs.entity.po.material.StageDropHourStatistics;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface StageDropHourStatisticsMapper extends BaseMapper<StageDropHourStatistics> {

    void insertTaskLog(@Param("log") StageDropStatisticsTaskLog stageDropStatisticsTaskLog);

    void updateTaskLog(@Param("log") StageDropStatisticsTaskLog stageDropStatisticsTaskLog);

    StageDropStatisticsTaskLog getTaskLogByTimeGranularityAndDate(@Param("timeGranularity") Integer timeGranularity,
                                                                  @Param("start") Date start, @Param("end") Date end);

    List<StageDropStatisticsTaskLog> listTaskLogByTimeGranularityAndDate(@Param("timeGranularity") Integer timeGranularity,
                                                                          @Param("start") Date start, @Param("end") Date end);

    void deleteTaskLogById(@Param("id") Long id);

    Integer insertBatch(@Param("list") List<StageDropHourStatistics> stageDropList);

    List<StageDropHourStatistics> selectListByDate(@Param("timeGranularity") Integer timeGranularity,
                                                   @Param("start") Date start, @Param("end") Date end);


    Integer expireHourStatisticsByRecordId(@Param("recordId") Long recordId, @Param("recordCode") Integer recordCode);
    

}
