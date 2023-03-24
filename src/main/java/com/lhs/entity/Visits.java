package com.lhs.entity;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;

import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@TableName("visits")
@Slf4j
public class Visits {


    @TableId
    private String date;

    private Integer visits;
    private Integer visitsBot;
    private Integer visitsIndex;
    private Integer visitsSchedule;
    private Integer visitsGacha;
    private Integer visitsPack;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public void init() {
        Date date = new Date();
        this.date = new SimpleDateFormat("yyyy-MM-dd").format(date);
        this.createTime = date;
        this.visits = 1;
        this.visitsBot = 0;
        this.visitsIndex = 0;
        this.visitsSchedule = 0;
        this.visitsGacha = 0;
        this.visitsPack = 0;
    }

    public void update(String path) {
        this.visits += 1;
        switch (path) {
            case "index":
                this.visitsIndex += 1;
                break;
            case "bot":
                this.visitsBot += 1;
                break;
            case "building":
                this.visitsSchedule += 1;
                break;
            case "gacha":
                this.visitsGacha += 1;
                break;
            case "pack":
                this.visitsPack += 1;
                break;
            default:
                log.error("非法路径");
                break;
        }
    }


}
