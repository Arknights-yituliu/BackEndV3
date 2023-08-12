package com.lhs.vo.user;

import com.lhs.entity.other.PageVisits;
import lombok.Data;

import java.util.List;

@Data
public class PageVisitsVo {
    private String pagePath;
    private Integer visitsCount;
    private List<PageVisits> pageVisitsList;
}
