package com.lhs.entity.vo.dev;

import com.lhs.entity.po.admin.PageViewStatistics;
import lombok.Data;

@Data
public class PageViewStatisticsVo {
    private String pagePath;

    private Integer pageView;
    private Long viewTime;

    public void copy(PageViewStatistics pageViewStatistics){
        this.pagePath = pageViewStatistics.getPagePath();
        this.pageView = pageViewStatistics.getPageView();
        this.viewTime = pageViewStatistics.getCreateTime().getTime();
    }
}
