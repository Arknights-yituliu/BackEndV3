package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName
@Data
public class SurveySubmitter {
    @TableId
    private Long id;
    private String ip;
    private Boolean banned;
    private Long createTime;
    private Long updateTime;
}
