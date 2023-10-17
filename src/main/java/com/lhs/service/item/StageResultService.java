package com.lhs.service.item;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Log;
import com.lhs.common.util.StageType;
import com.lhs.entity.dto.item.StageParamDTO;
import com.lhs.entity.po.item.Item;
import com.lhs.entity.po.item.Stage;
import com.lhs.entity.po.item.StageResultCommon;
import com.lhs.entity.po.item.StageResultDetail;
import com.lhs.entity.vo.item.*;
import com.lhs.mapper.item.StageResultCommonMapper;
import com.lhs.mapper.item.StageResultDetailMapper;
import com.lhs.service.dev.OSSService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StageResultService {

    private final StageResultDetailMapper stageResultDetailMapper;
    private final StageResultCommonMapper stageResultCommonMapper;
    private final ItemService itemService;
    private final StageCalService stageCalService;
    private final StageService stageService;
    private final OSSService ossService;
    private final RedisTemplate<String, Object> redisTemplate;


    public StageResultService(StageResultDetailMapper stageResultDetailMapper, StageResultCommonMapper stageResultCommonMapper, ItemService itemService, StageCalService stageCalService, StageService stageService, OSSService ossService, RedisTemplate<String, Object> redisTemplate) {
        this.stageResultDetailMapper = stageResultDetailMapper;
        this.stageResultCommonMapper = stageResultCommonMapper;
        this.itemService = itemService;
        this.stageCalService = stageCalService;
        this.stageService = stageService;
        this.ossService = ossService;
        this.redisTemplate = redisTemplate;
    }


    public void updateStageResultByTaskConfig() {
        String read = FileUtil.read(ApplicationConfig.Item + "stage_task_config.json");
        if (read == null) {
            Log.error("更新关卡配置文件为空");
            return;
        }

        JsonNode stageTaskConfig = JsonMapper.parseJSONObject(read);
        for (JsonNode config : stageTaskConfig) {
            int sampleSize = config.get("sampleSize").asInt();
            double expCoefficient = config.get("expCoefficient").asDouble();
            StageParamDTO stageParamDTO = new StageParamDTO();
            stageParamDTO.setSampleSize(sampleSize);
            stageParamDTO.setExpCoefficient(expCoefficient);
            updateStageResult(stageParamDTO);
            Log.info("本次更新关卡，经验书系数为" + expCoefficient + "，样本数量为" + sampleSize);
        }
    }

    public void updateStageResult(StageParamDTO stageParamDTO) {
        List<Item> items = itemService.getItemList(stageParamDTO);   //找出对应版本的材料价值
        if (items == null || items.size() < 1) {
            items = itemService.getBaseItemList();
        }
        items = itemService.ItemValueCal(items, stageParamDTO);  //计算新的新材料价值
        stageCalService.stageResultCal(items, stageParamDTO);      //用新材料价值计算新关卡效率
        Log.info("V2关卡效率更新成功");
    }

    @RedisCacheable(key = "Stage.T3.V2")
    public List<RecommendedStageVO> getT3RecommendedStageV2(StageParamDTO stageParamDTO) {


        //根据itemType将关卡主要掉落信息分组
        Map<String, List<StageResultCommon>> commonMapByItemType = stageResultCommonMapper
                .selectList(new QueryWrapper<StageResultCommon>()
                        .eq("version", stageParamDTO.getVersion())
                        .ge("end_time", new Date()))
                .stream()
                .filter(e -> !"empty".equals(e.getItemType()))
                .collect(Collectors.groupingBy(StageResultCommon::getItemTypeId));

        //查找关卡的详细掉落信息
        Map<String, StageResultDetail> detailMapByStageId = stageResultDetailMapper
                .selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", stageParamDTO.getVersion())
                        .ge("end_time", new Date()))
                .stream()
                .filter(e -> e.getRatioRank() == 0)
                .collect(Collectors.toMap((StageResultDetail::getStageId), Function.identity()));


        //要返回前端的数据集合
        List<RecommendedStageVO> recommendedStageVOList = new ArrayList<>();

        for (String itemTypeId : commonMapByItemType.keySet()) {
            //每次材料系的推荐关卡的通用掉落信息
            List<StageResultCommon> commonListByItemId = commonMapByItemType.get(itemTypeId);
            String itemType = commonListByItemId.get(0).getItemType();
            //将通用掉落信息按效率倒序排列
            commonListByItemId.sort(Comparator.comparing(StageResultCommon::getStageEfficiency).reversed());

            List<StageResultVOV2> stageResultVOV2List = new ArrayList<>();
            for (StageResultCommon common : commonListByItemId) {
                StageResultDetail detail = detailMapByStageId.get(common.getStageId());
                //将通用结果和详细结果复制到返回结果的实体类中
                StageResultVOV2 stageResultVOV2 = new StageResultVOV2();
                stageResultVOV2.copyByStageResultCommon(common);
                if (detail != null) {
                    stageResultVOV2.copyByStageResultDetail(detail);
                    stageResultVOV2List.add(stageResultVOV2);
                }
                //超过7个关卡退出，前端显示个数有限
                if (stageResultVOV2List.size() > 7) break;
            }

            RecommendedStageVO recommendedStageVo = new RecommendedStageVO();
            recommendedStageVo.setItemTypeId(itemTypeId);
            recommendedStageVo.setItemType(itemType);
            recommendedStageVo.setStageResultList(stageResultVOV2List);
            recommendedStageVOList.add(recommendedStageVo);

            //给关卡赋予等级颜色
            setStageColor(stageResultVOV2List);

        }


        return recommendedStageVOList;
    }


    private static void setStageColor(List<StageResultVOV2> stageResultVOV2List) {

        stageResultVOV2List.forEach(e -> {
            e.setStageColor(2);
            if (e.getItemRarity() == 2) {
                double apExpect = e.getApExpect() * 4 + 200 * 0.0036 - 1.17;
                if ("30012".equals(e.getItemId())) {
                    apExpect = e.getApExpect() * 5 + 200 * 0.0036 - 1.17;
                }
                e.setApExpect(apExpect);
            }
        });

        //拿到效率最高的关卡id
        String stageIdEffMax = stageResultVOV2List.get(0).getStageId();
        //效率最高为3
        stageResultVOV2List.get(0).setStageColor(3);
        //根据期望理智排序
        stageResultVOV2List = stageResultVOV2List.stream()
                .sorted(Comparator.comparing(StageResultVOV2::getApExpect))  //根据期望理智排序
                .collect(Collectors.toList());  //流转为集合
        //拿到期望理智最低的关卡id
        String stageIdExpectMin = stageResultVOV2List.get(0).getStageId();

        //对比俩个id是否一致,一致赋予4，反之赋予1
        stageResultVOV2List.get(0).setStageColor(stageIdEffMax.equals(stageIdExpectMin) ? 4 : 1);

        stageResultVOV2List.forEach(e -> {
            if (e.getItemRarity() == 2) {
                double apExpect = (e.getApExpect() + 1.17 - 200 * 0.0036) / 4;
                if ("30012".equals(e.getItemId())) {
                    apExpect = (e.getApExpect() + 1.17 - 200 * 0.0036) / 5;
                }
                e.setApExpect(apExpect);
            }
        });

    }

    @RedisCacheable(key = "Stage.T2.V2")
    public List<RecommendedStageVO> getT2RecommendedStage(StageParamDTO stageParamDTO) {

        List<RecommendedStageVO> recommendedStageVOList = new ArrayList<>();

        //查询关卡通用掉落信息,根据物品id分组
        Map<String, StageResultCommon> resultSampleMap = stageResultCommonMapper
                .selectList(new QueryWrapper<StageResultCommon>()
                        .eq("version", stageParamDTO.getVersion()))
                .stream()
                .collect(Collectors.toMap(StageResultCommon::getStageId, Function.identity()));

        //查询详细掉落信息
        Map<String, List<StageResultDetail>> collect = stageResultDetailMapper
                .selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", stageParamDTO.getVersion())
                        .ge("end_time", new Date()))
                .stream()
                .filter(e -> e.getItemRarity() == 2&&e.getItemId().startsWith("300"))
                .collect(Collectors.groupingBy(StageResultDetail::getItemId));


        //将通用掉落信息和详细掉落信息组合在一起
        for (String itemId : collect.keySet()) {

            List<StageResultVOV2> stageResultVOV2List = new ArrayList<>();
            List<StageResultDetail> stageResultDetailListByItemId = collect.get(itemId);
            String itemName = stageResultDetailListByItemId.get(0).getItemName();
            //将详细掉落信息根据期望正序排序
            stageResultDetailListByItemId.sort(Comparator.comparing(StageResultDetail::getApExpect));
            for (StageResultDetail detail : stageResultDetailListByItemId) {
                StageResultVOV2 stageResultVOV2 = new StageResultVOV2();
                StageResultCommon common = resultSampleMap.get(detail.getStageId());
                //将通用结果和详细结果复制到返回结果的实体类中
                if (common != null) {
                    stageResultVOV2.copyByStageResultCommon(common);
                }

                stageResultVOV2.copyByStageResultDetail(detail);
                stageResultVOV2.setStageColor(1);
                stageResultVOV2List.add(stageResultVOV2);
                //超过7个关卡退出，前端显示个数有限
                if (stageResultVOV2List.size() > 7) break;
            }

            RecommendedStageVO recommendedStageVo = new RecommendedStageVO();
            recommendedStageVo.setItemTypeId(itemId);
            recommendedStageVo.setItemType(itemName);
            recommendedStageVo.setStageResultList(stageResultVOV2List);
            recommendedStageVOList.add(recommendedStageVo);
        }
        return recommendedStageVOList;
    }


    @RedisCacheable(key = "Stage.Orundum.V2")
    public List<OrundumPerApResultVO> getOrundumRecommendedStage(StageParamDTO stageParamDTO) {
        List<OrundumPerApResultVO> orundumPerApResultVOList = new ArrayList<>();

        //查询关卡通用掉落信息,根据物品id分组
        Map<String, StageResultCommon> resultCommonMap = stageResultCommonMapper
                .selectList(new QueryWrapper<StageResultCommon>()
                        .eq("version", stageParamDTO.getVersion()))
                .stream().collect(Collectors.toMap(StageResultCommon::getStageId, Function.identity()));

        //查询关卡信息
        Map<String, Stage> stageMap = stageService.getStageMapKeyIsStageId();

        stageResultDetailMapper.selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", stageParamDTO.getVersion())
                        .ge("end_time", new Date())).stream()
                .filter(e -> !(e.getStageId().endsWith("LMD")) && e.getItemRarity() <3)
                .collect(Collectors.groupingBy(StageResultDetail::getStageId))
                .forEach((stageId, list) -> {
                    Map<String, Double> calResult = orundumPerApCal(list, stageMap.get(stageId).getApCost());
                    if (calResult.get("orundumPerAp") > 0.2) {
                        StageResultCommon stageResultCommon = resultCommonMap.get(stageId);
                        OrundumPerApResultVO resultVo = OrundumPerApResultVO.builder()
                                .stageCode(stageResultCommon.getStageCode())
                                .stageEfficiency(stageResultCommon.getStageEfficiency())
                                .orundumPerAp(calResult.get("orundumPerAp"))
                                .lMDCost(calResult.get("LMDCost"))
                                .orundumPerApEfficiency(calResult.get("orundumPerAp") / 1.09)
                                .build();
                        orundumPerApResultVOList.add(resultVo);
                    }
                });

        orundumPerApResultVOList.sort(Comparator.comparing(OrundumPerApResultVO::getOrundumPerAp).reversed());

        return orundumPerApResultVOList;
    }


    private Map<String, Double> orundumPerApCal(List<StageResultDetail> stageResultDetailList, double apCost) {
        double knockRating_30011 = 0.0, knockRating_30012 = 0.0, knockRating_30061 = 0.0, knockRating_30062 = 0.0;

        for (StageResultDetail result : stageResultDetailList) {
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
//                    Log.error("不是搓玉用的材料");
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


    @RedisCacheable(key = "Stage.ACT.V2")
    public List<ActStageVO> getActStage(StageParamDTO stageParamDTO) {

        Map<String, List<Stage>> stageGroupByZoneName = stageService.getStageList(new QueryWrapper<Stage>()
                        .le("end_time", new Date()))
                .stream()
                .filter(e -> StageType.ACT.equals(e.getStageType()) || StageType.ACT_REP.equals(e.getStageType()))
                .collect(Collectors.groupingBy(Stage::getZoneName));

        Map<String, StageResultCommon> resultCommonMap = stageResultCommonMapper.selectList(new QueryWrapper<StageResultCommon>()
                        .eq("version", stageParamDTO.getVersion())
                        .le("end_time", new Date()))
                .stream()
                .filter(e -> e.getStageId().endsWith("LMD"))
                .collect(Collectors.toMap(StageResultCommon::getStageId, Function.identity()));

        Map<String, StageResultDetail> resultDetailMap = stageResultDetailMapper.selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", stageParamDTO.getVersion())
                        .le("end_time", new Date()))
                .stream()
                .filter(e -> e.getStageId().endsWith("LMD") && e.getRatioRank() == 0)
                .collect(Collectors.toMap(StageResultDetail::getStageId, Function.identity()));

        List<ActStageVO> actStageVOList = new ArrayList<>();
        for (String zoneName : stageGroupByZoneName.keySet()) {
            List<Stage> stageList = stageGroupByZoneName.get(zoneName);
            Stage stage = stageList.get(0);
            ActStageVO actStageVo = new ActStageVO();
            actStageVo.setZoneName(zoneName);
            actStageVo.setStageType(stage.getStageType());
            actStageVo.setEndTime(stage.getEndTime());
            actStageVo.setActStageList(getActStageResultList(stageList, resultCommonMap, resultDetailMap));
            if (actStageVo.getActStageList().size() > 0) {
                actStageVOList.add(actStageVo);
            }
        }

        actStageVOList.sort(Comparator.comparing(ActStageVO::getEndTime).reversed());

        return actStageVOList;
    }

    private List<ActStageResultVO> getActStageResultList(List<Stage> stageList,
                                                         Map<String, StageResultCommon> resultCommonMap,
                                                         Map<String, StageResultDetail> resultDetailMap) {
        stageList.sort(Comparator.comparing(Stage::getStageId).reversed());

        List<ActStageResultVO> actStageResultVOList = new ArrayList<>();
        for (Stage stage : stageList) {
            String stageId = stage.getStageId() + "_LMD";
            ActStageResultVO actStageResultVO = new ActStageResultVO();
            if (resultCommonMap.get(stageId) != null && resultDetailMap.get(stageId) != null) {
                StageResultCommon common = resultCommonMap.get(stageId);
                actStageResultVO.setStageId(common.getStageId());
                actStageResultVO.setStageCode(common.getStageCode());
                actStageResultVO.setItemName(common.getItemType());
                actStageResultVO.setItemId(common.getItemTypeId());
                actStageResultVO.setStageEfficiency(common.getStageEfficiency());
                StageResultDetail detail = resultDetailMap.get(stageId);
                actStageResultVO.setApExpect(detail.getApExpect());
                actStageResultVO.setKnockRating(detail.getKnockRating());
                actStageResultVO.setKnockRating(actStageResultVO.getKnockRating());
                actStageResultVO.setApExpect(actStageResultVO.getApExpect());
                if (detail.getItemRarity() != null && detail.getItemRarity() == 3) {
                    actStageResultVOList.add(actStageResultVO);
                }
            }

            if (actStageResultVOList.size() > 2) break;
        }


        return actStageResultVOList;
    }


    public List<StageResultVOV2> getStageByZoneName(StageParamDTO stageParamDTO, String zone) {

        List<StageResultCommon> commonList =
                stageResultCommonMapper.selectList(new QueryWrapper<StageResultCommon>()
                        .eq("version", stageParamDTO.getVersion())
                        .ge("end_time", new Date())
                        .ne("item_type", "empty")
                        .like("stage_id", zone)
                        .orderByAsc("stage_id"));

        Map<String, StageResultDetail> detailMap = stageResultDetailMapper.selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", stageParamDTO.getVersion())
                        .eq("ratio_rank", 0)
                        .ge("end_time", new Date())
                        .like("stage_id", zone))
                .stream()
                .collect(Collectors.toMap(StageResultDetail::getStageId, Function.identity()));


        List<StageResultVOV2> stageResultVOV2List = new ArrayList<>();
        commonList.forEach(e -> {
            StageResultVOV2 stageResultVOV2 = new StageResultVOV2();
            stageResultVOV2.copyByStageResultCommon(e);
            if (detailMap.get(e.getStageId()) != null) {
                StageResultDetail detail = detailMap.get(e.getStageId());
                stageResultVOV2.copyByStageResultDetail(detail);

            }
            stageResultVOV2List.add(stageResultVOV2);
        });


        return stageResultVOV2List;
    }

    public Map<String, List<StageResultDetailVO>> getAllStageResult(StageParamDTO stageParamDTO) {
        List<StageResultDetail> stageResultDetailList = stageResultDetailMapper.selectList(new QueryWrapper<StageResultDetail>()
                .eq("version", stageParamDTO.getVersion()));

        Map<String, StageResultCommon> commonMap = stageResultCommonMapper.selectList(new QueryWrapper<StageResultCommon>()
                        .eq("version", stageParamDTO.getVersion()))
                .stream()
                .collect(Collectors.toMap(StageResultCommon::getStageId, Function.identity()));

        List<StageResultDetailVO> detailVOList = new ArrayList<>();
        for (StageResultDetail detail : stageResultDetailList) {
            StageResultDetailVO detailVO = new StageResultDetailVO();
            detailVO.copyByStageResultDetail(detail);
            if (commonMap.get(detail.getStageId()) != null) {
                StageResultCommon common = commonMap.get(detail.getStageId());
                detailVO.copyByStageResultCommon(common);
                detailVOList.add(detailVO);
            }
        }

        return detailVOList.stream()
                .collect(Collectors.groupingBy(StageResultDetailVO::getStageId));

    }

    public void backupStageResult() {
        double expCoefficient = 0.625;
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setExpCoefficient(expCoefficient);
        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        List<RecommendedStageVO> t3RecommendedStageV2 = getT3RecommendedStageV2(stageParamDTO);
        String yyyyMMddHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()); // 设置日期格式

        List<Item> items = itemService.getItemList(stageParamDTO);

        ossService.upload(JsonMapper.toJSONString(t3RecommendedStageV2), "backup/stage/" + yyyyMMdd + "/t3—" + expCoefficient + "—" + yyyyMMddHHmm + ".json");
        ossService.upload(JsonMapper.toJSONString(items), "backup/item/" + yyyyMMdd + "/item—" + expCoefficient + "—" + yyyyMMddHHmm + ".json");
    }



    public List<StageResultVOV2> getStageDetail(String stageId) {
        return null;
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




    //--------------------------------兼容旧接口--------------------------------


    @RedisCacheable(key = "Stage.T3.V1")
    public List<List<StageResultVO>> getT3RecommendedStageV1(StageParamDTO stageParamDTO) {
        //根据itemType将关卡主要掉落信息分组
        Map<String, List<StageResultCommon>> commonMapByItemType = stageResultCommonMapper
                .selectList(new QueryWrapper<StageResultCommon>()
                        .eq("version", stageParamDTO.getVersion())
                        .ge("end_time", new Date()))
                .stream()
                .filter(e -> !"empty".equals(e.getItemType()))
                .collect(Collectors.groupingBy(StageResultCommon::getItemTypeId));

        //查找关卡的详细掉落信息
        Map<String, StageResultDetail> detailMapByStageId = stageResultDetailMapper
                .selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", stageParamDTO.getVersion())
                        .ge("end_time", new Date()))
                .stream()
                .filter(e -> e.getRatioRank() == 0)
                .collect(Collectors.toMap((StageResultDetail::getStageId), Function.identity()));

        List<List<StageResultVO>> stageResultVOListList = new ArrayList<>();

        for (String itemTypeId : commonMapByItemType.keySet()) {
            //每次材料系的推荐关卡的通用掉落信息
            List<StageResultCommon> commonListByItemId = commonMapByItemType.get(itemTypeId);
            String itemType = commonListByItemId.get(0).getItemType();
            //将通用掉落信息按效率倒序排列
            commonListByItemId.sort(Comparator.comparing(StageResultCommon::getStageEfficiency).reversed());

            List<StageResultVO> stageResultVOList = new ArrayList<>();
            for (StageResultCommon common : commonListByItemId) {
                StageResultDetail detail = detailMapByStageId.get(common.getStageId());
                //将通用结果和详细结果复制到返回结果的实体类中
                StageResultVO stageResultVO = new StageResultVO();
                stageResultVO.copyByStageResultCommon(common);
                if (detail != null) {
                    stageResultVO.copyByStageResultDetail(detail);
                    stageResultVOList.add(stageResultVO);
                }
                stageResultVO.setStageEfficiency(stageResultVO.getStageEfficiency()*100);
                //超过7个关卡退出，前端显示个数有限
                if (stageResultVOList.size() > 7) break;
            }

            //给关卡赋予等级颜色
            setStageResultVoColor(stageResultVOList);
            stageResultVOListList.add(stageResultVOList);
        }

        return stageResultVOListList;
    }

    private static void setStageResultVoColor(List<StageResultVO> stageResultList) {
        if (stageResultList.size() < 1) return;

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



    @RedisCacheable(key = "Stage.T2.V1")
    public List<List<StageResultVO>> getT2RecommendedStageV1(StageParamDTO stageParamDTO) {

        //查询关卡通用掉落信息,根据物品id分组
        Map<String, StageResultCommon> resultSampleMap = stageResultCommonMapper
                .selectList(new QueryWrapper<StageResultCommon>()
                        .eq("version", stageParamDTO.getVersion()))
                .stream()
                .collect(Collectors.toMap(StageResultCommon::getStageId, Function.identity()));

        //查询详细掉落信息
        Map<String, List<StageResultDetail>> collect = stageResultDetailMapper
                .selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", stageParamDTO.getVersion())
                        .ge("end_time", new Date()))
                .stream()
                .filter(e -> e.getItemRarity() == 2&&e.getItemId().startsWith("300"))
                .collect(Collectors.groupingBy(StageResultDetail::getItemId));


        List<List<StageResultVO>> stageResultVOListList = new ArrayList<>();
        //将通用掉落信息和详细掉落信息组合在一起
        for (String itemId : collect.keySet()) {
            List<StageResultVO> stageResultVOList = new ArrayList<>();
            List<StageResultDetail> stageResultDetailListByItemId = collect.get(itemId);
            //将详细掉落信息根据期望正序排序
            stageResultDetailListByItemId.sort(Comparator.comparing(StageResultDetail::getApExpect));
            for (StageResultDetail detail : stageResultDetailListByItemId) {
                StageResultVO stageResultVO = new StageResultVO();
                StageResultCommon common = resultSampleMap.get(detail.getStageId());
                //将通用结果和详细结果复制到返回结果的实体类中
                if (common != null) {
                    stageResultVO.copyByStageResultCommon(common);
                }

                stageResultVO.copyByStageResultDetail(detail);
                stageResultVO.setStageColor(1);
                stageResultVO.setStageEfficiency(stageResultVO.getStageEfficiency()*100);
                stageResultVOList.add(stageResultVO);
                //超过7个关卡退出，前端显示个数有限
                if (stageResultVOList.size() > 7) break;
            }
            stageResultVOListList.add(stageResultVOList);
        }

        return stageResultVOListList;
    }

}
