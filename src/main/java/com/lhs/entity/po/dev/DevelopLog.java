package com.lhs.entity.po.dev;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName
public class DevelopLog {

    @TableId
    private Long id;
    private String tag;
    private String author;
    private String text;
    private Date commitTime;
    private Date createTime;
    private Date updateTime;

}
