package com.lhs.service.dto;

import lombok.Data;

import java.util.List;

@Data
public class CompositeTableDto {
    private String id;
    private List<ItemCost> itemCost;
}



