package com.lhs.entity.vo.item;


import com.lhs.entity.po.item.PackInfo;
import lombok.Data;

import java.util.List;

@Data
public class PackInfoVO {
    private Long id;  //数据库索引，类雪花算法生成
    private Long sortId;  //排序id
    private String name;  //礼包官方名称
    private String displayName;  //前端展示名称
    private double price;  //价格
    private Integer state;  //售卖状态
    private String type;  //礼包类型
    private String fileName;  //礼包图片名称
    private Integer ticketGacha;  //单抽券数量
    private Integer ticketGacha10; //十连券数量
    private Integer originium; //源石数量
    private Integer orundum; //合成玉数量
    private Double drawCount;  //总抽数
    private Double eachDrawPrice; //每一抽价格
    private Double eachOriginiumPrice; //每源石（折算物资后）价格
    private Double promotionRatioForMoney; //氪金性价比
    private Double equivalentOriginium; //礼包内物品折算为源石的价值
    private Double promotionRatioForComprehensive; //综合性价比
    private Long start;
    private Long end;
    private List<PackContentVO> packContent;  //礼包非抽卡道具内容
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
