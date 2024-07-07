package com.lhs.entity.po.common;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName
@Data
public class ServiceInfo {
    @TableId
    private String infoKey;
    private String info;
}
