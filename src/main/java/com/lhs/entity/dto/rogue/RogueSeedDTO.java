package com.lhs.entity.dto.rogue;

import java.util.List;



public class RogueSeedDTO {
    private Long seedId;
    private String seed;
    private String rogueTheme;
    private String rogueVersion;
    private String source;
    private Integer seedType;
    private Integer difficulty;
    private Integer ratingCount;
    private Integer rating;
    private List<String> squad;
    private List<String> operatorTeam;
    private Integer score;
    private String description;
    private List<String> tags;
    private String summaryImageLink;

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

    public Integer getSeedType() {
        return seedType;
    }

    public void setSeedType(Integer seedType) {
        this.seedType = seedType;
    }

    public List<String> getSquad() {
        return squad;
    }

    public void setSquad(List<String> squad) {
        this.squad = squad;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
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

    @Override
    public String toString() {
        return "RogueSeedDTO{" +
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
                '}';
    }
}
