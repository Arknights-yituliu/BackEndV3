package com.lhs.entity.vo.item;


import com.lhs.entity.po.item.PackInfo;
import lombok.Data;

import java.util.List;

@Data
public class PackInfoVO {
    private Long id;
    private Long sortId;
    private String name;
    private String displayName;
    private double price;
    private Integer state;
    private String type;
    private String fileName;
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

    public void copy(PackInfo packInfo){
        this.id = packInfo.getId();
        this.sortId = packInfo.getSortId();
        this.name = packInfo.getName();
        this.displayName = packInfo.getDisplayName();
        this.price = packInfo.getPrice();
        this.state = packInfo.getState();
        this.type = packInfo.getType();
        this.fileName = packInfo.getFileName();
        this.ticketGacha = packInfo.getTicketGacha();
        this.ticketGacha10 = packInfo.getTicketGacha10();
        this.originium = packInfo.getOriginium();
        this.orundum = packInfo.getOrundum();
        this.start = packInfo.getStart().getTime();
        this.end = packInfo.getEnd().getTime();
    }
}
