package com.lhs.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SurveyUser {
    private Long id;
    private String userName;
    private Date createTime;
    private Date updateTime;
    private String ip;
    private Integer status;
    private String charTable;
}
