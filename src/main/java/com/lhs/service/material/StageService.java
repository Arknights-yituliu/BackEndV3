package com.lhs.service.material;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.enums.StageType;
import com.lhs.common.util.*;
import com.lhs.entity.po.material.Stage;

import com.lhs.entity.vo.material.ZoneTableVO;
import com.lhs.mapper.material.StageMapper;

import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StageService {


    private final StageMapper stageMapper;

    public StageService(StageMapper stageMapper) {
        this.stageMapper = stageMapper;
    }


    public List<Stage> getStageList(QueryWrapper<Stage> queryWrapper) {
        return stageMapper.selectList(queryWrapper);
    }

    public List<Map<String,Object>> getStageInfo(){
           List<Map<String,Object>> list = new ArrayList<>();
           List<Stage> stageList = stageMapper.selectList(null);
           for(Stage item : stageList){
               Map<String,Object> map = new HashMap<>();
               map.put("stageId",item.getStageId());
               map.put("stageCode",item.getStageCode());
               map.put("zoneId",item.getZoneId());
               map.put("zoneName",item.getZoneName());
               map.put("apCost",item.getApCost());
               map.put("type",item.getStageType());
               map.put("start",item.getStartTime().getTime());
               map.put("end",item.getEndTime().getTime());
               list.add(map);
           }
           return list;
    }

    public Map<String, Stage> getStageMapKeyIsStageId() {
        QueryWrapper<Stage> stageNewQueryWrapper = new QueryWrapper<>();
        stageNewQueryWrapper.notLike("stage_id", "tough");
        List<Stage> stageList = stageMapper.selectList(stageNewQueryWrapper);
        return stageList.stream().collect(Collectors.toMap(Stage::getStageId, Function.identity()));
    }


    public Map<String, List<Stage>> getStageMapGroupByZone() {
        QueryWrapper<Stage> stageNewQueryWrapper = new QueryWrapper<>();
        stageNewQueryWrapper.notLike("stage_id", "tough")
                .orderByDesc("stage_id");
        List<Stage> stageList = stageMapper.selectList(stageNewQueryWrapper);
        return stageList.stream().collect(Collectors.groupingBy(Stage::getZoneName));
    }

    public Map<String, List<ZoneTableVO>> getZoneTable() {

        List<Stage> stageList = stageMapper.selectList(new QueryWrapper<Stage>().notLike("stage_id", "tough"));
        Map<String, List<Stage>> stageListMap = stageList.stream().collect(Collectors.groupingBy(Stage::getZoneId));

        List<ZoneTableVO> zoneTableVOList = new ArrayList<>();
        for (String zoneId : stageListMap.keySet()) {
            ZoneTableVO zoneTableVo = new ZoneTableVO();
            zoneTableVo.setZoneId(zoneId);
            List<Stage> stageListByZone = stageListMap.get(zoneId);
            Stage stage = stageListByZone.get(0);
            zoneTableVo.setZoneName(stage.getZoneName());
            zoneTableVo.setStageType(stage.getStageType());
            zoneTableVo.setStageList(stageListByZone);
            zoneTableVo.setEndTime(stage.getEndTime());
            zoneTableVOList.add(zoneTableVo);
        }


        Map<String, List<ZoneTableVO>> zoneMap = zoneTableVOList.stream().collect(Collectors.groupingBy(ZoneTableVO::getStageType));
        zoneMap.forEach((k,v)->v.sort(Comparator.comparing(ZoneTableVO::getEndTime).reversed()));

        return zoneMap;
    }



    public HashMap<Object, Object> updateStageList(List<Stage> stageList) {

        int affectedRows = 0;
        for (Stage stage : stageList) {
            QueryWrapper<Stage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("stage_id", stage.getStageId());
            int update = stageMapper.update(stage, queryWrapper);
            affectedRows += update;
        }

        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("affectedRows", affectedRows);
        return hashMap;
    }


    public void savePenguinData() {
        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()); // 设置日期格式


        String penguinGlobal = ConfigUtil.PenguinGlobal;
        String responseGlobal = HttpRequestUtil.get(penguinGlobal, new HashMap<>());
        if (responseGlobal == null) return;
        FileUtil.save(ConfigUtil.Penguin, "matrix global.json", responseGlobal);
//        ossService.upload(responseGlobal, "backup/penguin/" + yyyyMMdd + "/matrix global " + yyyyMMddHHmm + ".json");


        String penguinAuto = ConfigUtil.PenguinAuto;
        String responseAuto = HttpRequestUtil.get(penguinAuto, new HashMap<>());
        if (responseAuto == null) return;
        FileUtil.save(ConfigUtil.Penguin, "matrix auto.json", responseAuto);
//        ossService.upload(responseAuto, "backup/penguin/" + yyyyMMdd + "/matrix auto " + yyyyMMddHHmm + ".json");
    }


    public void getPenguinStagesDropData(){
        String response = HttpRequestUtil.get("https://penguin-stats.io/PenguinStats/api/v2/stages", new HashMap<>());
        JsonNode stageDtoList = JsonMapper.parseJSONObject(response);

        Map<String, Stage> stageMap = stageMapper.selectList(null)
                .stream()
                .collect(Collectors.toMap(Stage::getStageId, Function.identity()));

        for(JsonNode jsonNode:stageDtoList){
            String stageId = jsonNode.get("stageId").asText();

            if(stageMap.get(stageId)!=null) continue;

            if(stageId.startsWith("wk_")||stageId.startsWith("randomMaterial")
                    ||stageId.startsWith("recruit")||stageId.startsWith("pro_")
                    ||stageId.startsWith("gacha")) continue;


            int isReproduction = 1;

            String zoneId = jsonNode.get("zoneId").asText();
            String stageCode = jsonNode.get("code").asText();
            int apCost = jsonNode.get("apCost").asInt();
            String stageType = StageType.MAIN;
            if(stageId.contains("act")){
                stageType = StageType.ACT;
                isReproduction = 0;
            }
            if(stageId.contains("perm")){
                stageType = StageType.ACT_PERM;
            }
            if(stageId.contains("rep")){
                stageType = StageType.ACT_REP;
            }
            if(stageId.contains("mini")){
                stageType = StageType.ACT_MINI;
            }

            int minClearTime = 0;
            double spm = 0.0;
            if(jsonNode.get("minClearTime")!=null){
                minClearTime = jsonNode.get("minClearTime").asInt();
                if(minClearTime>0) spm = apCost* 60000.0 / minClearTime ;
            }

            Date date = new Date();
            Date openTime = date;
            Date endTime = new Date(date.getTime()+86400000*14);
            JsonNode existence = JsonMapper.parseJSONObject(jsonNode.get("existence").toPrettyString());
            JsonNode existenceCN = existence.get("CN");

            if(existenceCN.get("openTime")!=null){
                openTime = new Date(existenceCN.get("openTime").asLong());
            }

            if(existenceCN.get("closeTime")!=null)  {
                endTime = new Date(existenceCN.get("closeTime").asLong());
            }

            Stage stage = new Stage();
            stage.setStageId(stageId);
            stage.setStageCode(stageCode);
            stage.setZoneId(zoneId);
            stage.setZoneName("新活动关卡");
            stage.setApCost(apCost);
            stage.setStageType(stageType);
            stage.setStartTime(openTime);
            stage.setEndTime(endTime);
            stage.setSpm(spm);
            stage.setMinClearTime(minClearTime);

            LogUtils.info("本次拉取更新的关卡是："+stageId);
            stageMapper.insert(stage);
        }

    }






}
