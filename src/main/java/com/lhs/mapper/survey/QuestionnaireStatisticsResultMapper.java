package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.QuestionnaireStatisticsResult;
import org.apache.ibatis.annotations.Param;

public interface QuestionnaireStatisticsResultMapper extends BaseMapper<QuestionnaireStatisticsResult> {

    /**
     * 更新问卷统计结果状态
     * @param questionnaireCode  问卷编号
     * @param targetRecordType 更新后的状态
     * @param currentRecordType  当前状态
     */
    void updateStatisticsResultRecordType(@Param("questionnaireCode") Integer questionnaireCode,@Param("targetRecordType") Integer targetRecordType, @Param("currentRecordType") Integer currentRecordType);
}
