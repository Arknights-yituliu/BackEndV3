package com.lhs.entity.vo.material;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.po.material.PackInfo;

import java.util.List;

public class PackInfoVOV5 {
    private Long id;  //数据库索引，类雪花算法生成
    private String officialName;  //礼包官方名称
    private String displayName;  //前端展示名称
    private double price;  //价格
    private String saleType;  //礼包类型
    private String tags; //标签
    private String imageName;  //礼包图片名称
    private String imageLink;  //礼包图片名称
    private Integer gachaTicket;  //单抽券数量
    private Integer tenGachaTicket; //十连券数量
    private Integer originium; //源石数量
    private Integer orundum; //合成玉数量
    private List<PackContentVO> packContent;  //礼包非抽卡道具内容

    private Long start;
    private Long end;
    private String note;  //注意事项

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOfficialName() {
        return officialName;
    }

    public void setOfficialName(String officialName) {
        this.officialName = officialName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getSaleType() {
        return saleType;
    }

    public void setSaleType(String saleType) {
        this.saleType = saleType;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageLink() {
        return imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    public Integer getGachaTicket() {
        return gachaTicket;
    }

    public void setGachaTicket(Integer gachaTicket) {
        this.gachaTicket = gachaTicket;
    }

    public Integer getTenGachaTicket() {
        return tenGachaTicket;
    }

    public void setTenGachaTicket(Integer tenGachaTicket) {
        this.tenGachaTicket = tenGachaTicket;
    }

    public Integer getOriginium() {
        return originium;
    }

    public void setOriginium(Integer originium) {
        this.originium = originium;
    }

    public Integer getOrundum() {
        return orundum;
    }

    public void setOrundum(Integer orundum) {
        this.orundum = orundum;
    }

    public List<PackContentVO> getPackContent() {
        return packContent;
    }

    public void setPackContent(List<PackContentVO> packContent) {
        this.packContent = packContent;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

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

        String text = packInfo.getContent();
        if(text!=null&&text.length()>10){
            this.packContent = JsonMapper.parseJSONArray(text, new TypeReference<>() {
            });
        }

        this.note = packInfo.getNote();
        this.tags = packInfo.getTags();
    }
}
