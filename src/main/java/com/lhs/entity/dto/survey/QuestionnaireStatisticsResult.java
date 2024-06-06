package com.lhs.entity.dto.survey;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName
public class QuestionnaireStatisticsResult {
    private Integer createTime;
    private String statisticsResult;
    private String questionnaireType;
}
