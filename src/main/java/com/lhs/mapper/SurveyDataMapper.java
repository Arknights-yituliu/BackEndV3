package com.lhs.mapper;

import com.lhs.entity.SurveyDataChar;
import com.lhs.entity.SurveyStatisticsChar;
import com.lhs.entity.SurveyUser;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyDataMapper {

    Integer insertSurveyUser(@Param("user") SurveyUser user);

    SurveyUser selectSurveyUserByUserName(@Param("userName") String userName);

    Integer updateSurveyUser(@Param("user") SurveyUser user);

    Integer insertBatchSurveyDataChar(@Param("tableName") String tableName,
                                      @Param("surveyDataList") List<SurveyDataChar> surveyDataCharList);

    SurveyDataChar selectSurveyDataCharById(@Param("tableName") String tableName,
                                             @Param("id") String id);

    Integer updateSurveyDataCharById(@Param("tableName") String tableName,
                                     @Param("surveyData") SurveyDataChar surveyData);

    List<SurveyDataChar> selectSurveyDataCharByUid(@Param("tableName") String tableName,
                                                   @Param("ids") List<Long> ids);

    List<Long> selectSurveyUserIds();

    Integer insertBatchCharStatisticsChar(@Param("surveyStatisticsList")List<SurveyStatisticsChar> surveyStatisticsList);

    Integer truncateCharStatisticsTable();

    List<SurveyStatisticsChar> selectCharStatisticsList();

    String selectConfigByKey(@Param("configKey")String configKey);

    Integer updateConfigByKey(@Param("configValue")String configValue,@Param("configKey")String configKey);
}
