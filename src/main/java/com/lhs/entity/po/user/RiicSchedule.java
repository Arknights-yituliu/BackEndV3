package com.lhs.entity.po.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 基建排班表实体（riic_schedule 表，RIIC 排班）
 * <p>
 * 排班表内容为前端传入的 JSON，作为文本整体落库；
 * 每个用户最多保存 10 条，可按 id 覆盖（仅限本人）
 */
@Data
@TableName("riic_schedule")
public class RiicSchedule {

    /** 主键（雪花ID） */
    @TableId
    private Long id;

    /** 所属用户 uid */
    private Long uid;

    /** 排班表 JSON 文本 */
    private String schedule;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 最近更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
