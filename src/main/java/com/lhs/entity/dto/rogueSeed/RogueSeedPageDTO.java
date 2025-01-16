package com.lhs.entity.dto.rogueSeed;


import java.util.List;

public class RogueSeedPageDTO {

    private String sortCondition;
    private Integer pageSize;
    private Integer pageNum;
    private Integer seedType;
    private List<String> keywords;

    public String getSortCondition() {
        return sortCondition;
    }

    public void setSortCondition(String sortCondition) {
        this.sortCondition = sortCondition;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getSeedType() {
        return seedType;
    }

    public void setSeedType(Integer seedType) {
        this.seedType = seedType;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }


    @Override
    public String toString() {
        return "RogueSeedPageDTO{" +
                "sortCondition='" + sortCondition + '\'' +
                ", pageSize=" + pageSize +
                ", pageNum=" + pageNum +
                ", seedType=" + seedType +
                ", keywords=" + keywords +
                '}';
    }
}
