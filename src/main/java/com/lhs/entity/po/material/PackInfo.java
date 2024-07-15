package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lhs.entity.vo.item.PackInfoVO;
import lombok.Data;

import java.util.Date;


@Data
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
    private Long contentId;
    private Boolean deleteFlag = false;

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



}
