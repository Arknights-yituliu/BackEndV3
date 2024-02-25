package com.lhs.entity.po.item;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lhs.entity.vo.item.PackInfoVO;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.Date;

@Entity
@Data
@TableName
public class PackInfo {

    @TableId
    @Id
    private Long id;
    private String officialName;
    private String displayName;
    private double price;
    private Integer saleStatus;
    private String saleType;
    private String imageName;
    private Integer originium;
    private Integer orundum;
    private Integer gachaTicket;
    private Integer tenGachaTicket;
    private Date start;
    private Date end;
    private Date createTime;

    public void copy(PackInfoVO packInfoVO){
        this.id = packInfoVO.getId();
        this.officialName = packInfoVO.getOfficialName();
        this.displayName = packInfoVO.getDisplayName();
        this.price = packInfoVO.getPrice();
        this.saleStatus = packInfoVO.getSaleStatus();
        this.saleType = packInfoVO.getSaleType();
        this.imageName = packInfoVO.getImageName();
        this.gachaTicket = packInfoVO.getGachaTicket();
        this.tenGachaTicket = packInfoVO.getTenGachaTicket();
        this.originium = packInfoVO.getOriginium();
        this.orundum = packInfoVO.getOrundum();
        this.start = new Date(packInfoVO.getStart());
        this.end = new Date(packInfoVO.getEnd());
    }

}
