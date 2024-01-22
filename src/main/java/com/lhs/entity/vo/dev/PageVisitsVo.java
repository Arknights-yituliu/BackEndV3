package com.lhs.entity.vo.dev;

import com.lhs.entity.po.dev.PageVisits;
import lombok.Data;

import java.util.List;

@Data
public class PageVisitsVo {
    private String pagePath;
    private Integer visitsCount;
    private List<PageVisits> pageVisitsList;
}
