package com.lhs.entity.po.util;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * SMTP 邮件渠道配置实体
 * 对应数据库表 smtp_config
 */
@Data
@TableName("smtp_config")
public class SmtpConfig {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 渠道标识，如 mail163 / mail-qq */
    private String accountKey;
    /** SMTP 服务器地址 */
    private String host;
    /** SMTP 端口 */
    private Integer port;
    /** 登录账号（发件人邮箱） */
    private String username;
    /** SMTP 授权码 */
    private String password;
    /** 协议，默认 smtp */
    private String protocol;
    /** 默认编码，默认 UTF-8 */
    private String defaultEncoding;
    /** 是否启用 SSL */
    private Boolean sslEnable;
    /** 是否启用该渠道 */
    private Boolean enabled;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
