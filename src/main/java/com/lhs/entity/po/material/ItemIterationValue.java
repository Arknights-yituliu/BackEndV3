package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("item_iteration_value")
@Data
public class ItemIterationValue {
    @TableId
    private Long id;
    private String itemName;
    private String itemId;
    private Double iterationValue;
    private String version;
    private String stageCode;
}
