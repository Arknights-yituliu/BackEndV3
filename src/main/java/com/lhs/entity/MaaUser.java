package com.lhs.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.Random;

@Data
@TableName("maa_user")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaaUser {
    @TableId
    private Long id; //存储id
    private String penguinId;  //企鹅id
    private Integer operatorTotal; //干员总数
    private String server;  //地区 一般为CN
    private String source; //来源
    private String version;  //maa版本号
    private String tableName; //存储的表名
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;  //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private String ip; //ip

    public void init(){
        long millis = System.currentTimeMillis();
        int end3 = new Random().nextInt(9999);
        this.id = millis*1000+end3;
    }




}
