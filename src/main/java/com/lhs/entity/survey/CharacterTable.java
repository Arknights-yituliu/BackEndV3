package com.lhs.entity.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("character_table")
public class CharacterTable {
     @TableId
     public String id;
     public String charId;
     public String name;
     public Date updateTime;
}
