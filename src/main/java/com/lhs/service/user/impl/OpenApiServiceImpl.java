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
import com.lhs.service.user.OpenApiService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
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

    /**
     * 单用户可持有的 open-api token 数量上限
     */
    private static final long TOKEN_COUNT_LIMIT = 5L;

    private final RedisTemplate<String, String> redisTemplate;
    private final TokenRecordMapper tokenRecordMapper;
    private final IdGenerator idGenerator;

    public OpenApiServiceImpl(RedisTemplate<String, String> redisTemplate,
                              TokenRecordMapper tokenRecordMapper) {
        this.redisTemplate = redisTemplate;
        this.tokenRecordMapper = tokenRecordMapper;
        this.idGenerator = new IdGenerator(1L);
    }

    @Override
    public String generateOpenApiToken( List<Integer> scopeCodes, String remark) {

        Long uid = UserContext.getUid();


        // 检查token数量是否已达上限（最多5个）：token 永不过期，全部计入配额，仅能通过用户手动删除释放
        Long tokenCount = tokenRecordMapper.selectCount(
                new LambdaQueryWrapper<TokenRecord>()
                        .eq(TokenRecord::getUid, uid)
                        .eq(TokenRecord::getType, "open-api"));
        if (tokenCount >= TOKEN_COUNT_LIMIT) {
            throw new ServiceException(ResultCode.OPEN_API_TOKEN_COUNT_EXCEEDED);
        }

        // 使用UUID生成唯一token
        String token = UUID.randomUUID().toString().replace("-", "");

        // 构造Redis存储数据：{"uid": uid, "scope": [10001, 10002], "createTime": 时间戳}，不设置过期时间
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

        String redisKey = RedisKeyUtil.openApiToken(token);

        // 从Redis读取并解析token数据
        String tokenDataJson = redisTemplate.opsForValue().get(redisKey);
        OpenApiTokenDataDTO tokenData = tokenDataJson == null
                ? null
                : JsonMapper.parseObject(tokenDataJson, OpenApiTokenDataDTO.class);

        // 兜底：Redis 未命中（重启丢数据、被误删、或早期带 TTL 的存量 token 已过期）时，
        // 以数据库记录为准重建缓存并继续使用，保证 token 只要没被用户手动删除就一直有效
        if (tokenData == null) {
            tokenData = rebuildCacheFromDb(token, redisKey);
        }

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
        // 先删数据库：数据库是 token 的权威来源，同时以 uid 限定归属，防止越权删除他人 token
        int deleted = tokenRecordMapper.delete(new LambdaQueryWrapper<TokenRecord>()
                .eq(TokenRecord::getUid, uid)
                .eq(TokenRecord::getToken, token)
                .eq(TokenRecord::getType, "open-api"));
        if (deleted == 0) {
            // 记录不存在或不属于当前用户：不触碰 Redis，避免误删他人 token 的缓存
            throw new ServiceException(ResultCode.USER_INSUFFICIENT_PERMISSIONS);
        }
        // 数据库删除成功后再清理缓存，避免数据库删除失败时 Redis 已清空导致状态不一致
        redisTemplate.delete(RedisKeyUtil.openApiToken(token));
        Logger.info("用户 {} (uid={}) 删除了第三方API token", uid);
    }

    @Override
    public List<Map<String, Object>> listUserTokens() {

        Long uid = UserContext.getUid();

        // 从数据库查询该用户所有open-api类型token，token 永不过期故不再按创建时间过滤
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

    /**
     * Redis 缓存缺失时，以数据库记录为权威重建 token 缓存
     * <p>
     * 触发场景：Redis 重启丢数据、key 被误删、或改造前签发的带 TTL 的存量 token 已自然过期。
     * 若数据库同样查不到记录，说明 token 已被用户手动删除或从未签发过，返回 null 交由调用方判定。
     *
     * @param token    待恢复的 token 字符串
     * @param redisKey 对应的 Redis key
     * @return 重建后的 token 数据，数据库中不存在时返回 null
     */
    private OpenApiTokenDataDTO rebuildCacheFromDb(String token, String redisKey) {
        TokenRecord record = tokenRecordMapper.selectOne(new LambdaQueryWrapper<TokenRecord>()
                .eq(TokenRecord::getToken, token)
                .eq(TokenRecord::getType, "open-api")
                .last("limit 1"));
        if (record == null) {
            return null;
        }

        // scope 在数据库中以 JSON 字符串存储，需还原为整形列表后才能写回缓存与做权限判定
        List<Integer> scope = JsonMapper.parseJSONArray(record.getScope(), new TypeReference<List<Integer>>() {
        });
        if (scope == null) {
            // 存量数据 scope 格式异常，无法判定权限，按无效 token 处理
            Logger.warn("第三方API token 数据库记录 scope 解析失败，已跳过缓存重建 (uid={})", record.getUid());
            return null;
        }

        OpenApiTokenDataDTO tokenData = new OpenApiTokenDataDTO();
        tokenData.setUid(record.getUid());
        tokenData.setScope(scope);
        tokenData.setCreateTime(record.getCreateTime().getTime());

        // 不设置过期时间：token 永不过期，仅用户手动删除才失效
        redisTemplate.opsForValue().set(redisKey, JsonMapper.toJSONString(tokenData));
        Logger.info("第三方API token 缓存缺失，已依据数据库记录重建 (uid={})", record.getUid());
        return tokenData;
    }
}
