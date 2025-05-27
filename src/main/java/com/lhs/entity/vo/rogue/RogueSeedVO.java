package com.lhs.entity.vo.rogue;


import java.util.List;


public class RogueSeedVO {
    private Long seedId;
    private String seed;
    private String rogueVersion;
    private Integer seedType;
    private Double rating;
    private Integer thumbsUp;
    private Integer thumbsDown;
    private Integer ratingCount;
    private Integer uploadTimes;
    private Integer difficulty;
    private String rogueTheme;
    private String squad;
    private List<String> operatorTeam;
    private String description;
    private List<String> tags;
    private String summaryImageLink;
    private Long createTime;

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

    public String getRogueVersion() {
        return rogueVersion;
    }

    public void setRogueVersion(String rogueVersion) {
        this.rogueVersion = rogueVersion;
    }

    public Integer getSeedType() {
        return seedType;
    }

    public void setSeedType(Integer seedType) {
        this.seedType = seedType;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getThumbsUp() {
        return thumbsUp;
    }

    public void setThumbsUp(Integer thumbsUp) {
        this.thumbsUp = thumbsUp;
    }

    public Integer getThumbsDown() {
        return thumbsDown;
    }

    public void setThumbsDown(Integer thumbsDown) {
        this.thumbsDown = thumbsDown;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Integer getUploadTimes() {
        return uploadTimes;
    }

    public void setUploadTimes(Integer uploadTimes) {
        this.uploadTimes = uploadTimes;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public String getRogueTheme() {
        return rogueTheme;
    }

    public void setRogueTheme(String rogueTheme) {
        this.rogueTheme = rogueTheme;
    }

    public String getSquad() {
        return squad;
    }

    public void setSquad(String squad) {
        this.squad = squad;
    }

    public List<String> getOperatorTeam() {
        return operatorTeam;
    }

    public void setOperatorTeam(List<String> operatorTeam) {
        this.operatorTeam = operatorTeam;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getSummaryImageLink() {
        return summaryImageLink;
    }

    public void setSummaryImageLink(String summaryImageLink) {
        this.summaryImageLink = summaryImageLink;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "RogueSeedPageVO{" +
                "seedId=" + seedId +
                ", seed='" + seed + '\'' +
                ", rogueVersion='" + rogueVersion + '\'' +
                ", rating=" + rating +
                ", ratingCount=" + ratingCount +
                ", uploadTimes=" + uploadTimes +
                ", difficulty=" + difficulty +
                ", rogueTheme='" + rogueTheme + '\'' +
                ", squad='" + squad + '\'' +
                ", operatorTeam=" + operatorTeam +
                ", description='" + description + '\'' +
                ", tags=" + tags +
                ", summaryImageLink='" + summaryImageLink + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
