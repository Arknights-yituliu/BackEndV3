package com.lhs.entity.vo.material;


import com.lhs.entity.po.material.PackInfo;
import lombok.Data;

import java.util.List;

@Data
public class PackInfoVO {
    private Long id;  //数据库索引，类雪花算法生成
    private String officialName;  //礼包官方名称
    private String displayName;  //前端展示名称
    private double price;  //价格
    private String saleType;  //礼包类型
    private String tags;
    private String imageName;  //礼包图片名称
    private String imageLink;  //礼包图片名称
    private Integer gachaTicket;  //单抽券数量
    private Integer tenGachaTicket; //十连券数量
    private Integer originium; //源石数量
    private Integer orundum; //合成玉数量
    private Double draws;  //总抽数
    private Double drawPrice; //每一抽价格
    private Double packedOriginium; //礼包内物品折算为源石的价值
    private Double packedOriginiumPrice; //每源石（折算物资后）价格
    private Double drawEfficiency; //氪金性价比
    private Double packEfficiency; //综合性价比
    private Double drawsKernel;  //总抽数（含蓝票）
    private Double drawPriceKernel; //每一抽价格（含蓝票）
    private Double packedOriginiumKernel; //礼包内物品折算为源石的价值（含蓝票）
    private Double packedOriginiumPriceKernel; //每源石（折算物资后）价格（含蓝票）
    private Double drawEfficiencyKernel; //氪金性价比（含蓝票）
    private Double packEfficiencyKernel; //综合性价比（含蓝票）
    private List<PackContentVO> packContent;  //礼包非抽卡道具内容
    private Long start;
    private Long end;
    private String note;


    public void copy(PackInfo packInfo) {
        this.id = packInfo.getId();
        this.officialName = packInfo.getOfficialName();
        this.displayName = packInfo.getDisplayName();
        this.price = packInfo.getPrice();
        this.saleType = packInfo.getSaleType();
        this.gachaTicket = packInfo.getGachaTicket();
        this.tenGachaTicket = packInfo.getTenGachaTicket();
        this.originium = packInfo.getOriginium();
        this.orundum = packInfo.getOrundum();
        this.start = packInfo.getStart().getTime();
        this.end = packInfo.getEnd().getTime();
        this.note = packInfo.getNote();
        this.tags = packInfo.getTags();
    }


}
