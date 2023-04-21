package com.lhs.service.request;

import lombok.Data;

import java.util.List;

@Data
public class CompositeTableJsonVo {
    private String id;
    private List<ItemCost> itemCost;
}



