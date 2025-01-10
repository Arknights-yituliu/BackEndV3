package com.lhs.entity.po.rogueSeed;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;


@TableName
public class RogueSeed {
    @TableId
    private Long seedId;
    private String seed;
    private String rogueTheme;
    private String rogueVersion;
    private String source;
    private Integer difficulty;
    private Integer ratingCount;
    private Integer rating;
    private String squad;
    private String operatorTeam;
    private String description;
    private String tags;
    private String summaryImageLink;
    private Date createTime;
    private Date updateTime;
    private Boolean deleteFlag;

    public Long getSeedId() {
        return seedId;
    }

    public void setSeedId(Long seedId) {
        this.seedId = seedId;
    }

    public String getSeed() {
        return seed;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }

    public String getRogueTheme() {
        return rogueTheme;
    }

    public void setRogueTheme(String rogueTheme) {
        this.rogueTheme = rogueTheme;
    }

    public String getRogueVersion() {
        return rogueVersion;
    }

    public void setRogueVersion(String rogueVersion) {
        this.rogueVersion = rogueVersion;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getSquad() {
        return squad;
    }

    public void setSquad(String squad) {
        this.squad = squad;
    }

    public String getOperatorTeam() {
        return operatorTeam;
    }

    public void setOperatorTeam(String operatorTeam) {
        this.operatorTeam = operatorTeam;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getSummaryImageLink() {
        return summaryImageLink;
    }

    public void setSummaryImageLink(String summaryImageLink) {
        this.summaryImageLink = summaryImageLink;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Boolean getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Boolean deleteFlag) {
        this.deleteFlag = deleteFlag;
    }


    @Override
    public String toString() {
        return "RogueSeed{" +
                "seedId=" + seedId +
                ", seed='" + seed + '\'' +
                ", rogueTheme='" + rogueTheme + '\'' +
                ", rogueVersion='" + rogueVersion + '\'' +
                ", source='" + source + '\'' +
                ", difficulty=" + difficulty +
                ", ratingCount=" + ratingCount +
                ", rating=" + rating +
                ", squad='" + squad + '\'' +
                ", operatorTeam='" + operatorTeam + '\'' +
                ", description='" + description + '\'' +
                ", tags='" + tags + '\'' +
                ", summaryImageLink='" + summaryImageLink + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", deleteFlag=" + deleteFlag +
                '}';
    }
}
