package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("operator_progression_manual_data")
public class OperatorProgressionManualData {

    @TableId
    private String akUid;
    private String operatorProgression;
    private Date createTime;
    private Date updateTime;
}
