package com.lhs.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.user.OAuthUserInfo;
import org.springframework.stereotype.Repository;

/**
 * OAuth2 用户资料缓存表 Mapper
 */
@Repository
public interface OAuthUserInfoMapper extends BaseMapper<OAuthUserInfo> {
}
