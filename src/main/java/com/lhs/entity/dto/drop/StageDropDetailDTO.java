package com.lhs.entity.dto.drop;

import com.baomidou.mybatisplus.annotation.TableId;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName
public class StageDropDetailDTO {
    @TableId
    private Long id;
    private String uid;
    private Long childId;
    private String itemId;
    private String dropType;
    private Integer quantity;
}
