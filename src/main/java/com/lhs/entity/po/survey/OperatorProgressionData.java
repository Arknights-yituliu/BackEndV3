package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName
public class OperatorProgressionData {

    @TableId
    private String akUid;
    private String operatorProgression;

    private Date createTime;

    public String getAkUid() {
        return akUid;
    }

    public void setAkUid(String akUid) {
        this.akUid = akUid;
    }

    public String getOperatorProgression() {
        return operatorProgression;
    }

    public void setOperatorProgression(String operatorProgression) {
        this.operatorProgression = operatorProgression;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
