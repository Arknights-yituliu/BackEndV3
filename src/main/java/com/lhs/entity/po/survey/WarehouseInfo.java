package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName
public class WarehouseInfo {
    @TableId
    private Long id;
    private Long uid;
    private String akUid;
    private String itemId;
    private Integer quantity;
    private Long lastDataId;
    private Long updateTime;
}
