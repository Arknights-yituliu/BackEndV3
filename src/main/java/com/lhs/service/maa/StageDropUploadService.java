package com.lhs.service.maa;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.*;

import com.lhs.entity.dto.item.StageDropDTO;
import com.lhs.entity.dto.item.StageDropDetailDTO;
import com.lhs.entity.po.material.StageDrop;
import com.lhs.entity.po.material.StageDropDetail;
import com.lhs.entity.vo.item.StageDropDetailVO;
import com.lhs.entity.vo.item.StageDropVO;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StageDropUploadService {

    private final StageDropMapper stageDropMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private final StageDropDetailMapper stageDropDetailMapper;

    private final StageDropDetailMapperService stageDropDetailMapperService;

    private final static Long DROP_UPLOAD_SERVICE_START_TIME_STAMP = 1709262000000L;


    private final OSSService ossService;

    private final IdGenerator idGenerator;


    public StageDropUploadService(StageDropMapper stageDropMapper,
                                  RedisTemplate<String, Object> redisTemplate,
                                  StageDropDetailMapper stageDropDetailMapper,
                                  StageDropDetailMapperService stageDropDetailMapperService,
                                  OSSService ossService) {
        this.stageDropMapper = stageDropMapper;
        this.redisTemplate = redisTemplate;
        this.stageDropDetailMapper = stageDropDetailMapper;
        this.stageDropDetailMapperService = stageDropDetailMapperService;
        this.ossService = ossService;
        this.idGenerator = new IdGenerator(1L);
    }

    public String saveStageDrop(HttpServletRequest httpServletRequest, StageDropDTO stageDropDTO) {


        if("main_01-07".equals(stageDropDTO.getStageId())){
            return "本次作战已成功上传";
        }

        long nowTimeStamp = System.currentTimeMillis();
        String authorization = httpServletRequest.getHeader("authorization");
        if (authorization == null) return "请求头未携带企鹅物流账号";

        List<StageDropDetailDTO> drops = stageDropDTO.getDrops();
        if (stageDropDTO.getServer() == null || stageDropDTO.getSource() == null
                || stageDropDTO.getVersion() == null) {
            return "掉落、版本、资源、服务信息为空";
        }


        String[] auth = authorization.split(" ");
        if (auth.length < 2) return "请求头未携带企鹅物流账号";

        String penguinId = auth[1];
        Boolean lock = redisTemplate.opsForValue().setIfAbsent(penguinId, nowTimeStamp, 7, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(lock)) {
            return "已保存";
        }



        JsonNode itemTable = getItemTable();
        Long stageDropId = idGenerator.nextId();
        StageDrop stageDrop = new StageDrop();
        stageDrop.setId(stageDropId);
        stageDrop.setStageId(stageDropDTO.getStageId());
        stageDrop.setTimes(stageDropDTO.getTimes() == null ? 1 : stageDropDTO.getTimes());
        stageDrop.setServer(stageDropDTO.getServer());
        stageDrop.setSource(stageDropDTO.getSource());
        stageDrop.setUid(penguinId);
        stageDrop.setVersion(stageDropDTO.getVersion());
        stageDrop.setCreateTime(nowTimeStamp);

        List<StageDropDetail> dropDetailList = new ArrayList<>();

        if (drops != null) {
            for (StageDropDetailDTO dropDTO : drops) {
                StageDropDetail dropDetail = new StageDropDetail();
                if (itemTable.get(dropDTO.getItemId()) == null) continue;
                dropDetail.setId(idGenerator.nextId());
                dropDetail.setUid(penguinId);
                dropDetail.setChildId(stageDropId);
                dropDetail.setItemId(dropDTO.getItemId());
                dropDetail.setQuantity(dropDTO.getQuantity());
                dropDetail.setDropType(dropDTO.getDropType());
                dropDetailList.add(dropDetail);
            }
        }

        try {
            stageDropMapper.insert(stageDrop);
            if (!dropDetailList.isEmpty()) {
                stageDropDetailMapperService.saveBatch(dropDetailList);
            }
        } catch (Exception e) {
            redisTemplate.delete(penguinId);
            throw new RuntimeException(e);
        }

        return "本次作战已成功上传";
    }

    @RedisCacheable(key = "ItemTable", timeout = 604800)
    private JsonNode getItemTable() {
        return JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.Item+"drop_table.json"));
//        return JsonMapper.parseJSONObject(FileUtil.read("src/main/resources/item/drop_table.json"));
    }



    public long getYesterdayMidnightTimeStamp(){
        // 获取现在的日期和时间
        LocalDateTime now = LocalDateTime.now();

        // 获取昨天的日期和时间（0点0分0秒）
        LocalDateTime yesterdayMidnight = now.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        // 获取系统默认时区
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");

        // 将LocalDateTime转换为ZonedDateTime
        ZonedDateTime zonedDateTime = yesterdayMidnight.atZone(zoneId);

        //导出数据起始时间戳（毫秒）
        return  zonedDateTime.toInstant().toEpochMilli();

    }

    public void exportData() {

        Date date = new Date();

        Object LastExportStageDropDataTimeStamp = redisTemplate.opsForValue().get("Item:LastExportStageDropDataTimeStamp");

        long startTimestamp = DROP_UPLOAD_SERVICE_START_TIME_STAMP;

        if(LastExportStageDropDataTimeStamp!=null){
            startTimestamp = Long.parseLong(String.valueOf(LastExportStageDropDataTimeStamp));
        }

        long currentTimeStamp = date.getTime();
        if(currentTimeStamp-startTimestamp< 60 * 60 * 2 * 1000){
            Logger.error("距离当前时间过短");
            redisTemplate.opsForValue().set("Item:LastExportStageDropDataTimeStamp",startTimestamp);
            return;
        }

        //导出数据结束时间戳（毫秒）,每次导出1小时的数据
        long endTimestamp = startTimestamp+ 60 * 60 * 1000;



        QueryWrapper<StageDrop> stageDropQueryWrapper = new QueryWrapper<>();
        stageDropQueryWrapper.ge("create_time",startTimestamp).le("create_time",endTimestamp);
        List<StageDrop> stageDropList = stageDropMapper.selectList(stageDropQueryWrapper);


        if(stageDropList.isEmpty()){
            redisTemplate.opsForValue().set("Item:LastExportStageDropDataTimeStamp",endTimestamp);
            Logger.error("无数据");
            return;
        }

        Long startChildId = stageDropList.get(0).getId();
        Long endChildId = stageDropList.get(stageDropList.size()-1).getId();

        QueryWrapper<StageDropDetail> stageDropDetailQueryWrapper = new QueryWrapper<>();
        stageDropDetailQueryWrapper.ge("child_id",startChildId).le("child_id",endChildId);
        List<StageDropDetail> stageDropDetailList = stageDropDetailMapper.selectList(stageDropDetailQueryWrapper);
        Map<Long, List<StageDropDetail>> stageDropDetailCollect = stageDropDetailList.stream().collect(Collectors.groupingBy(StageDropDetail::getChildId));

        List<StageDropVO> stageDropVOList = new ArrayList<>();
        for(StageDrop stageDrop:stageDropList){
            StageDropVO stageDropVO = new StageDropVO();
            stageDropVO.setId(stageDrop.getId());
            stageDropVO.setUid(stageDrop.getUid());
            stageDropVO.setStageId(stageDrop.getStageId());
            stageDropVO.setVersion(stageDrop.getVersion());
            stageDropVO.setTimes(stageDrop.getTimes());
            stageDropVO.setServer(stageDrop.getServer());
            stageDropVO.setCreateTime(stageDrop.getCreateTime());
            List<StageDropDetail> dropDetailListById = stageDropDetailCollect.get(stageDrop.getId());
            if(dropDetailListById!=null){
                stageDropVO.setDropList(getStageDropDetailVOList(dropDetailListById));
            }
            stageDropVOList.add(stageDropVO);
        }

        if(stageDropVOList.size()<1000){
            redisTemplate.opsForValue().set("Item:LastExportStageDropDataTimeStamp",endTimestamp);
            Logger.error("数据过少");
            return;
        }

        Logger.info("本次导出数据量为"+stageDropVOList.size());

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd/HH:mm");
        String dateText = simpleDateFormat.format(new Date(startTimestamp));
        String[] dateTextSplit = dateText.split("/");


        String year = dateTextSplit[0];
        String month = dateTextSplit[1];
        String day = dateTextSplit[2];
        String hourAndMinute = dateTextSplit[3];

        String path = "export2/"+year+"/"+month+"/"+day+"/stage_drop_"+hourAndMinute+".json";
        ossService.upload(JsonMapper.toJSONString(stageDropVOList),path);
        Logger.info("OSS上传路径为："+path);
        redisTemplate.opsForValue().set("Item:LastExportStageDropDataTimeStamp",endTimestamp);
    }

    private static List<StageDropDetailVO> getStageDropDetailVOList(List<StageDropDetail> dropDetailListById) {
        List<StageDropDetailVO> stageDropDetailVOList = new ArrayList<>();
        for(StageDropDetail stageDropDetail: dropDetailListById){
            StageDropDetailVO stageDropDetailVO = new StageDropDetailVO();
            stageDropDetailVO.setItemId(stageDropDetail.getItemId());
            stageDropDetailVO.setDropType(stageDropDetail.getDropType());
            stageDropDetailVO.setQuantity(stageDropDetail.getQuantity());
            stageDropDetailVOList.add(stageDropDetailVO);
        }
        return stageDropDetailVOList;
    }


}
