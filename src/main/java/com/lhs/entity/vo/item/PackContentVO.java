package com.lhs.entity.vo.item;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lhs.entity.po.item.PackContent;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
public class PackContentVO {
    private Long id;  //数据库id
    private String itemName;  //物品名称
    private String itemId;  //物品id
    private Integer quantity; //物品数量

    public void copy(PackContent packContent){
        this.id = packContent.getId();
        this.itemId = packContent.getItemId();
        this.itemName = packContent.getItemName();
        this.quantity = packContent.getQuantity();
    }
}
