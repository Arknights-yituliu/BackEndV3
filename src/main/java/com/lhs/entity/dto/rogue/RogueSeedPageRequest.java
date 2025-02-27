package com.lhs.entity.dto.rogue;

import lombok.Data;

import java.util.List;

@Data
public class RogueSeedPageRequest {
    private Integer pageSize;
    private Integer pageNum;
    private String orderBy;
    private List<String> keyWords;
}
