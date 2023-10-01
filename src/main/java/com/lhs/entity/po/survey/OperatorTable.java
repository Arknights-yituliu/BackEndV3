package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperatorTable {

    @TableId
    public String charId;
    public String name;
    public String obtainApproach;
    public Integer rarity;
    public Date updateTime;

}