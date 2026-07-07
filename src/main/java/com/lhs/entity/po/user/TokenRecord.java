package com.lhs.entity.po.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Token记录表，记录登录token和第三方API token
 */
@Data
@TableName("token_record")
public class TokenRecord {

    @TableId
    private Long id;

    /**
     * 用户ID
     */
    private Long uid;

    /**
     * token字符串
     */
    private String token;

    /**
     * token类型：login / open-api
     */
    private String type;

    /**
     * 权限（JSON字符串，如 "[10001,10002]"）
     */
    private String scope;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;
}