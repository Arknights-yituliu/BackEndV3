package com.lhs.service.stage;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.Log;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.stage.Item;
import com.lhs.entity.stage.StageResult;
import com.lhs.mapper.StageResultMapper;
import com.lhs.service.dev.OSSService;
import com.lhs.vo.stage.OrundumPerApResultVo;
import com.lhs.vo.stage.StageAndItemCoefficient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StageResultService  {

    private final StageResultMapper stageResultMapper;
    private final ItemService itemService;
    private final OSSService ossService;
    private final StageCalService stageCalService;


    private final RedisTemplate<String,Object> redisTemplate;

    public StageResultService(StageResultMapper stageResultMapper,
                              ItemService itemService,
                              OSSService ossService,
                              StageCalService stageCalService,
                              RedisTemplate<String,Object> redisTemplate) {
        this.stageResultMapper = stageResultMapper;
        this.itemService = itemService;
        this.ossService = ossService;
        this.stageCalService = stageCalService;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(cron = "0 3/10 * * * ?")
    public void updateStageResult(){
        StageAndItemCoefficient stageAndItemCoefficient = new StageAndItemCoefficient();
        stageAndItemCoefficient.setExpCoefficient(0.625);
        stageAndItemCoefficient.setSampleSize(200);
        stageAndItemCoefficient.setType("public");
        String version = "public-"+stageAndItemCoefficient.getExpCoefficient();

        List<Item> items = itemService.queryItemList(version);   //找出该经验书价值系数版本的材料价值表Vn
        if(items.size()<1){
            items = itemService.queryItemList("public-0.625");
        }

        items = itemService.ItemValueCalculation(items, stageAndItemCoefficient);  //用上面蓝材料对应的常驻最高关卡效率En计算新的新材料价值表Vn+1
        stageCalService.stageResultCal(items, stageAndItemCoefficient);      //用新材料价值表Vn+1再次计算新关卡效率En+1
        Log.info("关卡效率更新成功");
    }

    @Scheduled(cron = "0 5/10 * * * ?")
    public void authUpdateStageResult(){
        StageAndItemCoefficient stageAndItemCoefficient = new StageAndItemCoefficient();
        stageAndItemCoefficient.setExpCoefficient(0.625);
        stageAndItemCoefficient.setSampleSize(10);
        stageAndItemCoefficient.setType("auth");
        String version = "auth-" +stageAndItemCoefficient.getExpCoefficient();

        List<Item> items = itemService.queryItemList(version);   //找出该经验书价值系数版本的材料价值表Vn

        if(items.size()<1){
            items = itemService.queryItemList("public-0.625");
        }

        items = itemService.ItemValueCalculation(items, stageAndItemCoefficient);  //用上面蓝材料对应的常驻最高关卡效率En计算新的新材料价值表Vn+1

        stageCalService.stageResultCal(items, stageAndItemCoefficient);      //用新材料价值表Vn+1再次计算新关卡效率En+1
        Log.info("低样本关卡效率更新成功");
    }

    @Scheduled(cron = "0 5/10 * * * ?")
    public void backupStageResult(){

        double expCoefficient = 0.625;
        String version = "public-"+expCoefficient;

        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()); // 设置日期格式

        List<List<StageResult>> resultData_t3 = queryStageResultData_t3(version);
        List<List<StageResult>> resultData_closed =queryStageResultData_closedActivities(version);
        List<Item> items = itemService.queryItemList(version);

        ossService.upload(JSON.toJSONString(resultData_t3), "stage/" + yyyyMMdd + "/t3—" + expCoefficient + "—" + yyyyMMddHHmm + ".json");
        ossService.upload(JSON.toJSONString(resultData_closed), "stage/" + yyyyMMdd + "/closed—" + expCoefficient + "—" + yyyyMMddHHmm + ".json");
        ossService.upload(JSON.toJSONString(items), "item/" + yyyyMMdd + "/item—" + expCoefficient + "—" + yyyyMMddHHmm + ".json");

    }

    /**
     * 查询蓝材料的推荐关卡
     *
     * @param version 版本
     * @return 关卡结果
     */
    @RedisCacheable(key = "stage.t3.#version", timeout = 1200)
    public List<List<StageResult>> queryStageResultData_t3(String version) {
        List<List<StageResult>> stageResultList = new ArrayList<>();
        QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ratio_rank", 0)
                .eq("version", version)
                .ge("end_time", new Date())
                .ne("item_type", "empty");
        List<StageResult> list = stageResultMapper.selectList(queryWrapper);
        Map<String, List<StageResult>> collect = list.stream()
                .collect(Collectors.groupingBy(StageResult::getItemType));

        for (String itemType : collect.keySet()) {
            List<StageResult> resultList = collect.get(itemType);
            resultList = resultList.stream()
                    .sorted(Comparator.comparing(StageResult::getStageEfficiency).reversed())
                    .limit(8)
                    .collect(Collectors.toList());
            setStageColor(resultList);
            stageResultList.add(resultList);
        }

        return stageResultList;
    }

    /**
     * 查询绿材料的推荐关卡
     *
     * @param version 版本
     * @return 关卡结果
     */
    @RedisCacheable(key = "stage.t2.#version", timeout = 1200)
    public List<List<StageResult>> queryStageResultData_t2(String version) {
        List<List<StageResult>> stageResultList = new ArrayList<>();
        QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("item_rarity", 2)
                .eq("version", version)
                .ge("end_time", new Date());
        List<StageResult> resultNewList = stageResultMapper.selectList(queryWrapper);
        Map<String, List<StageResult>> collect = resultNewList.stream()
                .collect(Collectors.groupingBy(StageResult::getItemName));

        for (String itemName : collect.keySet()) {
            List<StageResult> resultList = collect.get(itemName);
            resultList = resultList.stream()
                                   .sorted(Comparator
                                   .comparing(StageResult::getApExpect))
                                   .limit(8)
                                   .collect(Collectors.toList());

            stageResultList.add(resultList);
        }
        return stageResultList;
    }

    /**
     * 查询已关闭活动关卡
     * @param version 版本
     * @return 关卡结果
     */
    @RedisCacheable(key = "stage.closed.#version", timeout = 86400)
    public List<List<StageResult>> queryStageResultData_closedActivities(String version) {
        List<List<StageResult>> stageResultList = new ArrayList<>();
        QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ratio_rank", 0)
                .eq("item_rarity", 3)
                .eq("stage_type",3)
                .eq("version", version)
                .le("end_time", new Date())
                .notLike("stage_id", "LMD")
                .notLike("stage_id", "rep")
                .ne("item_type", "empty");

        List<StageResult> list = stageResultMapper.selectList(queryWrapper);
        Map<String, List<StageResult>> collect = list.stream()
                .collect(Collectors.groupingBy(StageResult::getZoneName));
        List<StageResult> sortList = new ArrayList<>();
        for (String zoneName : collect.keySet()) {
            sortList.add(collect.get(zoneName).get(0));
        }

        sortList = sortList.stream()
                .sorted(Comparator.comparing(StageResult::getStartTime).reversed())
                .collect(Collectors.toList());

        for (StageResult stageResult : sortList) {
            List<StageResult> resultList = collect.get(stageResult.getZoneName());
            resultList = resultList.stream().
                    sorted(Comparator.comparing(StageResult::getStageId))
                    .collect(Collectors.toList());
            stageResultList.add(resultList);
        }


        return stageResultList;
    }


    /**
     * 查询搓玉推荐关卡
     *
     * @param version 版本
     * @return 关卡结果
     */
    @RedisCacheable(key = "stage.orundum.#version", timeout = 1200)
    public List<OrundumPerApResultVo> queryStageResultData_Orundum(String version) {
        List<OrundumPerApResultVo> orundumPerApResultVoList = new ArrayList<>();

        QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("version", version)
                .in("item_name", "固源岩", "源岩", "装置", "破损装置")
                .notLike("stage_id", "LMD")
                .ge("end_time", new Date());
        List<StageResult> stageResultList = stageResultMapper.selectList(queryWrapper);
        stageResultList.stream()
                .collect(Collectors.groupingBy(StageResult::getStageId))
                .forEach((k, list) -> {
                    HashMap<String, Double> calResult = orundumPerApCal(list);
                    if (calResult.get("orundumPerAp") > 0.2) {

                        OrundumPerApResultVo resultVo = OrundumPerApResultVo.builder()
                                .stageCode(list.get(0).getStageCode())
                                .stageEfficiency(list.get(0).getStageEfficiency())
                                .orundumPerAp(calResult.get("orundumPerAp"))
                                .lMDCost(calResult.get("LMDCost"))
                                .orundumPerApEfficiency(calResult.get("orundumPerAp") / 1.09 * 100)
                                .build();
                        orundumPerApResultVoList.add(resultVo);
                    }
                });

        orundumPerApResultVoList.sort(Comparator.comparing(OrundumPerApResultVo::getOrundumPerAp).reversed());

        return orundumPerApResultVoList;
    }

    private HashMap<String, Double> orundumPerApCal(List<StageResult> stageResults) {
        double knockRating_30011 = 0.0, knockRating_30012 = 0.0, knockRating_30061 = 0.0, knockRating_30062 = 0.0;

        double apCost = stageResults.get(0).getApCost();
        for (StageResult result : stageResults) {
            switch (result.getItemId()) {
                case "30011":
                    knockRating_30011 = result.getKnockRating();
                    break;
                case "30012":
                    knockRating_30012 = result.getKnockRating();
                    break;
                case "30061":
                    knockRating_30061 = result.getKnockRating();
                    break;
                case "30062":
                    knockRating_30062 = result.getKnockRating();
                    break;
                default:
                    log.error("不是搓玉用的材料");
            }
        }


        double orundumPerAp = (knockRating_30012 * 5 + knockRating_30011 * 5 / 3 +
                knockRating_30062 * 10 + knockRating_30061 * 10 / 3) / apCost;

        double LMDCostPerAp = (knockRating_30012 * 800 + knockRating_30011 * 800 / 3 +
                knockRating_30062 * 1000 + knockRating_30061 * 1000 / 3) / apCost;

        double LMDCost = LMDCostPerAp * 600 / orundumPerAp / 10000;


        HashMap<String, Double> hashMap = new HashMap<>();
        hashMap.put("orundumPerAp", orundumPerAp);
        hashMap.put("LMDCost", LMDCost);
        return hashMap;
    }

    /**
     * 查询关卡详细
     *
     * @param stageId 关卡id
     * @return
     */
    public List<StageResult> queryStageResultDataDetailByStageId(String stageId) {
        QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("version", "public-"+0.625)
                .eq("stage_id", stageId);

        List<StageResult> stageResults = stageResultMapper.selectList(queryWrapper);
        if (stageResults == null || stageResults.size() == 0) throw new ServiceException(ResultCode.DATA_NONE);

        return stageResults;
    }

    public String resetCache(){

        Boolean delete = redisTemplate.delete("stage.t3.public-0.625");
        Boolean delete1 = redisTemplate.delete("stage.t2.public-0.625");
        Boolean delete2 = redisTemplate.delete("stage.orundum.public-0.625");
        Boolean delete3 = redisTemplate.delete("stage.closed.public-0.625");

        String message = "清空失败";
        if(Boolean.TRUE.equals(delete) && Boolean.TRUE.equals(delete1) &&
                Boolean.TRUE.equals(delete2) && Boolean.TRUE.equals(delete3)){
            message = "清空缓存";
        }

        return message;
    }



    private static void setStageColor(List<StageResult> stageResultList) {
        if (stageResultList.size() < 1) return;

        stageResultList = stageResultList.stream()
                .filter(e -> e.getStageColor() > 0).collect(Collectors.toList());

        for (StageResult stageResult : stageResultList) {
            stageResult.setStageColor(2);
            if (stageResult.getItemRarity() == 2) {
                double apExpect = stageResult.getApExpect() * 4 + 200 * 0.0036 - 1.17;
                if ("固源岩".equals(stageResult.getMain())) {
                    apExpect = stageResult.getApExpect() * 5 + 200 * 0.0036 - 1.17;
                }
                stageResult.setApExpect(apExpect);
            }
//            stageResult.setStageEfficiency(stageResult.getStageEfficiency() * 100);
        }

        String stageId_effMax = stageResultList.get(0).getStageId();   //拿到效率最高的关卡id
        stageResultList.get(0).setStageColor(3);  //效率最高为3

        stageResultList = stageResultList.stream()
                .limit(8)  //限制个数
                .sorted(Comparator.comparing(StageResult::getApExpect))  //根据期望理智排序
                .collect(Collectors.toList());  //流转为集合

        String stageId_expectMin = stageResultList.get(0).getStageId(); //拿到期望理智最低的关卡id

        if (stageId_effMax.equals(stageId_expectMin)) {  //对比俩个id是否一致
            stageResultList.get(0).setStageColor(4); // 一致为4
        } else {
            stageResultList.get(0).setStageColor(1); // 不一致为1
        }

        for (StageResult stageResult : stageResultList) {
            if (stageResult.getItemRarity() == 2) {
                double apExpect = (stageResult.getApExpect() + 1.17 - 200 * 0.0036) / 4;

                if ("固源岩".equals(stageResult.getMain())) {
                    apExpect = (stageResult.getApExpect() + 1.17 - 200 * 0.0036) / 5;
                }
                stageResult.setApExpect(apExpect);
            }
        }
    }
}