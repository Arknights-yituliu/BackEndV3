package com.lhs.entity.dto.stage;

import lombok.Data;

import java.util.List;

@Data
public class CompositeTableDTO {
    private String id;
    private List<ItemCostDTO> itemCost;
}



