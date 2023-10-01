package com.lhs.entity.po.maa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecruitStatistics {
    private String statisticalItem ;
    private Integer statisticalResult;
}
