package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName
@Data
public class QuestionnaireResult {
    @TableId
    private Long id;
    private Long ipId;
    private Integer QuestionnaireType;
    private String QuestionnaireContent;
    private Long createTime;
    private Long updateTime;
}
