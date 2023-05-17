package com.lhs.mapper;

import com.lhs.entity.survey.SurveyDataChar;
import com.lhs.entity.survey.SurveyDataCharVo;
import com.lhs.entity.survey.SurveyStatisticsChar;
import com.lhs.entity.survey.SurveyUser;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyMapper {

    //新增用户
    Integer insertSurveyUser(@Param("user") SurveyUser user);

    //查询用户根据用户名
    SurveyUser selectSurveyUserByUserName(@Param("userName") String userName);

    //查询用户根据id
    SurveyUser selectSurveyUserById(@Param("id") Long id);

    //查询用户根据ip
    SurveyUser  selectLastSurveyUserIp(@Param("id") String id);

    //更新用户
    Integer updateSurveyUser(@Param("user") SurveyUser user);

    //用户表所有uid
    List<Long> selectSurveyUserIds();

    //干员练度信息表，批量插入
    Integer insertBatchSurveyDataChar(@Param("tableName") String tableName,
                                      @Param("surveyDataList") List<SurveyDataChar> surveyDataCharList);

    //干员练度信息表，根据id查询用户某条干员的数据
    SurveyDataChar selectSurveyDataCharById(@Param("tableName") String tableName,
                                             @Param("id") String id);
     //干员练度信息表，根据id更新
    Integer updateSurveyDataCharById(@Param("tableName") String tableName,
                                     @Param("surveyData") SurveyDataChar surveyData);
    //
    // 干员练度信息表，根据uid查询用户所有数据
    List<SurveyDataCharVo> selectSurveyDataCharVoByUidList(@Param("tableName") String tableName,
                                                         @Param("ids") List<Long> ids);

    List<SurveyDataCharVo> selectSurveyDataCharVoByUid(@Param("tableName") String tableName,
                                                       @Param("uid") Long uid);

    List<SurveyDataChar> selectSurveyDataCharByUid(@Param("tableName") String tableName,
                                                     @Param("uid") Long uid);

    Integer deleteSurveyDataCharById(@Param("tableName") String tableName,
                                     @Param("ids") List<String> ids);

    Long selectLastSurveyUserId();

    //统计表批量插入
    Integer insertBatchCharStatistics(@Param("surveyStatisticsList")List<SurveyStatisticsChar> surveyStatisticsList);

    //清空统计表
    Integer truncateCharStatisticsTable();

    //统计表结果查询
    List<SurveyStatisticsChar> selectCharStatisticsList();

    String selectConfigByKey(@Param("configKey")String configKey);

    Integer updateConfigByKey(@Param("configValue")String configValue,@Param("configKey")String configKey);
}
