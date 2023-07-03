package com.lhs.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("developer")
@Data
public class Developer {
    private String developer;
    @TableId
    private String email;
    private Integer level;



}
