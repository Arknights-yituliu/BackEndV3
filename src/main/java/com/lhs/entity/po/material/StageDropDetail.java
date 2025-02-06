package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName
public class StageDropDetail {
    @TableId
    private Long id;
    private String uid;
    private Long childId;
    private String itemId;
    private String dropType;
    private Integer quantity;
}
