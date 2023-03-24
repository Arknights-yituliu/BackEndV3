package com.lhs.entity;


import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("item")   //用于存储最终的等效理智/绿票价值
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @TableId

    private Long id;
    private String itemId;  //物品id
    private String itemName; //物品名称
    @TableField(value ="item_value_commendation_cert")
    private Double itemValue; //物品价值 单位：绿票
    @TableField(value ="item_value_ap")
    private Double itemValueAp; //物品价值 单位：理智
    private String  type; //物品稀有度
    private Integer rarity; //物品稀有度
    private Integer cardNum;  //前端排序的用索引
    private Double expCoefficient;  //经验书系数
    private Double weight;   //加工站爆率

}
