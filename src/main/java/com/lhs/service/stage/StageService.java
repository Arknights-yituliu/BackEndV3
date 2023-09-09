package com.lhs.service.stage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.HttpRequestUtil;
import com.lhs.entity.stage.Stage;

import com.lhs.mapper.StageMapper;
import com.lhs.service.dev.OSSService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StageService {

    @Resource
    private StageMapper stageMapper;
    @Resource
    private OSSService ossService;

    /**
     * 保存企鹅物流数据到本地
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void savePenguinData() {
        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()); // 设置日期格式


        String penguinGlobal = ApplicationConfig.PenguinGlobal;
        String responseGlobal = HttpRequestUtil.get(penguinGlobal, new HashMap<>());
        if (responseGlobal == null) return;
        FileUtil.save(ApplicationConfig.Penguin, "matrix global.json", responseGlobal);
        ossService.upload(responseGlobal, "penguin/" + yyyyMMdd + "/matrix global " + yyyyMMddHHmm + ".json");


        String penguinAuto = ApplicationConfig.PenguinAuto;
        String responseAuto = HttpRequestUtil.get(penguinAuto, new HashMap<>());
        if (responseAuto == null) return;
        FileUtil.save(ApplicationConfig.Penguin, "matrix auto.json", responseAuto);
        ossService.upload(responseAuto, "penguin/" + yyyyMMdd + "/matrix auto " + yyyyMMddHHmm + ".json");
    }





    public List<Stage> getStageList(QueryWrapper<Stage> queryWrapper) {
        QueryWrapper<Stage> stageNewQueryWrapper = new QueryWrapper<>();
        stageNewQueryWrapper.notLike("stage_id", "tough").orderByDesc("stage_id");
        List<Stage> stageList = stageMapper.selectList(stageNewQueryWrapper);
        return stageMapper.selectList(queryWrapper);
    }

    public Map<String, List<Stage>> getStageMap() {
        QueryWrapper<Stage> stageNewQueryWrapper = new QueryWrapper<>();
        stageNewQueryWrapper.notLike("stage_id", "tough")
                .orderByDesc("stage_id");
        List<Stage> stageList = stageMapper.selectList(stageNewQueryWrapper);
        Map<String, List<Stage>> collect = stageList.stream().collect(Collectors.groupingBy(Stage::getZoneName));
        return collect;
    }

    public Map<String, List<Stage>> getStageMenu() {
        QueryWrapper<Stage> queryWrapper = new QueryWrapper<>();
        queryWrapper.notLike("stage_id", "tough")
                    .groupBy("zone_name")
                    .orderByDesc("start_time");
        List<Stage> stages = stageMapper.selectList(queryWrapper);

        List<Stage> main = new ArrayList<>();
        List<Stage> perm = new ArrayList<>();
        List<Stage> act = new ArrayList<>();
        List<Stage> mini = new ArrayList<>();

        for(Stage stage : stages){
//            System.out.println(stage.getStageId() +stage.getStageId().contains("main"));
            if(stage.getStageType()==1){
                main.add(stage);
                continue;
            }
            if(stage.getStageType()==2){
                perm.add(stage);
                continue;
            }
            if(stage.getStageType()==3){
                act.add(stage);
                continue;
            }
            if(stage.getStageType()==4){
                mini.add(stage);
            }


        }

        Map<String, List<Stage>> hashMap = new LinkedHashMap<>();
        hashMap.put("主线章节",main);
        hashMap.put("支线故事",act);
        hashMap.put("别传插曲",perm);
        hashMap.put("故事集",mini);
        return hashMap;
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

    public LinkedHashMap<String, List<Stage>> queryStageTable() {
        List<Stage> stageList = stageMapper.selectList(new QueryWrapper<Stage>()
                .notLike("stage_id", "tough").orderByDesc("stage_id"));
        List<Stage> zoneList = stageMapper.selectList(new QueryWrapper<Stage>()
                .notLike("stage_id", "tough").groupBy("zone_id").orderByDesc("stage_id"));
        Map<String, List<Stage>> collect = stageList.stream().collect(Collectors.groupingBy(Stage::getZoneName));
        LinkedHashMap<String, List<Stage>> result = new LinkedHashMap<>();
        zoneList.forEach(stage -> result.put(stage.getZoneName(), collect.get(stage.getZoneName())));

        return result;
    }


}
