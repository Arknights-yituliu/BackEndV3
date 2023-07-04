package com.lhs.vo.stage;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "售卖的商品")
public class StoreItem {
    @ApiModelProperty(value = "商店区域")
    private Integer itemArea;

    @ApiModelProperty(value = "物品id")
    private String itemId;

    @ApiModelProperty(value = "物品名称")
    private String itemName;

    @ApiModelProperty(value = "性价比")
    private Double itemPPR;

    @ApiModelProperty(value = "售卖价格")
    private Integer itemPrice;

    @ApiModelProperty(value = "售卖数量")
    private Integer itemQuantity;

    @ApiModelProperty(value = "库存")
    private Integer itemStock;
}
