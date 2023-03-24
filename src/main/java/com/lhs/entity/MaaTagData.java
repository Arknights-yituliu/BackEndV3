package com.lhs.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("maa_tag_data")  //存储maa的公招数据
public class MaaTagData {

    @TableId
    private Long id;
    private String uid;
    private String tag1;
    private String tag2;
    private String tag3;
    private String tag4;
    private String tag5;
    private Integer level;
    private Date createTime;
    private String tagResult;
    private String server;
    private String source;
    private String version;


}
