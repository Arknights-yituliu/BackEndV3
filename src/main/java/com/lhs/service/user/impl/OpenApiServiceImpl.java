package com.lhs.service.user.impl;

import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.entity.dto.user.OpenApiTokenDataDTO;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.service.user.OpenApiService;
import com.lhs.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * OpenAPI Token 管理服务实现
 */
@Service
public class OpenApiServiceImpl implements OpenApiService {

    private final RedisTemplate<String, String> redisTemplate;
    private final UserService userService;

    public OpenApiServiceImpl(RedisTemplate<String, String> redisTemplate, UserService userService) {
        this.redisTemplate = redisTemplate;
        this.userService = userService;
    }

    @Override
    public String generateOpenApiToken(HttpServletRequest httpServletRequest, List<Integer> scopeCodes) {
        UserInfoVO userInfoVO = userService.getUserInfoVOByHttpServletRequest(httpServletRequest);
        Long uid = userInfoVO.getUid();

        // 使用UUID生成唯一token
        String token = UUID.randomUUID().toString().replace("-", "");

        // 构造Redis存储数据：{"uid": uid, "scope": [10001, 10002]}
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("uid", uid);
        tokenData.put("scope", scopeCodes);

        redisTemplate.opsForValue().set("open-api-token:" + token, JsonMapper.toJSONString(tokenData), 30, TimeUnit.DAYS);

        Logger.info("为用户 {} 生成了 scope={} 的第三方API token", uid, scopeCodes);
        return token;
    }

    @Override
    public Long validateOpenApiToken(String token, int requiredCode) {
        if (token == null || token.isEmpty()) {
            throw new ServiceException(ResultCode.USER_TOKEN_FORMAT_ERROR_OR_USER_NOT_LOGIN);
        }

        // 从Redis读取并解析token数据
        String tokenDataJson = redisTemplate.opsForValue().get("open-api-token:" + token);
        if (tokenDataJson == null) {
            throw new ServiceException(ResultCode.USER_TOKEN_FORMAT_ERROR_OR_USER_NOT_LOGIN);
        }

        OpenApiTokenDataDTO tokenData = JsonMapper.parseObject(tokenDataJson, OpenApiTokenDataDTO.class);
        if (tokenData == null || tokenData.getUid() == null || tokenData.getScope() == null) {
            throw new ServiceException(ResultCode.USER_TOKEN_FORMAT_ERROR_OR_USER_NOT_LOGIN);
        }

        // 权限校验：检查用户 scope 列表是否包含所需权限 code
        if (!tokenData.getScope().contains(requiredCode)) {
            throw new ServiceException(ResultCode.USER_INSUFFICIENT_PERMISSIONS);
        }

        return tokenData.getUid();
    }

    @Override
    public void deleteOpenApiToken(HttpServletRequest httpServletRequest) {
        UserInfoVO userInfoVO = userService.getUserInfoVOByHttpServletRequest(httpServletRequest);
        String token = httpServletRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Authorization") && token.length() > 30) {
            token = token.replace("Authorization", "");
        }
        redisTemplate.delete("open-api-token:" + token);
        Logger.info("用户 {} (uid={}) 删除了第三方API token", userInfoVO.getUserName(), userInfoVO.getUid());
    }
}
