package com.lhs.service.resultVo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitData {
    private List<String> tags;
    private String name;
    private String  charId;
    private Integer rarity;
    private String level;
    private String profession;
    private String position;
}
