package com.lhs.service.maa;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;

import com.lhs.common.util.Logger;
import com.lhs.entity.dto.item.StageDropDTO;
import com.lhs.entity.dto.item.StageDropDetailDTO;
import com.lhs.entity.po.item.StageDrop;
import com.lhs.entity.po.item.StageDropDetail;
import com.lhs.entity.vo.item.StageDropDetailVO;
import com.lhs.entity.vo.item.StageDropVO;
import com.lhs.mapper.item.StageDropDetailMapper;
import com.lhs.mapper.item.StageDropMapper;
import com.lhs.mapper.item.service.StageDropDetailMapperService;
import com.lhs.service.util.OSSService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MAAUploadService {

    private final StageDropMapper stageDropMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private final StageDropDetailMapper stageDropDetailMapper;

    private final StageDropDetailMapperService stageDropDetailMapperService;

    private final OSSService ossService;

    private final IdGenerator idGenerator;

    public MAAUploadService(StageDropMapper stageDropMapper,
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
        Long stage_drop_id = idGenerator.nextId();
        StageDrop stageDrop = new StageDrop();
        stageDrop.setId(stage_drop_id);
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
                dropDetail.setChildId(stage_drop_id);
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
//        return JsonMapper.parseJSONObject(FileUtil.read("/backend/resources/drop_table.json"));
        return JsonMapper.parseJSONObject(FileUtil.read("src/main/resources/item/drop_table.json"));
    }


    @Scheduled(cron = "0 0/10 * * * ?")
    public void exportData() {

        Object exportStart = redisTemplate.opsForValue().get("ExportStart");
        if (exportStart == null) exportStart = "0";
        long startId = Long.parseLong(String.valueOf(exportStart));
        long endId = startId + 50000;
        QueryWrapper<StageDrop> queryWrapper = new QueryWrapper<>();
        queryWrapper .ge("id", startId)
                .lt("id", endId);
        List<StageDrop> stageDropList = stageDropMapper.selectList(queryWrapper);

        if (stageDropList == null) return;

        if (stageDropList.size() < 30000) return;

        Long startParentId = stageDropList.get(0).getId();
        Long endParentId = stageDropList.get(stageDropList.size() - 1).getId();
        List<StageDropDetail> dropDetailList = stageDropDetailMapper.selectList(new QueryWrapper<StageDropDetail>()
                .ge("child_id", startParentId)
                .le("child_id", endParentId));

        if (dropDetailList == null) return;

        Map<Long, List<StageDropDetail>> collect = dropDetailList.stream()
                .collect(Collectors.groupingBy(StageDropDetail::getChildId));

        List<StageDropVO> stageDropVOList = new ArrayList<>();

        stageDropList.forEach(e -> {
            StageDropVO stageDropVO = new StageDropVO();
            stageDropVO.setId(e.getId());
            stageDropVO.setTimes(e.getTimes());
            stageDropVO.setStageId(e.getStageId());
            stageDropVO.setCreateTime(e.getCreateTime());
            stageDropVO.setVersion(e.getVersion());
            stageDropVO.setUid(e.getUid());
            List<StageDropDetailVO> list = new ArrayList<>();
            List<StageDropDetail> dropDetailListById = collect.get(e.getId());
            if (dropDetailListById == null) {
                stageDropVO.setDropList(null);
            } else {
                dropDetailListById.forEach(d -> {
                    StageDropDetailVO dropVO = new StageDropDetailVO();
                    dropVO.copyByDropDetail(d);
                    list.add(dropVO);
                });
                stageDropVO.setDropList(list);
            }
            stageDropVOList.add(stageDropVO);
        });

        Long fileIndex = redisTemplate.opsForValue().increment("FileIndex");

        redisTemplate.opsForValue().set("ExportStart", endParentId+1);

        String path = "export/stage_drop" + fileIndex + ".json";

        String string = JsonMapper.toJSONString(stageDropVOList);

        ossService.upload(string, path);
        Logger.info("导出文件路径为：" + path);

        List<String> menu = new ArrayList<>();
        menu.add("https://public.yituliu.site/" + path);
    }


}
