package com.lhs.entity.po.item;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lhs.entity.vo.item.PackContentVO;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@TableName
public class PackContent {
    @Id
    @TableId
    private Long id;
    private Long packId;
    private String itemName;
    private String itemId;
    private Integer quantity;
    private Boolean archived;

    public void copy(PackContentVO packContentVO){
        this.id = packContentVO.getId();
        this.itemId = packContentVO.getItemId();
        this.itemName = packContentVO.getItemName();
        this.quantity = packContentVO.getQuantity();
    }
}
