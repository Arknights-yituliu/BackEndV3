package com.lhs.entity.dto.material;


import com.lhs.entity.po.material.PackInfo;
import com.lhs.entity.vo.material.PackContentVO;
import lombok.Data;

import java.util.List;


public class PackInfoDTO {
    private Long id;  //数据库索引，类雪花算法生成
    private String officialName;  //礼包官方名称
    private String displayName;  //前端展示名称
    private double price;  //价格
    private String saleType;  //礼包类型
    private List<String> tags;
    private String imageLink;  //礼包图片名称
    private Integer gachaTicket;  //单抽券数量
    private Integer tenGachaTicket; //十连券数量
    private Integer originium; //源石数量
    private Integer orundum; //合成玉数量
    private List<PackContentVO> packContent;  //礼包非抽卡道具内容
    private Long start;
    private Long end;
    private String note;


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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
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
}
