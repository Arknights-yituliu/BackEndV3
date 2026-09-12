package com.lhs.service.user.impl;

import com.lhs.common.context.UserContext;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.common.util.RedisKeyUtil;
import com.lhs.entity.dto.user.OpenApiTokenDataDTO;
import com.lhs.entity.po.user.TokenRecord;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.user.TokenRecordMapper;
import com.lhs.service.user.OAuthUserService;
import com.lhs.service.user.OpenApiService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAPI Token 管理服务实现
 */
@Service
public class OpenApiServiceImpl implements OpenApiService {

    private final RedisTemplate<String, String> redisTemplate;
    private final OAuthUserService oAuthUserService;
    private final TokenRecordMapper tokenRecordMapper;
    private final IdGenerator idGenerator;

    public OpenApiServiceImpl(RedisTemplate<String, String> redisTemplate, OAuthUserService oAuthUserService,
                              TokenRecordMapper tokenRecordMapper) {
        this.redisTemplate = redisTemplate;
        this.oAuthUserService = oAuthUserService;
        this.tokenRecordMapper = tokenRecordMapper;
        this.idGenerator = new IdGenerator(1L);
    }

    @Override
    public String generateOpenApiToken( List<Integer> scopeCodes, String remark) {

        Long uid = UserContext.getUid();


        // 检查token数量是否已达上限（最多5个）
        Long tokenCount = tokenRecordMapper.selectCount(
                new LambdaQueryWrapper<TokenRecord>()
                        .eq(TokenRecord::getUid, uid)
                        .eq(TokenRecord::getType, "open-api"));
        if (tokenCount >= 5) {
            throw new ServiceException(ResultCode.OPEN_API_TOKEN_COUNT_EXCEEDED);
        }

        // 使用UUID生成唯一token
        String token = UUID.randomUUID().toString().replace("-", "");

        // 构造Redis存储数据：{"uid": uid, "scope": [10001, 10002], "createTime": 时间戳}，不设置过期时间，token 永不过期
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("uid", uid);
        tokenData.put("scope", scopeCodes);
        tokenData.put("createTime", System.currentTimeMillis());

        redisTemplate.opsForValue().set(RedisKeyUtil.openApiToken(token), JsonMapper.toJSONString(tokenData));

        // 将token写入数据库记录
        TokenRecord record = new TokenRecord();
        record.setId(idGenerator.nextId());
        record.setUid(uid);
        record.setToken(token);
        record.setType("open-api");
        record.setScope(JsonMapper.toJSONString(scopeCodes));
        record.setRemark(remark);
        record.setCreateTime(new Date());
        tokenRecordMapper.insert(record);

        Logger.info("为用户 {} 生成了 scope={} 的第三方API token", uid, scopeCodes);
        return token;
    }

    @Override
    public Long validateOpenApiToken(String token, int requiredCode) {
        if (token == null || token.isEmpty()) {
            throw new ServiceException(ResultCode.USER_TOKEN_FORMAT_ERROR_OR_USER_NOT_LOGIN);
        }

        // 从Redis读取并解析token数据
        String tokenDataJson = redisTemplate.opsForValue().get(RedisKeyUtil.openApiToken(token));
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
    public void deleteOpenApiToken( String token) {
        Long uid = UserContext.getUid();
        redisTemplate.delete(RedisKeyUtil.openApiToken(token));
        // 删除数据库中的token记录
        tokenRecordMapper.delete(new LambdaQueryWrapper<TokenRecord>()
                .eq(TokenRecord::getToken, token));
        Logger.info("用户 {} (uid={}) 删除了第三方API token", uid);
    }

    @Override
    public List<Map<String, Object>> listUserTokens() {

        Long uid = UserContext.getUid();

        // 从数据库查询该用户所有open-api类型的token
        List<TokenRecord> records = tokenRecordMapper.selectList(
                new LambdaQueryWrapper<TokenRecord>()
                        .eq(TokenRecord::getUid, uid)
                        .eq(TokenRecord::getType, "open-api")
                        .orderByDesc(TokenRecord::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (TokenRecord record : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("token", record.getToken());
            item.put("scope", record.getScope());
            item.put("remark", record.getRemark());
            item.put("createTime", record.getCreateTime());
            result.add(item);
        }
        return result;
    }
}
