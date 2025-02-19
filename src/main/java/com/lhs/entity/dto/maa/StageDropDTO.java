package com.lhs.entity.dto.maa;

import lombok.Data;

@Data
public class StageDropDTO {
    private Long id;
    private String stageId;
    private Integer times;
    private String server;
    private String source;
    private String uid;
    private String version;
    private Long createTime;
}
