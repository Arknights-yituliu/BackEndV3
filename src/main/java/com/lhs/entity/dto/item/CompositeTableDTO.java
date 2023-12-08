package com.lhs.entity.dto.item;

import lombok.Data;

import java.util.List;

@Data
public class CompositeTableDTO {
    private String id;
    private String name;
    private Boolean resolve;
    private List<ItemCostDTO> pathway;
}



