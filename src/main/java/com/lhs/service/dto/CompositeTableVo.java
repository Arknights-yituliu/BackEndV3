package com.lhs.service.dto;

import lombok.Data;

import java.util.List;

@Data
public class CompositeTableVo {
    private String id;
    private List<ItemCost> itemCost;
}



