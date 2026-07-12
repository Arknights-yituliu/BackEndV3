package com.lhs.entity.dto;

/**
 * 访问日志数据传输对象
 * 用于接收前端上报的访问记录
 */
public class AccessLogDTO {
    private String url;
    private String region;

    public AccessLogDTO() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
