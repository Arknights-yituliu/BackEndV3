package com.lhs.entity.po.material;

import lombok.Data;

@Data
public class StageDropV1 {
    private Long id;
    private String stageId;
    private Integer times;
    private String server;
    private String source;
    private String uid;
    private String version;
    private Long createTime;
}
