package com.lhs.entity.dto.item;

import lombok.Data;

import java.util.List;

@Data
public class StageDropDTO {
    private String stageId;
    private Integer times;
    private List<StageDropDetailDTO> drops;
    private String server;
    private String source;
    private String version;


}
