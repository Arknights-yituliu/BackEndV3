package com.lhs.service.request;

import lombok.Data;

@Data
public class StoreActJsonVo {

    private String itemName;
    private Integer itemQuantity;
    private Integer itemPrice;
    private Double itemPPR;
    private Integer itemStock;
    private Integer itemArea;
    private String itemId;


}
