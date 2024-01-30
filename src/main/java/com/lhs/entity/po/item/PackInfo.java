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
    private Long sortId;
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

    public void copy(PackInfoVO packInfoVO){
        this.id = packInfoVO.getId();
        this.sortId = packInfoVO.getSortId();
        this.name = packInfoVO.getName();
        this.displayName = packInfoVO.getDisplayName();
        this.price = packInfoVO.getPrice();
        this.state = packInfoVO.getState();
        this.type = packInfoVO.getType();
        this.ticketGacha = packInfoVO.getTicketGacha();
        this.ticketGacha10 = packInfoVO.getTicketGacha10();
        this.originium = packInfoVO.getOriginium();
        this.orundum = packInfoVO.getOrundum();
        this.start = new Date(packInfoVO.getStart());
        this.end = new Date(packInfoVO.getEnd());
    }

}
