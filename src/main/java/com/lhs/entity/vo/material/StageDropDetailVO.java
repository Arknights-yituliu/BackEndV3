package com.lhs.entity.vo.material;
import com.lhs.entity.po.material.StageDropDetail;
import lombok.Data;

@Data
public class StageDropDetailVO {
    private Long childId;
    private String itemId;
    private Integer quantity;
    private String dropType;

    public void copyByDropDetail(StageDropDetail stageDropDetail){
        this.itemId = stageDropDetail.getItemId();
        this.quantity = stageDropDetail.getQuantity();
        this.dropType = stageDropDetail.getDropType();
    }
}
