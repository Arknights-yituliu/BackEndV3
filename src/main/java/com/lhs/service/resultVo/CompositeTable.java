package com.lhs.service.resultVo;

import lombok.Data;

import java.util.List;

@Data
public class CompositeTable {
    private String id;
    private List<ItemCost> itemCost;
}



