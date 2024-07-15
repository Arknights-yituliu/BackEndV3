package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class StageDrop {
    @TableId
    @Id
    private Long id;
    private String stageId;
    private Integer times;
    private String server;
    private String source;
    private String uid;
    private String version;
    private Long createTime;

}
