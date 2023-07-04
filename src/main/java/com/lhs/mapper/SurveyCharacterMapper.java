package com.lhs.mapper;

import com.lhs.entity.survey.SurveyCharacter;
import com.lhs.vo.survey.SurveyCharacterVo;
import com.lhs.entity.survey.SurveyStatisticsCharacter;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyCharacterMapper {



    //批量插入干员练度信息表
    Integer insertBatchSurveyCharacter(@Param("tableName") String tableName,
                                       @Param("characterList") List<SurveyCharacter> characterList);

     //根据id更新干员练度信息表
    Integer updateSurveyCharacterById(@Param("tableName") String tableName,
                                      @Param("character") SurveyCharacter character);

    // 根据多个uid查询干员练度信息表
    List<SurveyCharacterVo> selectSurveyCharacterVoByUidList(@Param("tableName") String tableName,
                                                             @Param("ids") List<Long> ids);

    // 根据单个uid查询干员练度信息表
    List<SurveyCharacter> selectSurveyCharacterByUid(@Param("tableName") String tableName,
                                                     @Param("uid") Long uid);

    //批量插入统计表
    Integer insertBatchCharacterStatistics(@Param("surveyStatisticsList")List<SurveyStatisticsCharacter> surveyStatisticsList);

    //清空统计表
    Integer truncateCharacterStatisticsTable();

    //查询统计表所有结果
    List<SurveyStatisticsCharacter> selectCharacterStatisticsList();

}
