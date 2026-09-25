package com.lhs.entity.dto.survey;

import lombok.Data;

import java.util.List;

/**
 * 一图流内手动维护的干员练度数据。
 */
@Data
public class ManualOperatorDataDTO {
    private List<OperatorProgressionDataDTO> operatorDataList;
}
