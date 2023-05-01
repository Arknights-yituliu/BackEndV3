package com.lhs.mapper;


import com.lhs.entity.RecruitData;
import com.lhs.entity.RecruitStatistics;
import com.lhs.entity.RecruitStatisticsConfig;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;


public interface RecruitDataMapper {

    Integer insertRecruitData(@Param("tableName") String table, @Param("recruitData") RecruitData recruitData);

    List<RecruitData> selectRecruitDataByCreateTime(@Param("tableName") String table, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    Integer insertRecruitStatistics(@Param("statistics") RecruitStatistics statistics);

    Integer updateRecruitStatistics(@Param("statistics") RecruitStatistics statistics);

    RecruitStatistics selectRecruitStatisticsByItem(@Param("statisticalItem") String statisticalItem);

    List<RecruitStatistics> selectRecruitStatistics();

    RecruitStatisticsConfig selectConfigByKey(@Param("configKey") String configKey);

    Integer updateConfigByKey(@Param("configKey") String configKey, @Param("configValue") String configValue);

}
