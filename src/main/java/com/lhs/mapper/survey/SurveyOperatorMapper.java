package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.SurveyOperator;
import com.lhs.entity.po.survey.SurveyStatisticsUser;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyOperatorMapper extends BaseMapper<SurveyOperator> {

    //批量插入干员练度信息表
    Integer insertBatch(@Param("tableName") String tableName,
                        @Param("list") List<SurveyOperator> characterList);

    void updateByUid(@Param("tableName") String tableName, @Param("item") SurveyOperator surveyOperator);

    SurveyStatisticsUser selectBakId(@Param("uid") String uid);

    List<SurveyOperator> selectBakOperatorDataById(@Param("id") Long id);
}
