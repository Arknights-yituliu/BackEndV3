package com.lhs.mapper;

import com.lhs.entity.survey.SurveyScore;
import com.lhs.entity.survey.SurveyStatisticsScore;
import com.lhs.vo.survey.SurveyScoreVo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyScoreMapper {


    //批量插入干员练度信息表
    Integer insertBatchSurveyScore(@Param("tableName") String tableName,
                                   @Param("scoreList") List<SurveyScore> scoreList);

    //根据id更新干员练度信息表
    Integer updateSurveyScoreById(@Param("tableName") String tableName,
                                  @Param("score") SurveyScore score);

    // 根据多个uid查询干员练度信息表
    List<SurveyScoreVo> selectSurveyScoreVoByUidList(@Param("tableName") String tableName,
                                                     @Param("ids") List<Long> ids);

    // 根据单个uid查询干员练度信息表Score
    List<SurveyScore> selectSurveyScoreByUid(@Param("tableName") String tableName,
                                             @Param("uid") Long uid);

    //批量插入统计表
    Integer insertBatchScoreStatistics(@Param("surveyStatisticsList")List<SurveyStatisticsScore> surveyStatisticsList);

    //清空统计表
    Integer truncateScoreStatisticsTable();

    //查询统计表所有结果
    List<SurveyStatisticsScore> selectScoreStatisticsList();

    String selectConfigByKey(@Param("configKey")String configKey);

    Integer updateConfigByKey(@Param("configValue")String configValue,@Param("configKey")String configKey);

}
