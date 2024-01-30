package com.lhs.entity.po.item;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lhs.entity.vo.item.PackPromotionRatioVO;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.Date;

@Entity
@Data
@TableName
public class PackPromotionRatio {

    @TableId
    @Id
    private Long id;
    private Long typeId;
    private String name;
    private String displayName;
    private double price;
    private Integer state;
    private String type;
    private Integer ticketGacha;
    private Integer ticketGacha10;
    private Integer originium;
    private Integer orundum;
    private Date start;
    private Date end;

    public void copy(PackPromotionRatioVO packPromotionRatioVO){
        this.id = packPromotionRatioVO.getId();
        this.typeId = packPromotionRatioVO.getTypeId();
        this.name = packPromotionRatioVO.getName();
        this.displayName = packPromotionRatioVO.getDisplayName();
        this.price = packPromotionRatioVO.getPrice();
        this.state = packPromotionRatioVO.getState();
        this.type = packPromotionRatioVO.getType();
        this.ticketGacha = packPromotionRatioVO.getTicketGacha();
        this.ticketGacha10 = packPromotionRatioVO.getTicketGacha10();
        this.originium = packPromotionRatioVO.getOriginium();
        this.orundum = packPromotionRatioVO.getOrundum();
        this.start = new Date(packPromotionRatioVO.getStart());
        this.end = new Date(packPromotionRatioVO.getEnd());
    }

}
