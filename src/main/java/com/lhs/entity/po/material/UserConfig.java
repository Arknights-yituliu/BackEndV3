package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName
@Data
public class UserConfig {

    @TableId
    private Long uid;
    private String config;
    private Long createTime;
    private Long updateTime;
}
