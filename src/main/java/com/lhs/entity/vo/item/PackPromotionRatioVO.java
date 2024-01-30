package com.lhs.entity.vo.item;


import com.lhs.entity.po.item.PackPromotionRatio;
import lombok.Data;

import java.util.List;

@Data
public class PackPromotionRatioVO {
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
    private Double drawCount;
    private Double eachDrawPrice; //每一抽价格
    private Double eachOriginiumPrice; //每源石（折算物资后）价格
    private Double promotionRatioForMoney; //氪金性价比
    private Double equivalentOriginium; //礼包内物品折算为源石的价值
    private Double promotionRatioForComprehensive; //综合性价比
    private Long start;
    private Long end;
    private List<PackContentVO> packContent;
    private Boolean newPack;

    public void copy(PackPromotionRatio packPromotionRatio){
        this.id = packPromotionRatio.getId();
        this.typeId = packPromotionRatio.getTypeId();
        this.name = packPromotionRatio.getName();
        this.displayName = packPromotionRatio.getDisplayName();
        this.price = packPromotionRatio.getPrice();
        this.state = packPromotionRatio.getState();
        this.type = packPromotionRatio.getType();
        this.ticketGacha = packPromotionRatio.getTicketGacha();
        this.ticketGacha10 = packPromotionRatio.getTicketGacha10();
        this.originium = packPromotionRatio.getOriginium();
        this.orundum = packPromotionRatio.getOrundum();
        this.start = packPromotionRatio.getStart().getTime();
        this.end = packPromotionRatio.getEnd().getTime();
    }
}
