package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName
public class OperatorUpdateTime {

    @TableId
    private String name;
    private String obtainApproach;
    private Date updateTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObtainApproach() {
        return obtainApproach;
    }

    public void setObtainApproach(String obtainApproach) {
        this.obtainApproach = obtainApproach;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
