package com.lhs.entity.po.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName
public class UserConfig {
    @TableId
    private Long uid;
    private String config;
    private Date createTime;  //创建时间
    private Date updateTime;  //最后一次上传数据时间
}
