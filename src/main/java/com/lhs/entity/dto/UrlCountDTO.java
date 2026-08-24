package com.lhs.entity.dto;

/**
 * URL访问量统计原始DTO
 * 用于接收按URL分组聚合的访问量查询结果
 */
public class UrlCountDTO {

    private String url;
    private Long count;

    public UrlCountDTO() {
    }

    public UrlCountDTO(String url, Long count) {
        this.url = url;
        this.count = count;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
