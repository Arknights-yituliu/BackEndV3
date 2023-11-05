package com.lhs.mapper.survey;


import com.lhs.entity.po.maa.RecruitData;
import com.lhs.entity.po.maa.RecruitStatistics;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecruitDataMapper {

    void insertRecruitData(@Param("tableName") String table, @Param("surveyRecruit") RecruitData recruitData);

    List<RecruitData> selectRecruitDataByCreateTime(@Param("tableName") String table, @Param("startTime") Long startTime, @Param("endTime") Long endTime);

    void insertRecruitStatistics(@Param("statistics") RecruitStatistics statistics);

    void updateRecruitStatistics(@Param("statistics") RecruitStatistics statistics);

    RecruitStatistics selectRecruitStatisticsByItem(@Param("statisticalItem") String statisticalItem);

    List<RecruitStatistics> selectRecruitStatistics();


}
