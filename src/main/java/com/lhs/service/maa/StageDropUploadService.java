package com.lhs.service.maa;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.enums.TimeGranularity;
import com.lhs.common.util.*;

import com.lhs.entity.dto.material.StageDropDTO;
import com.lhs.entity.dto.material.StageDropDetailDTO;
import com.lhs.entity.po.material.StageDrop;
import com.lhs.entity.po.material.StageDropDetail;
import com.lhs.entity.po.material.StageDropStatistics;
import com.lhs.entity.po.material.StageDropV2;
import com.lhs.entity.vo.material.StageDropDetailVO;
import com.lhs.entity.vo.material.StageDropVO;
import com.lhs.mapper.material.StageDropDetailMapper;
import com.lhs.mapper.material.StageDropMapper;
import com.lhs.mapper.material.service.StageDropDetailMapperService;
import com.lhs.service.util.OSSService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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


    private static final long EXPIRATION_TIME = 12 * 60 * 60;

    public String saveStageDrop(HttpServletRequest httpServletRequest, StageDropDTO stageDropDTO) {




        Date date = new Date();
        String authorization = httpServletRequest.getHeader("authorization");
        if (authorization == null) {
            return "请求头未携带企鹅物流账号";
        }


        if (stageDropDTO.getServer() == null || stageDropDTO.getSource() == null
                || stageDropDTO.getVersion() == null) {
            return "掉落、版本、资源、服务信息为空";
        }


        String[] auth = authorization.split(" ");
        if (auth.length < 2) {
            return "请求头未携带企鹅物流账号";
        }

        String penguinId = auth[1];

        if (penguinId.length() > 50) {
            LogUtils.info("MAA版本号 {} " + stageDropDTO.getVersion());
            return "凭证异常";
        }

        Boolean lock = redisTemplate.opsForValue().setIfAbsent(penguinId, date.getTime(), 5, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(lock)) {
            return "请勿重复上传";
        }


        if ("main_01-07".equals(stageDropDTO.getStageId())) {
            Long maxUploads = redisTemplate.opsForValue().increment("1-7_MAX_UPLOADS_PER_DAY");
            // 设置过期时间，为了防止错过，前10次请求设置请求过期时间
            if (maxUploads != null && maxUploads <10) {
                redisTemplate.expire("1-7_MAX_UPLOADS_PER_DAY", EXPIRATION_TIME, TimeUnit.SECONDS);
            }

            // 检查是否超过限制，每12小时仅可上传1000次1-7，服务器塞满了放不下了
            if (maxUploads != null && maxUploads > 1000) {
                return "本次作战已成功上传";
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

        if (drops != null&&!drops.isEmpty()) {
            stageDrop.setDrops(JsonMapper.toJSONString(drops));
        } else {
            stageDrop.setDrops("[]");
        }

        stageDropMapper.insert(stageDrop);
        return "本次作战已成功上传";
    }

    public void collectHourlyDropData(){
        Date start = getCurrentHourTime();
        long hour = 60*60*1000L;
        Date end = new Date(start.getTime() + hour);
        List<StageDrop> stageDropList = stageDropMapper.listStageDropByDate(start, end);
        HashMap<String, Integer> timesMap = new HashMap<>();
        HashMap<String, StageDropStatistics> dropQuantity = new HashMap<>();
        Date date = new Date();
        for (StageDrop stageDrop : stageDropList) {

            if (!"CN".equals(stageDrop.getServer())) {
                continue;
            }

            String stageId = stageDrop.getStageId();
            timesMap.merge(stageId, 1, Integer::sum);

            if (stageDrop.getDrops()!=null&&stageDrop.getDrops().length() > 5) {
                List<StageDropDetailDTO> stageDropDetailDTOList = JsonMapper.parseJSONArray(stageDrop.getDrops(),
                        new TypeReference<>() {
                        });
                for (StageDropDetailDTO dropDetail : stageDropDetailDTOList) {
                    String itemId = dropDetail.getItemId();
                    String key = stageId + "&" + itemId;
                    Integer quantity = dropDetail.getQuantity();
                    if (dropQuantity.get(key) != null) {
                        dropQuantity.get(key).addQuantity(quantity);
                    } else {
                        StageDropStatistics stageDropStatistics = new StageDropStatistics();

                        stageDropStatistics.setStageId(stageId);
                        stageDropStatistics.setItemId(itemId);
                        stageDropStatistics.setQuantity(quantity);
                        stageDropStatistics.setStart(start);
                        stageDropStatistics.setEnd(end);
                        stageDropStatistics.setTimeGranularity(TimeGranularity.HOUR.code());
                        stageDropStatistics.setCreateTime(date);
                        dropQuantity.put(key, stageDropStatistics);
                    }
                }
            }
        }

        List<StageDropStatistics> insertList = new ArrayList<>();
        for (String key : dropQuantity.keySet()) {
            StageDropStatistics stageDropStatistics = dropQuantity.get(key);
            stageDropStatistics.setId(idGenerator.nextId());
            String stageId = stageDropStatistics.getStageId();
            stageDropStatistics.setTimes(timesMap.get(stageId));
            insertList.add(stageDropStatistics);
        }

        stageDropMapper.insertBatchStageDropStatistics(insertList);
    }


    /**
     *获取当前小时的整点，例如当前时间为16时多一点，获取16:00:00
     */
    private static Date getCurrentHourTime() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        LocalDateTime startOfHour = now.withMinute(0).withSecond(0).withNano(0);
        return Date.from(startOfHour.atZone(ZoneId.systemDefault()).toInstant());
    }






}
