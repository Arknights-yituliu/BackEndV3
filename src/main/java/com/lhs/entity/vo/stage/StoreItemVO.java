package com.lhs.entity.vo.stage;


import lombok.Data;

@Data

public class StoreItemVO {
    private Integer itemArea;

    private String itemId;

    private String itemName;

    private Double itemPPR;

    private Integer itemPrice;

    private Integer itemQuantity;

    private Integer itemStock;
}
