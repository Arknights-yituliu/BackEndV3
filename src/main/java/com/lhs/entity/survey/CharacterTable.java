package com.lhs.entity.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@TableName("character_table")
public class CharacterTable {

    @TableId
    @Id
    public String charId;
    public String name;
    public String obtainApproach;
    public Integer rarity;
    public Date updateTime;

}