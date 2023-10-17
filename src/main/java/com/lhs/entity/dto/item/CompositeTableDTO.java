package com.lhs.entity.dto.item;

import lombok.Data;

import java.util.List;

@Data
public class CompositeTableDTO {
    private String id;
    private List<ItemCostDTO> itemCost;
}



