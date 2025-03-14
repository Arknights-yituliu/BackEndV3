package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.QuestionnaireStatisticsResult;
import org.apache.ibatis.annotations.Param;

public interface QuestionnaireStatisticsResultMapper extends BaseMapper<QuestionnaireStatisticsResult> {

    void expireOldData(@Param("targetRecordType") Integer targetRecordType, @Param("currentRecordType") Integer currentRecordType);
}
