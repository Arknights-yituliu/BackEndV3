package com.lhs.service.util.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.vo.material.StorePermVO;
import com.lhs.service.util.DataCacheService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DataCacheServiceImpl implements DataCacheService {

    private final RedisTemplate<String,Object> redisTemplate;

    public DataCacheServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Map<String, Object> getStageCacheData(String key) {
        Object o = redisTemplate.opsForValue().get("RecommendedStage" + key);
        return JsonMapper.parseObject(String.valueOf(o), new TypeReference<>() {
        });
    }
}
