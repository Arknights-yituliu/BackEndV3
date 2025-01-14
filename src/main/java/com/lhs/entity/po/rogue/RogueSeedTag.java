package com.lhs.entity.po.rogue;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;


@TableName
public class RogueSeedTag {
    private Long tagId;
    private Long seedId;
    private String tag;
    private Date createTime;
    private Boolean deleteFlag;

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public Long getSeedId() {
        return seedId;
    }

    public void setSeedId(Long seedId) {
        this.seedId = seedId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Boolean getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Boolean deleteFlag) {
        this.deleteFlag = deleteFlag;
    }
}
