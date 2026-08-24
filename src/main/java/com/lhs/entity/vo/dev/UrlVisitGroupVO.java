package com.lhs.entity.vo.dev;

import java.util.List;

/**
 * URL分组访问量统计VO
 * 每个URL包含其所有天数的访问次数明细
 */
public class UrlVisitGroupVO {
    private String url;
    private List<UrlPeriodDataVO> data;

    public UrlVisitGroupVO() {
    }

    public UrlVisitGroupVO(String url, List<UrlPeriodDataVO> data) {
        this.url = url;
        this.data = data;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<UrlPeriodDataVO> getData() {
        return data;
    }

    public void setData(List<UrlPeriodDataVO> data) {
        this.data = data;
    }
}
