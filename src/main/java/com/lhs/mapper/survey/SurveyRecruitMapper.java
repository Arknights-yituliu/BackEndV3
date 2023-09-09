package com.lhs.mapper.survey;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.maa.SurveyRecruit;
import com.lhs.entity.maa.RecruitStatistics;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface SurveyRecruitMapper  {

    Integer insertRecruitData(@Param("tableName") String table, @Param("surveyRecruit") SurveyRecruit surveyRecruit);

    List<SurveyRecruit> selectRecruitDataByCreateTime(@Param("tableName") String table, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    Integer insertRecruitStatistics(@Param("statistics") RecruitStatistics statistics);

    Integer updateRecruitStatistics(@Param("statistics") RecruitStatistics statistics);

    RecruitStatistics selectRecruitStatisticsByItem(@Param("statisticalItem") String statisticalItem);

    List<RecruitStatistics> selectRecruitStatistics();


}
