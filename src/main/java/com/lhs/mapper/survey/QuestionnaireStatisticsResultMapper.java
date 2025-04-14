package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.QuestionnaireStatisticsResult;
import org.apache.ibatis.annotations.Param;

public interface QuestionnaireStatisticsResultMapper extends BaseMapper<QuestionnaireStatisticsResult> {

    /**
     * 更新问卷统计结果状态
     * @param targetRecordType 更新后的状态
     * @param version  复合索引
     */
    void updateRecordType(@Param("targetRecordType") Integer targetRecordType, @Param("version") String version);


    /**
     *
     * @param version 复合索引
     * @return 统计结果
     */
    QuestionnaireStatisticsResult getLastData(@Param("version") String  version );

}
