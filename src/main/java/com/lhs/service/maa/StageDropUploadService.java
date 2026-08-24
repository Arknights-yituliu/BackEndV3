package com.lhs.service.maa;

import com.lhs.common.util.*;

import com.lhs.entity.dto.drop.StageDropDTO;
import com.lhs.entity.dto.drop.StageDropDetailDTO;
import com.lhs.entity.po.material.StageDrop;

import com.lhs.mapper.material.StageDropMapper;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class StageDropUploadService {

    private final StageDropMapper stageDropMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final IdGenerator idGenerator;


    public StageDropUploadService(StageDropMapper stageDropMapper,
                                  RedisTemplate<String, Object> redisTemplate) {
        this.stageDropMapper = stageDropMapper;
        this.redisTemplate = redisTemplate;
        this.idGenerator = new IdGenerator(1L);
    }


    private static final long EXPIRATION_TIME = 24 * 60 * 60;

    public void saveStageDrop(HttpServletRequest httpServletRequest, StageDropDTO stageDropDTO) {


        Date date = new Date();
        String authorization = httpServletRequest.getHeader("authorization");
        if (authorization == null) {
            return;
        }


        if (stageDropDTO.getServer() == null || stageDropDTO.getSource() == null
                || stageDropDTO.getVersion() == null) {
            return;
        }


        String[] auth = authorization.split(" ");
        if (auth.length < 2) {
            return;
        }

        String penguinId = auth[1];

        if (penguinId.length() > 50) {
            Logger.info("MAA版本号 {} " + stageDropDTO.getVersion());
            return;
        }

        Boolean lock = redisTemplate.opsForValue().setIfAbsent("StageDropLimit:" + penguinId, date.getTime(), 5, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(lock)) {
            return;
        }


        if ("main_01-07".equals(stageDropDTO.getStageId())) {
            Long maxUploads = redisTemplate.opsForValue().increment("1-7_MAX_UPLOADS_PER_DAY");
            // 设置过期时间，为了防止错过，前10次请求设置请求过期时间
            if (maxUploads != null && maxUploads < 10) {
                redisTemplate.expire("1-7_MAX_UPLOADS_PER_DAY", EXPIRATION_TIME, TimeUnit.SECONDS);
            }

            // 检查是否超过限制，每24小时仅可上传50000次1-7，服务器塞满了放不下了
            if (maxUploads != null && maxUploads > 20000) {
                return;
            }
        }


        Long stageDropId = idGenerator.nextId();

        StageDrop stageDrop = new StageDrop();
        stageDrop.setId(stageDropId);
        stageDrop.setStageId(stageDropDTO.getStageId());
        stageDrop.setTimes(stageDropDTO.getTimes() == null ? 1 : stageDropDTO.getTimes());
        stageDrop.setServer(stageDropDTO.getServer());
        stageDrop.setSource(stageDropDTO.getSource());
        stageDrop.setUid(penguinId);
        stageDrop.setVersion(stageDropDTO.getVersion());
        stageDrop.setCreateTime(date);


        List<StageDropDetailDTO> drops = stageDropDTO.getDrops();

        if (drops != null && !drops.isEmpty()) {
            stageDrop.setDrops(JsonMapper.toJSONString(drops));
        } else {
            stageDrop.setDrops("[]");
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        String tableName = String.format("stage_drop_%d_%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);

       
        stageDropMapper.insertByTable(tableName, stageDrop);
    }





}
