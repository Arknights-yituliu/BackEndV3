package com.lhs.entity.po.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.Date;

@TableName
@Data
@Entity
public class Admin {
    @Id
    @TableId
    private Long id;
    private String developer;
    private String email;
    private Integer level;
    private String token;
    private Date expire;


}
