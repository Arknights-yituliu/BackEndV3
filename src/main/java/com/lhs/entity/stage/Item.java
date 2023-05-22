package com.lhs.entity.stage;


import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("item")   //用于存储最终的等效理智/绿票价值
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(value = "物品价值返回对象")
public class Item {

    @TableId
    @ApiModelProperty("索引id")
    private Long id;

    @ApiModelProperty("物品id")
    private String itemId;  //物品id

    @ApiModelProperty("物品名称")
    private String itemName; //物品名称

    @ApiModelProperty("物品价值 单位：绿票")
    @TableField(value ="item_value_cert")
    private Double itemValue; //物品价值 单位：绿票

    @TableField(value ="item_value_ap")
    @ApiModelProperty("物品价值 单位：理智")
    private Double itemValueAp; //物品价值 单位：理智

    @ApiModelProperty("物品稀有度")
    private String  type; //物品稀有度

    @ApiModelProperty("物品稀有度")
    private Integer rarity; //物品稀有度

    @ApiModelProperty("前端排序的用索引")
    private Integer cardNum;  //前端排序的用索引

    @ApiModelProperty("经验书系数")
    private Double expCoefficient;  //经验书系数

    @ApiModelProperty("加工站爆率")
    private Double weight;   //加工站爆率

}
