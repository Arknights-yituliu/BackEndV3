package com.lhs.entity.po.item;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lhs.entity.vo.item.PackInfoVO;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.Date;
import java.util.List;


@Data
@TableName
public class PackInfo {
    @TableId
    private Long id;
    private String officialName;
    private String displayName;
    private double price;
    private String saleType;
    private String imageName;
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

    public void copy(PackInfoVO packInfoVO){
        this.id = packInfoVO.getId();
        this.officialName = packInfoVO.getOfficialName();
        this.displayName = packInfoVO.getDisplayName();
        this.price = packInfoVO.getPrice();
        this.saleType = packInfoVO.getSaleType();
        this.tags = ListToString(packInfoVO.getTags());
        this.imageName = packInfoVO.getImageName();
        this.gachaTicket = packInfoVO.getGachaTicket();
        this.tenGachaTicket = packInfoVO.getTenGachaTicket();
        this.originium = packInfoVO.getOriginium();
        this.orundum = packInfoVO.getOrundum();
        this.start = new Date(packInfoVO.getStart());
        this.end = new Date(packInfoVO.getEnd());
        this.note = packInfoVO.getNote();
    }

    private String ListToString(List<String> list){
        if(list==null||list.isEmpty()){
            return "";
        }

        StringBuilder tagStr = new StringBuilder();
        for(String str:list){
            if(str.isEmpty()){
                continue;
            }
            if(!tagStr.isEmpty()){
                tagStr.append(",").append(str);
            }

        }

        return tagStr.toString();
    }

}
