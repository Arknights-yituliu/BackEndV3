package com.lhs.entity.dto.stage;

import lombok.Data;

import java.util.List;

@Data
public class CompositeTable {
    private String id;
    private List<ItemCost> itemCost;
}



