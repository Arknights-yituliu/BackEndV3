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
import java.util.concurrent.TimeUnit;

/**
 * OpenAPI Token 管理服务实现
 */
@Service
public class OpenApiServiceImpl implements OpenApiService {

    /**
     * OpenAPI token 有效期（天），Redis 与数据库记录均以此为准判定过期
     */
    private static final long TOKEN_EXPIRE_DAYS = 180L;

    /**
     * 单用户可持有的有效 open-api token 数量上限
     */
    private static final long TOKEN_COUNT_LIMIT = 5L;

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


        // 检查token数量是否已达上限（最多5个），仅统计未过期的记录，避免过期token永久占用配额
        Long tokenCount = tokenRecordMapper.selectCount(
                new LambdaQueryWrapper<TokenRecord>()
                        .eq(TokenRecord::getUid, uid)
                        .eq(TokenRecord::getType, "open-api")
                        .gt(TokenRecord::getCreateTime, tokenExpireThreshold()));
        if (tokenCount >= TOKEN_COUNT_LIMIT) {
            throw new ServiceException(ResultCode.OPEN_API_TOKEN_COUNT_EXCEEDED);
        }

        // 使用UUID生成唯一token
        String token = UUID.randomUUID().toString().replace("-", "");

        // 构造Redis存储数据：{"uid": uid, "scope": [10001, 10002], "createTime": 时间戳}，有效期 180 天
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("uid", uid);
        tokenData.put("scope", scopeCodes);
        tokenData.put("createTime", System.currentTimeMillis());

        redisTemplate.opsForValue().set(RedisKeyUtil.openApiToken(token), JsonMapper.toJSONString(tokenData),
                TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);

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

        String redisKey = RedisKeyUtil.openApiToken(token);

        // 从Redis读取并解析token数据
        String tokenDataJson = redisTemplate.opsForValue().get(redisKey);
        if (tokenDataJson == null) {
            // Redis未命中：可能已被TTL自动清除，若数据库仍有残留记录则一并清理并明确提示已过期
            if (removeTokenRecord(token) > 0) {
                Logger.info("第三方API token 在Redis中已失效，同步清理数据库残留记录");
                throw new ServiceException(ResultCode.OPEN_API_TOKEN_EXPIRED);
            }
            throw new ServiceException(ResultCode.USER_TOKEN_FORMAT_ERROR_OR_USER_NOT_LOGIN);
        }

        OpenApiTokenDataDTO tokenData = JsonMapper.parseObject(tokenDataJson, OpenApiTokenDataDTO.class);
        if (tokenData == null || tokenData.getUid() == null || tokenData.getScope() == null) {
            throw new ServiceException(ResultCode.USER_TOKEN_FORMAT_ERROR_OR_USER_NOT_LOGIN);
        }

        // 有效期兜底校验：兼容存量未设置TTL的token，按createTime判定是否已超期
        if (isTokenExpired(tokenData.getCreateTime())) {
            redisTemplate.delete(redisKey);
            removeTokenRecord(token);
            Logger.info("第三方API token 已超过 {} 天有效期，已自动清理 (uid={})",
                    TOKEN_EXPIRE_DAYS, tokenData.getUid());
            throw new ServiceException(ResultCode.OPEN_API_TOKEN_EXPIRED);
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

        // 从数据库查询该用户所有未过期的open-api类型token，已过期记录不再返回
        List<TokenRecord> records = tokenRecordMapper.selectList(
                new LambdaQueryWrapper<TokenRecord>()
                        .eq(TokenRecord::getUid, uid)
                        .eq(TokenRecord::getType, "open-api")
                        .gt(TokenRecord::getCreateTime, tokenExpireThreshold())
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

    /**
     * 计算 token 过期判定的时间下限：创建时间早于该时刻的记录视为已过期
     *
     * @return 过期阈值时间点
     */
    private static Date tokenExpireThreshold() {
        return new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(TOKEN_EXPIRE_DAYS));
    }

    /**
     * 判断 token 是否已超过有效期
     *
     * @param createTime token 创建时间戳（毫秒），为空时按已过期处理
     * @return 是否已过期
     */
    private static boolean isTokenExpired(Long createTime) {
        if (createTime == null) {
            return true;
        }
        return createTime < tokenExpireThreshold().getTime();
    }

    /**
     * 删除数据库中指定 token 的 open-api 记录，用于过期 token 的惰性清理
     *
     * @param token token 字符串
     * @return 实际删除的记录条数
     */
    private int removeTokenRecord(String token) {
        return tokenRecordMapper.delete(new LambdaQueryWrapper<TokenRecord>()
                .eq(TokenRecord::getToken, token)
                .eq(TokenRecord::getType, "open-api"));
    }
}
