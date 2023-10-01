package com.lhs.entity.po.dev;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("page_visits")
public class PageVisits {
    @TableId
    private String redisKey;
    private String pagePath;
    private String visitsTime;
    private Integer visitsCount;
    private Date createTime;

}

