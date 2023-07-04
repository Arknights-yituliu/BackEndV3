package com.lhs.entity.stage;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("work_shop_products")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkShopProducts {
    @TableId
    private Long id;
    private String rank;
    private Double expectValue;
    private Double expCoefficient;
}
