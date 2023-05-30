package com.lhs.mapper;

import com.lhs.entity.survey.SurveyEvaluation;
import com.lhs.entity.survey.SurveyStatisticsEvaluation;
import com.lhs.service.vo.SurveyEvaluationVo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyEvaluationMapper {

    //批量插入干员练度信息表
    Integer insertBatchSurveyEvaluation(@Param("tableName") String tableName,
                                       @Param("evaluationList") List<SurveyEvaluation> evaluationList);

    //根据id更新干员练度信息表
    Integer updateSurveyEvaluationById(@Param("tableName") String tableName,
                                      @Param("evaluation") SurveyEvaluation evaluation);

    // 根据多个uid查询干员练度信息表
    List<SurveyEvaluationVo> selectSurveyEvaluationVoByUidList(@Param("tableName") String tableName,
                                                             @Param("ids") List<Long> ids);

    // 根据单个uid查询干员练度信息表
    List<SurveyEvaluation> selectSurveyEvaluationByUid(@Param("tableName") String tableName,
                                                     @Param("uid") Long uid);

    //批量插入统计表
    Integer insertBatchEvaluationStatistics(@Param("surveyStatisticsList")List<SurveyStatisticsEvaluation> surveyStatisticsList);

    //清空统计表
    Integer truncateEvaluationStatisticsTable();

    //查询统计表所有结果
    List<SurveyStatisticsEvaluation> selectEvaluationStatisticsList();

    String selectConfigByKey(@Param("configKey")String configKey);

    Integer updateConfigByKey(@Param("configValue")String configValue,@Param("configKey")String configKey);

}
