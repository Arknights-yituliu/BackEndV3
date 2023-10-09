package com.lhs.service.stage;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Log;
import com.lhs.common.entity.ResultCode;
import com.lhs.entity.dto.stage.StageParamDTO;
import com.lhs.entity.po.stage.Item;
import com.lhs.entity.po.stage.Stage;
import com.lhs.entity.po.stage.StageResult;
import com.lhs.entity.vo.stage.StageResultVO;
import com.lhs.mapper.StageResultMapper;
import com.lhs.service.dev.OSSService;
import com.lhs.entity.vo.stage.OrundumPerApResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StageResultService {

    private final StageResultMapper stageResultMapper;
    private final ItemService itemService;
    private final OSSService ossService;
    private final StageCalService stageCalService;
    private final StageService stageService;
    private final RedisTemplate<String, Object> redisTemplate;

    public StageResultService(StageResultMapper stageResultMapper, ItemService itemService, OSSService ossService, StageCalService stageCalService, StageService stageService, RedisTemplate<String, Object> redisTemplate) {
        this.stageResultMapper = stageResultMapper;
        this.itemService = itemService;
        this.ossService = ossService;
        this.stageCalService = stageCalService;
        this.stageService = stageService;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(cron = "0 3/10 * * * ?")
    public void updateStageResult() {
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setExpCoefficient(0.625);
        stageParamDTO.setSampleSize(100);
        stageParamDTO.setType("public");

        List<Item> items = itemService.queryItemList(stageParamDTO);   //找出对应版本的材料价值

        if (items == null || items.size() < 1) {
            items = itemService.queryBaseItemList();
        }

        items = itemService.ItemValueCal(items, stageParamDTO);  //计算新的新材料价值
        stageCalService.stageResultCal(items, stageParamDTO);      //用新材料价值计算新关卡效率
        Log.info("关卡效率更新成功");
    }

    @Scheduled(cron = "0 5/10 * * * ?")
    public void authUpdateStageResult() {
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setExpCoefficient(0.625);
        stageParamDTO.setSampleSize(10);
        stageParamDTO.setType("auth");
        List<Item> items = itemService.queryItemList(stageParamDTO);   //找出对应版本的材料价值

        if (items == null || items.size() < 1) {
            items = itemService.queryBaseItemList();
        }

        items = itemService.ItemValueCal(items, stageParamDTO);  //计算新的新材料价值

        stageCalService.stageResultCal(items, stageParamDTO);      //用新材料价值计算新关卡效率
        Log.info("低样本关卡效率更新成功");
    }

    @Scheduled(cron = "0 5/10 * * * ?")
    public void backupStageResult() {

        double expCoefficient = 0.625;
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setExpCoefficient(expCoefficient);
        String version = stageParamDTO.getVersion();

        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()); // 设置日期格式

        List<List<StageResultVO>> resultData_t3 = getStageResultDataT3V2(stageParamDTO);
        List<List<StageResult>> resultData_closed = getStageResultDataClosedStage(stageParamDTO);
        List<Item> items = itemService.queryItemList(stageParamDTO);

        ossService.upload(JsonMapper.toJSONString(resultData_t3), "backup/stage/" + yyyyMMdd + "/t3—" + expCoefficient + "—" + yyyyMMddHHmm + ".json");
        ossService.upload(JsonMapper.toJSONString(resultData_closed), "backup/stage/" + yyyyMMdd + "/closed—" + expCoefficient + "—" + yyyyMMddHHmm + ".json");
        ossService.upload(JsonMapper.toJSONString(items), "backup/item/" + yyyyMMdd + "/item—" + expCoefficient + "—" + yyyyMMddHHmm + ".json");

    }

    @RedisCacheable(key = "Stage.T3", timeout = 1200)
    public List<List<StageResultVO>> getStageResultDataT3V2(StageParamDTO stageParamDTO) {

        QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("version", stageParamDTO.getVersion())
                .ge("end_time", new Date())
                .ne("item_type", "empty");

        //查出全部计算结果
        List<StageResult> list = stageResultMapper.selectList(queryWrapper);

        List<StageResultVO> stageResultVOList = new ArrayList<>();
        //根据关卡id分组
        Map<String, List<StageResult>> collectByStageId = list.stream()
                .collect(Collectors.groupingBy(StageResult::getStageId));
        //计算关卡的主产物所在材料系每种等级的产出占比
        for (String stageId : collectByStageId.keySet()) {
            List<StageResult> stageResults = collectByStageId.get(stageId);
            //排序
            stageResults = stageResults.stream()
                    .sorted(Comparator.comparing(StageResult::getRatioRank)).collect(Collectors.toList());
            //返还前端的vo类
            StageResultVO stageResultVo = new StageResultVO();
            //复制关卡结果
            stageResultVo.copy(stageResults.get(0));
            //计算主产物所在材料系每种等级的产出占比
            calMainItemRatio(stageResultVo, stageResults);
            stageResultVOList.add(stageResultVo);
        }

        //根据材料系分类关卡计算结果
        Map<String, List<StageResultVO>> collectByItemType = stageResultVOList.stream()
                .collect(Collectors.groupingBy(StageResultVO::getItemType));


        List<List<StageResultVO>> stageResultVoListList = new ArrayList<>();

        //给每个材料系的关卡结果排序，截取最大长度，赋予效率等级颜色
        for (String itemType : collectByItemType.keySet()) {
            List<StageResultVO> resultList = collectByItemType.get(itemType);
            resultList = resultList.stream()
                    .sorted(Comparator.comparing(StageResultVO::getStageEfficiency).reversed())
                    .limit(8)
                    .collect(Collectors.toList());
            //赋予效率等级颜色
            setStageResultVoColor(resultList);
            stageResultVoListList.add(resultList);
        }

        return stageResultVoListList;
    }

    /**
     * @param stageResultVo 关卡结果展示对象
     * @param stageResults  关卡每种掉落物品的详细数据
     */
    private void calMainItemRatio(StageResultVO stageResultVo, List<StageResult> stageResults) {

        String itemType = stageResults.get(0).getItemType();  //关卡掉落材料所属类型   比如1-7，4-6这种同属”固源岩组“类
        //获取材料的上下位材料
        String read = FileUtil.read(ApplicationConfig.Item + "item_type_table.json");
        JsonNode itemTypeTable = JsonMapper.parseJSONObject(read);
        JsonNode itemTypeNode = itemTypeTable.get(itemType);

        //该系材料的每种等级材料在关卡中的占比
        double rarity1Ratio = 0.0;
        double rarity2Ratio = 0.0;
        double rarity3Ratio = 0.0;
        double rarity4Ratio = 0.0;

        //计算该系材料的每种等级材料在关卡中的占比
        for (StageResult stageResult : stageResults) {
            String itemName = stageResult.getItemName();
            if (itemTypeNode.get(itemName) != null) {
                JsonNode item = itemTypeNode.get(itemName);
                int rarity = item.get("rarity").asInt();
                if (rarity == 1) rarity1Ratio = stageResult.getRatio();
                if (rarity == 2) rarity2Ratio = stageResult.getRatio();
                if (rarity == 3) rarity3Ratio = stageResult.getRatio();
                if (rarity == 4) rarity4Ratio = stageResult.getRatio();
            }
        }

        stageResultVo.setItemRarityLessThan5Ratio(rarity4Ratio + rarity3Ratio + rarity2Ratio + rarity1Ratio);  //等级1-4的材料占比
        stageResultVo.setItemRarityLessThan4Ratio(rarity3Ratio + rarity2Ratio + rarity1Ratio); //等级1-3的材料占比
        stageResultVo.setItemRarityLessThan3Ratio(rarity2Ratio + rarity1Ratio); //等级1-2的材料占比
    }

    public List<StageResultVO> getStageResultDataByZone(String version, String zone) {
        QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ratio_rank", 0)
                .eq("version", version)
                .ge("end_time", new Date())
                .ne("item_type", "empty")
                .like("stage_code", zone);


        List<Stage> stageList = stageService.getStageListByWrapper(
                new QueryWrapper<Stage>().like("stage_id", "main_" + zone));

        List<StageResult> stageResultList = stageResultMapper.selectList(queryWrapper);
        List<StageResultVO> stageResultVOList = new ArrayList<>();

        Map<String, StageResult> collectByStageId = stageResultList.stream().collect(Collectors.toMap(StageResult::getStageId, Function.identity()));

        for(Stage stage:stageList){
            String stageId = stage.getStageId();
            if(collectByStageId.get(stageId)==null){
                StageResult build = StageResult.builder()
                        .stageId(stage.getStageId())
                        .stageCode(stage.getStageCode())
                        .sampleSize(0)
                        .stageEfficiency(0.0)
                        .updateTime(stageResultList.get(0).getUpdateTime())
                        .build();
                stageResultList.add(build);
            }
        }

        stageResultList.stream().sorted(Comparator.comparing(StageResult::getStageId)).forEach(e -> {
            StageResultVO stageResultVo = new StageResultVO();
            stageResultVo.copy(e);
            stageResultVOList.add(stageResultVo);
        });

        return stageResultVOList;
    }

    /**
     * 查询绿材料的推荐关卡
     *
     * @param stageParamDTO 版本
     * @return 关卡结果
     */
    @RedisCacheable(key = "Stage.T2", timeout = 1200)
    public List<List<StageResultVO>> getStageResultDataT2(StageParamDTO stageParamDTO) {

        QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("item_rarity", 2)
                .eq("version", stageParamDTO.getVersion())
                .ge("end_time", new Date());
        List<StageResult> resultNewList = stageResultMapper.selectList(queryWrapper);
        Map<String, List<StageResult>> collect = resultNewList.stream()
                .collect(Collectors.groupingBy(StageResult::getItemName));

        List<List<StageResultVO>> stageResultVoListList = new ArrayList<>();

        for (String itemName : collect.keySet()) {
            List<StageResultVO> stageResultVOList = new ArrayList<>();
            collect.get(itemName).stream()
                    .sorted(Comparator
                            .comparing(StageResult::getApExpect))
                    .limit(8)
                    .collect(Collectors.toList())
                    .forEach(e -> {
                        StageResultVO stageResultVo = new StageResultVO();
                        stageResultVo.copy(e);
                        stageResultVOList.add(stageResultVo);
                    });
            stageResultVoListList.add(stageResultVOList);
        }
        return stageResultVoListList;
    }

    /**
     * 查询已关闭活动关卡
     *
     * @param stageParamDTO 版本
     * @return 关卡结果
     */
    @RedisCacheable(key = "Stage.Closed", timeout = 86400)
    public List<List<StageResult>> getStageResultDataClosedStage(StageParamDTO stageParamDTO) {
        List<List<StageResult>> stageResultList = new ArrayList<>();
        QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ratio_rank", 0)
                .eq("item_rarity", 3)
                .eq("stage_type", 3)
                .eq("version", stageParamDTO.getVersion())
                .le("end_time", new Date())
                .like("stage_id", "LMD")
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
    @RedisCacheable(key = "Stage.Orundum", timeout = 1200)
    public List<OrundumPerApResultVO> getStageResultDataOrundum(String version) {
        List<OrundumPerApResultVO> orundumPerApResultVOList = new ArrayList<>();

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

                        OrundumPerApResultVO resultVo = OrundumPerApResultVO.builder()
                                .stageCode(list.get(0).getStageCode())
                                .stageEfficiency(list.get(0).getStageEfficiency())
                                .orundumPerAp(calResult.get("orundumPerAp"))
                                .lMDCost(calResult.get("LMDCost"))
                                .orundumPerApEfficiency(calResult.get("orundumPerAp") / 1.09 * 100)
                                .build();
                        orundumPerApResultVOList.add(resultVo);
                    }
                });

        orundumPerApResultVOList.sort(Comparator.comparing(OrundumPerApResultVO::getOrundumPerAp).reversed());

        return orundumPerApResultVOList;
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
     * @return 关卡详情信息
     */
    public List<StageResult> getStageResultDataDetailByStageId(String stageId) {
        QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("version", "public." + 0.625)
                .eq("stage_id", stageId);

        List<StageResult> stageResults = stageResultMapper.selectList(queryWrapper);
        if (stageResults == null || stageResults.size() == 0) throw new ServiceException(ResultCode.DATA_NONE);

        return stageResults;
    }

    public String resetCache() {

        Boolean delete = redisTemplate.delete("Stage.T3");
        Boolean delete1 = redisTemplate.delete("Stage.T2");
        Boolean delete2 = redisTemplate.delete("Stage.Orundum");
        Boolean delete3 = redisTemplate.delete("Stage.Closed");

        String message = "清空失败";
        if (Boolean.TRUE.equals(delete) && Boolean.TRUE.equals(delete1) &&
                Boolean.TRUE.equals(delete2) && Boolean.TRUE.equals(delete3)) {
            message = "清空缓存";
        }

        return message;
    }


    private static void setStageResultVoColor(List<StageResultVO> stageResultList) {
        if (stageResultList.size() < 1) return;

        stageResultList = stageResultList.stream()
                .filter(e -> e.getStageColor() > 0).collect(Collectors.toList());

        for (StageResultVO stageResult : stageResultList) {
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
                .sorted(Comparator.comparing(StageResultVO::getApExpect))  //根据期望理智排序
                .collect(Collectors.toList());  //流转为集合

        String stageId_expectMin = stageResultList.get(0).getStageId(); //拿到期望理智最低的关卡id

        if (stageId_effMax.equals(stageId_expectMin)) {  //对比俩个id是否一致
            stageResultList.get(0).setStageColor(4); // 一致为4
        } else {
            stageResultList.get(0).setStageColor(1); // 不一致为1
        }

        for (StageResultVO stageResult : stageResultList) {
            if (stageResult.getItemRarity() == 2) {
                double apExpect = (stageResult.getApExpect() + 1.17 - 200 * 0.0036) / 4;

                if ("固源岩".equals(stageResult.getMain())) {
                    apExpect = (stageResult.getApExpect() + 1.17 - 200 * 0.0036) / 5;
                }
                stageResult.setApExpect(apExpect);
            }
        }
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