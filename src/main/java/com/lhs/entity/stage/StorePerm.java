package com.lhs.entity.stage;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("store_perm")
@ApiModel(value = "常驻商店性价比返回对象",description = "返回的是Map<商店类型,常驻商店性价比返回对象>")
public class StorePerm {
    @TableId
    private Integer id;
    @ApiModelProperty("商店类型")
    private String storeType;
    @ApiModelProperty("物品名称")
    private String itemName;
    @ApiModelProperty("物品id")
    private String itemId;
    @ApiModelProperty("售价")
    private Double cost;
    @ApiModelProperty("性价比")
    private Double costPer;
    @ApiModelProperty("售卖数量")
    private Integer quantity;
    @ApiModelProperty("物品星级")
    private Integer rarity;
}
