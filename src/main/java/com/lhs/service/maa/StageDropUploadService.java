package com.lhs.service.maa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.enums.TimeGranularity;
import com.lhs.common.util.*;

import com.lhs.entity.dto.material.StageDropCollect;
import com.lhs.entity.dto.material.StageDropDTO;
import com.lhs.entity.dto.material.StageDropDetailDTO;
import com.lhs.entity.po.material.StageDrop;
import com.lhs.entity.po.material.StageDropStatistics;

import com.lhs.mapper.material.StageDropMapper;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
            LogUtils.info("MAA版本号 {} " + stageDropDTO.getVersion());
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

        stageDropMapper.insert(stageDrop);
    }

    public void stageDropHourlyStatistics() {
        Date date = getCurrentHourTime();
        long hour = 60 * 60 * 1000L;
        long start = 1742400000000L;

        for (int i = 0; i < 10000; i++) {
            Date startTime = new Date(start);
            Date endTime = new Date(start + hour);
            List<StageDrop> stageDropList = stageDropMapper.listStageDropByDate(startTime, endTime);
            start+=hour;
            if(stageDropList.isEmpty()){
                continue;
            }

            Map<String, StageDropCollect> dropCollectHashMap = new HashMap<>();
            for (StageDrop stageDrop : stageDropList) {
                String stageId = stageDrop.getStageId();
                String server = stageDrop.getServer();
                if (!"CN".equals(server)) {
                    continue;
                }
                Integer times = stageDrop.getTimes();
                if (times > 1) {
                    continue;
                }

                StageDropCollect item = dropCollectHashMap.get(stageId);

                if (item == null) {
                    item = new StageDropCollect();
                    dropCollectHashMap.put(stageId, item); // 将新对象放入 map
                }

                item.addTimes(1);
                String drops = stageDrop.getDrops();
                if (drops != null && drops.length() > 5) {
                    List<StageDropDetailDTO> stageDropDetailDTOList = JsonMapper.parseJSONArray(drops, new TypeReference<>() {
                    });
                    for (StageDropDetailDTO dropDetail : stageDropDetailDTOList) {
                        Integer quantity = dropDetail.getQuantity();
                        String itemId = dropDetail.getItemId();
                        item.getDrops().merge(itemId, quantity, Integer::sum);
                    }
                }
            }

            List<StageDropStatistics> stageDropStatisticsList = new ArrayList<>();
            for(String stageId: dropCollectHashMap.keySet()){
                StageDropCollect stageDropCollect = dropCollectHashMap.get(stageId);
                Integer times = stageDropCollect.getTimes();
                Map<String, Integer> drops = stageDropCollect.getDrops();
                for(String itemId:drops.keySet()){
                    Integer quantity = drops.get(itemId);
                    StageDropStatistics stageDropStatistics = new StageDropStatistics();
                    stageDropStatistics.setId(idGenerator.nextId());
                    stageDropStatistics.setTimes(times);
                    stageDropStatistics.setStageId(stageId);
                    stageDropStatistics.setItemId(itemId);
                    stageDropStatistics.setQuantity(quantity);
                    stageDropStatistics.setStart(startTime);
                    stageDropStatistics.setEnd(endTime);
                    stageDropStatistics.setTimeGranularity(TimeGranularity.HOUR.code());
                    stageDropStatistics.setCreateTime(date);
                    stageDropStatisticsList.add(stageDropStatistics);
                }
            }

            System.out.println("开始插入");
            System.out.println(stageDropStatisticsList.size());


            stageDropMapper.insertBatchStageDropStatistics(stageDropStatisticsList);
        }
    }


    public void collectHourlyDropData() {
        Date start = getCurrentHourTime();
        long hour = 60 * 60 * 1000L;
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

            if (stageDrop.getDrops() != null && stageDrop.getDrops().length() > 5) {
                List<StageDropDetailDTO> stageDropDetailDTOList = JsonMapper.parseJSONArray(stageDrop.getDrops(), new TypeReference<>() {
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

    public void stageDropDataMigration(){

        long hour = 60 * 60 * 1000L;
        long start = 1740758400000L;

        for (int i = 0; i < 10000; i++) {
            Date startTime = new Date(start);
            Date endTime = new Date(start + hour);
            List<StageDrop> stageDropList = stageDropMapper.listOldStageDropByDate("stage_drop_20250303_20250322", startTime, endTime);
            System.out.println(startTime);
            start = start+hour;
            if(stageDropList.isEmpty()){
                continue;
            }
            stageDropMapper.insertBatch(stageDropList);

        }


    }


    /**
     * 获取当前小时的整点，例如当前时间为16时多一点，获取16:00:00
     */
    private static Date getCurrentHourTime() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        LocalDateTime startOfHour = now.withMinute(0).withSecond(0).withNano(0);
        return Date.from(startOfHour.atZone(ZoneId.systemDefault()).toInstant());
    }


}
