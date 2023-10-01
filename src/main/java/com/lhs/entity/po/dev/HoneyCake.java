package com.lhs.entity.po.dev;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("honey_cake")
public class HoneyCake {
    @TableId
    private String name;
    private Integer originium;
    private Integer orundum;
    private Integer permit;
    private Integer permit10;
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss",timezone = "GMT+8")
    private Date start;
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss",timezone = "GMT+8")
    private Date end;
    private String rewardType;
    private String module;
}
