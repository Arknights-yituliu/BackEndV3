package com.lhs.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("maa_recruit_data")
public class MaaRecruitData {

    @TableId
    private Long id;
    private String uid;  //企鹅物流id
    private String tag;  //tag组合，是个集合
    private Integer level;  //本次招募的最高星级
    private Date createTime;  //创建时间
    private String server;  //地区 一般为CN
    private String source; //来源
    private String version;  //maa版本号

    public void init(){
        long time = new Date().getTime();
        int random = (int) (Math.random() * 1000);
        this.id = time*1000+random;
    }

    public MaaRecruitData() {
    }

    public MaaRecruitData(String uid, String tag, Integer level, Date createTime, String server, String source, String version) {
        this.uid = uid;
        this.tag = tag;
        this.level = level;
        this.createTime = createTime;
        this.server = server;
        this.source = source;
        this.version = version;
    }
}
