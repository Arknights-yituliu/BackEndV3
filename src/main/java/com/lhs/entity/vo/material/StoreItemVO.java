package com.lhs.entity.vo.material;


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
