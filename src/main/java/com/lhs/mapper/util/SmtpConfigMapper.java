package com.lhs.mapper.util;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.util.SmtpConfig;
import org.springframework.stereotype.Repository;

/**
 * SMTP 邮件渠道配置 Mapper
 */
@Repository
public interface SmtpConfigMapper extends BaseMapper<SmtpConfig> {
}
