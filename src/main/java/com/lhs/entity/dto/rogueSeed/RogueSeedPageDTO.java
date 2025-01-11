package com.lhs.entity.dto.rogueSeed;


import java.util.List;

public class RogueSeedPageDTO {

    private String sortCondition;
    private Integer pageSize;
    private Integer pageNum;
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

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
}
