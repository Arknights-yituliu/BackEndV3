package com.lhs.entity.other;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageVisits {
    private Long id;
    private String pagePath;
    private String visitsTime;
    private Integer visitsCount;
    private Date createTime;
    private String redisKey;
}

