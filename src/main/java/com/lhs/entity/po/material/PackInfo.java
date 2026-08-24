package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lhs.entity.dto.material.PackInfoDTO;
import com.lhs.entity.vo.material.PackInfoVO;


import java.util.Date;



@TableName
public class PackInfo {
    @TableId
    private Long id;
    private String officialName;
    private String displayName;
    private double price;
    private String saleType;
    private String tags;
    private Integer originium;
    private Integer orundum;
    private Integer gachaTicket;
    private Integer tenGachaTicket;
    private Date start;
    private Date end;
    private Date createTime;
    private String note;
    private String content;
    private Boolean deleteFlag = false;

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

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }



    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Boolean deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public void copy(PackInfoVO packInfoVO){
        this.id = packInfoVO.getId();
        this.officialName = packInfoVO.getOfficialName();
        this.displayName = packInfoVO.getDisplayName();
        this.price = packInfoVO.getPrice();
        this.saleType = packInfoVO.getSaleType();
        this.tags = packInfoVO.getTags();
        this.gachaTicket = packInfoVO.getGachaTicket();
        this.tenGachaTicket = packInfoVO.getTenGachaTicket();
        this.originium = packInfoVO.getOriginium();
        this.orundum = packInfoVO.getOrundum();
        this.start = new Date(packInfoVO.getStart());
        this.end = new Date(packInfoVO.getEnd());
        this.note = packInfoVO.getNote();
    }

    public void copy(PackInfoDTO packInfoDTO){
        this.id = packInfoDTO.getId();
        this.officialName = packInfoDTO.getOfficialName();
        this.displayName = packInfoDTO.getDisplayName();
        this.price = packInfoDTO.getPrice();
        this.saleType = packInfoDTO.getSaleType();
        this.tags =  String.join(",",packInfoDTO.getTags()) ;
        this.gachaTicket = packInfoDTO.getGachaTicket();
        this.tenGachaTicket = packInfoDTO.getTenGachaTicket();
        this.originium = packInfoDTO.getOriginium();
        this.orundum = packInfoDTO.getOrundum();
        this.start = new Date(packInfoDTO.getStart());
        this.end = new Date(packInfoDTO.getEnd());
        this.note = packInfoDTO.getNote();
    }


    @Override
    public String toString() {
        return "PackInfo{" +
                "id=" + id +
                ", officialName='" + officialName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", price=" + price +
                ", saleType='" + saleType + '\'' +
                ", tags='" + tags + '\'' +
                ", originium=" + originium +
                ", orundum=" + orundum +
                ", gachaTicket=" + gachaTicket +
                ", tenGachaTicket=" + tenGachaTicket +
                ", start=" + start +
                ", end=" + end +
                ", createTime=" + createTime +
                ", note='" + note + '\'' +
                ", content='" + content + '\'' +
                ", deleteFlag=" + deleteFlag +
                '}';
    }
}
