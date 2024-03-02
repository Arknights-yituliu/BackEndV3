package com.lhs.entity.po.item;

import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
@Entity
@Data
public class StageDropDetail {
    @TableId
    @Id
    private Long id;
    private String uid;
    private Long childId;
    private String itemId;
    private String dropType;
    private Integer quantity;
    private Long createTime;
}
