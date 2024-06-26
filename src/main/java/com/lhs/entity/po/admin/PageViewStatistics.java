package com.lhs.entity.po.admin;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("page_view_statistics")
public class PageViewStatistics {
    @TableId
    private String redisKey;
    private String pagePath;
    private String viewTime;
    private Integer pageView;
    private Date createTime;



}

