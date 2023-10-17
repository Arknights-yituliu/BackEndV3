package com.lhs.entity.vo.item;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemVO {
    @ExcelProperty("物品id")
    private String itemId;  //物品id
    @ExcelProperty("物品名称")
    private String itemName; //物品名称
    @ExcelProperty("物品价值 单位：绿票")
    private Double itemValue; //物品价值 单位：绿票
    @ExcelProperty("物品价值 单位：理智")
    private Double itemValueAp; //物品价值 单位：理智
    @ExcelProperty("物品稀有度")
    private Integer rarity; //物品稀有度

}
