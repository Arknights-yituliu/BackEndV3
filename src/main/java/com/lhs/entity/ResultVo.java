package com.lhs.entity;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;


@Data
@TableName("result_vo")
public class ResultVo {   //储存各种结果的表
    @TableId
    private Long id;
    private String path;  //api路径
    private String result;  //各种计算结果
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public ResultVo(String path, String result) {
        Date date = new Date();
        this.id = date.getTime();
        this.path = path;
        this.result = result;
        this.createTime = date;
    }
}
