package com.lhs.entity.po.common;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName
@Data
public class DataCache {
    @TableId
    private String dataKey;
    private String dataValue;
}
