package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;

import lombok.Data;


@Data
public class StageDrop {
    @TableId

    private Long id;
    private String stageId;
    private Integer times;
    private String server;
    private String source;
    private String uid;
    private String version;
    private Long createTime;

}
