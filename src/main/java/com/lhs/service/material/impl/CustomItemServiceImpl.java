package com.lhs.service.material.impl;

import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.service.material.ItemService;
import com.lhs.service.material.StageCalService;
import com.lhs.service.material.StageService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CustomItemServiceImpl {

    private final StageCalService stageCalService;

    private final ItemService itemService;

    private final StageService stageService;

    private final RedisTemplate<String, Object> redisTemplate;

    public CustomItemServiceImpl(StageCalService stageCalService, ItemService itemService, StageService stageService, RedisTemplate<String, Object> redisTemplate) {
        this.stageCalService = stageCalService;
        this.itemService = itemService;
        this.stageService = stageService;
        this.redisTemplate = redisTemplate;
    }

    public void CustomItemValue(StageConfigDTO stageConfigDTO) {
    }

}
