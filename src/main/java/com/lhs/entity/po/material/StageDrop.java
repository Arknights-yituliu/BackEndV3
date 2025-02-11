package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;


@Data
@TableName
public class StageDrop {
    @TableId
    private Long id;
    private String stageId;
    private String drops;
    private Integer times;
    private String server;
    private String source;
    private String uid;
    private String version;
    private Date createTime;

}
