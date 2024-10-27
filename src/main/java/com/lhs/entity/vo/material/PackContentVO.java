package com.lhs.entity.vo.material;

import com.lhs.entity.po.material.PackContent;
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
