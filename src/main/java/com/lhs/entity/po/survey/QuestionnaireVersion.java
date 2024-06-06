package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName
public class QuestionnaireVersion {

    private Integer id;
    private Integer version;
    private Integer versionName;
    private Integer questionnaireType;
    private Long createTime;
    

}
