package com.lhs.entity.vo.dev;

/**
 * URL总访问量统计VO
 * 用于返回每个URL在时间范围内的总访问次数
 */
public class UrlTotalVisitVO {
    private String url;
    private Long totalCount;

    public UrlTotalVisitVO() {
    }

    public UrlTotalVisitVO(String url, Long totalCount) {
        this.url = url;
        this.totalCount = totalCount;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }
}
