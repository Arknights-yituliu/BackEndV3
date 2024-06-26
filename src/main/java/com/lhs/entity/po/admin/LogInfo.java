package com.lhs.entity.po.admin;

import lombok.Data;

import java.util.Date;

@Data
public class LogInfo {

    private Long id;
    private String message;
    private String logType;
    private String apiPath;
    private Date createTime;

}
