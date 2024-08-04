package com.lhs.service.material;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.item.StageParamDTO;
import com.lhs.entity.po.material.*;
import com.lhs.entity.vo.item.*;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.material.MaterialValueConfigMapper;
import com.lhs.mapper.material.StageResultMapper;
import com.lhs.mapper.material.StageResultDetailMapper;
import com.lhs.service.user.UserService;
import com.lhs.service.util.OSSService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StageResultService {

    private final StageResultDetailMapper stageResultDetailMapper;
    private final StageResultMapper stageResultMapper;
    private final ItemService itemService;
    private final StageCalService stageCalService;
    private final StageService stageService;
    private final OSSService ossService;
    private final RedisTemplate<String, Object> redisTemplate;

    private final UserService userService;
    private final MaterialValueConfigMapper materialValueConfigMapper;

    public StageResultService(StageResultDetailMapper stageResultDetailMapper, StageResultMapper stageResultMapper, ItemService itemService, StageCalService stageCalService, StageService stageService, OSSService ossService, RedisTemplate<String, Object> redisTemplate, UserService userService, MaterialValueConfigMapper materialValueConfigMapper) {
        this.stageResultDetailMapper = stageResultDetailMapper;
        this.stageResultMapper = stageResultMapper;
        this.itemService = itemService;
        this.stageCalService = stageCalService;
        this.stageService = stageService;
        this.ossService = ossService;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        this.materialValueConfigMapper = materialValueConfigMapper;
    }


    public void updateStageResultByTaskConfig() {
        String read = FileUtil.read(ConfigUtil.Item + "stage_task_config.json");
        if (read == null) {
            Logger.error("更新关卡配置文件为空");
            return;
        }

        JsonNode stageTaskConfig = JsonMapper.parseJSONObject(read);

        JsonNode expCoefficientNode = stageTaskConfig.get("expCoefficient");
        JsonNode sampleSizeNode = stageTaskConfig.get("sampleSize");

        Map<String, String> stageBlacklist = new HashMap<>();
        if (stageTaskConfig.get("stageBlacklist") != null) {
            JsonNode jsonNode = stageTaskConfig.get("stageBlacklist");
            for (JsonNode stageIdNode : jsonNode) {
                stageBlacklist.put(stageIdNode.asText(), stageIdNode.asText());
            }
        }

        List<Double> expCoefficientList = new ArrayList<>();
        List<Integer> sampleSizeList = new ArrayList<>();

        for (JsonNode jsonNode : expCoefficientNode) {
            expCoefficientList.add(jsonNode.asDouble());
        }

        for (JsonNode jsonNode : sampleSizeNode) {
            sampleSizeList.add(jsonNode.asInt());
        }

        for (Double expCoefficient : expCoefficientList) {
            for (Integer sampleSize : sampleSizeList) {
                StageParamDTO stageParamDTO = new StageParamDTO();
                stageParamDTO.setSampleSize(sampleSize);
                stageParamDTO.setExpCoefficient(expCoefficient);
                stageParamDTO.setStageBlacklist(stageBlacklist);
                updateStageResult(stageParamDTO);
                Logger.info("本次更新关卡，经验书系数为" + expCoefficient + "，样本数量为" + sampleSize);
            }
        }
    }

    public void updateStageResult(StageParamDTO stageParamDTO) {
        List<Item> items = itemService.getItemList(stageParamDTO);   //找出对应版本的材料价值
        if (items == null || items.isEmpty()) {
            items = itemService.getBaseItemList();
        }
        items = itemService.ItemValueCal(items, stageParamDTO);  //计算新的新材料价值
        stageCalService.stageResultCal(items, stageParamDTO);      //用新材料价值计算新关卡效率
        Logger.info("V2关卡效率更新成功");
    }


    public String saveMaterialValueConfig(Map<String, Object> params) {

        Object oToken = params.get("token");
        if (oToken == null) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        UserInfoVO userInfoByToken = userService.getUserInfoByToken(String.valueOf(oToken));
        Long uid = userInfoByToken.getUid();

        Object oConfig = params.get("config");

        LambdaQueryWrapper<MaterialValueConfig> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(MaterialValueConfig::getUid, uid);
        MaterialValueConfig materialValueConfigByUid = materialValueConfigMapper.selectOne(lambdaQueryWrapper);
        long timeStamp = System.currentTimeMillis();
        if (materialValueConfigByUid == null) {
            MaterialValueConfig materialValueConfig = new MaterialValueConfig();
            materialValueConfig.setUid(uid);
            materialValueConfig.setCreateTime(timeStamp);
            materialValueConfig.setUpdateTime(timeStamp);
            materialValueConfig.setConfig(String.valueOf(oConfig));
            materialValueConfigMapper.insert(materialValueConfig);
        } else {
            materialValueConfigByUid.setUpdateTime(timeStamp);
            materialValueConfigByUid.setConfig(String.valueOf(oConfig));
            materialValueConfigMapper.updateById(materialValueConfigByUid);
        }

        return "更新成功";
    }

    @RedisCacheable(key = "Item:Stage.T3.V2", params = "version")
    public Map<String, Object> getT3RecommendedStageV2(String version) {

        Map<String, Stage> stageMap = stageService.getStageList(null)
                .stream()
                .collect(Collectors.toMap(Stage::getStageId, Function.identity()));

        Map<String, Item> itemMap = itemService.getItemListCache(version)
                .stream()
                .collect(Collectors.toMap(Item::getItemId, Function.identity()));

        //根据itemType将关卡主要掉落信息分组
        Map<String, List<StageResult>> resultGroupByItemType = stageResultMapper
                .selectList(new QueryWrapper<StageResult>()
                        .eq("version", version)
                        .ge("end_time", new Date())
                        .ge("stage_efficiency", 0.6))
                .stream()
                .filter(e -> !"empty".equals(e.getItemSeries()))
                .collect(Collectors.groupingBy(StageResult::getItemSeriesId));

        //查找关卡的详细掉落信息
        Map<String, StageResultDetail> detailGroupByStageId = stageResultDetailMapper
                .selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", version)
                        .ge("end_time", new Date()))
                .stream()
                .filter(e -> e.getRatioRank() == 0)
                .collect(Collectors.toMap((StageResultDetail::getStageId), Function.identity()));


        //要返回前端的数据集合
        List<RecommendedStageVO> recommendedStageVOList = new ArrayList<>();

        for (String itemSeriesId : resultGroupByItemType.keySet()) {
            //每次材料系的推荐关卡的通用掉落信息
            List<StageResult> commonListByItemId = resultGroupByItemType.get(itemSeriesId);
            String itemSeries = commonListByItemId.get(0).getItemSeries();
            //将通用掉落信息按效率倒序排列


            List<StageResultVOV2> stageResultVOV2List = new ArrayList<>();
            for (StageResult common : commonListByItemId) {
                StageResultDetail detail = detailGroupByStageId.get(common.getStageId());
                if (detail == null) continue;
                //将通用结果和详细结果复制到返回结果的实体类中
                StageResultVOV2 stageResultVOV2 = new StageResultVOV2();
                stageResultVOV2.copyByStageResultCommon(common);
                stageResultVOV2.copyByStageResultDetail(detail);
                stageResultVOV2List.add(stageResultVOV2);

                Stage stage = stageMap.get(common.getStageId());
                if (stage == null) continue;
                if (StageType.ACT.equals(stage.getStageType()) || StageType.ACT_REP.equals(stage.getStageType())) {
                    stageResultVOV2.setStageColor(-1);
                    StageResultVOV2 stageResultVOV2Extra = new StageResultVOV2();
                    stageResultVOV2Extra.copyByStageResultCommon(common);
                    stageResultVOV2Extra.copyByStageResultDetail(detail);
                    stageResultVOV2Extra.setStageEfficiency(stageResultVOV2.getStageEfficiency() - 0.072);
                    stageResultVOV2List.add(stageResultVOV2Extra);
                }
                //超过7个关卡退出，前端显示个数有限
            }

            RecommendedStageVO recommendedStageVo = new RecommendedStageVO();
            recommendedStageVo.setItemSeriesId(itemSeriesId);
            recommendedStageVo.setItemSeries(itemSeries);
            recommendedStageVo.setItemType(itemSeries);
            recommendedStageVo.setItemTypeId(itemSeriesId);
            recommendedStageVo.setVersion(version);
            recommendedStageVo.setStageResultList(stageResultVOV2List);
            recommendedStageVOList.add(recommendedStageVo);
            //给关卡赋予等级颜色
            stageResultVOV2List.sort(Comparator.comparing(StageResultVOV2::getStageEfficiency).reversed());
            setStageColor(stageResultVOV2List, itemMap);

        }

        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("updateTime", redisTemplate.opsForValue().get("Item:updateTime"));
        hashMap.put("recommendedStageList", recommendedStageVOList);
        return hashMap;
    }

    private static void setStageColor(List<StageResultVOV2> stageResultVOV2List, Map<String, Item> itemMap) {
        stageResultVOV2List = stageResultVOV2List.stream()
                .filter(e -> e.getStageColor() == null)
                .collect(Collectors.toList());

        stageResultVOV2List.forEach(e -> {
            e.setStageColor(2);
            if (itemMap.get(e.getItemId()) != null && itemMap.get(e.getItemId()).getRarity() == 2) {
                double apExpect = e.getApExpect() * ("30012".equals(e.getItemId()) ? 5 : 4);
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
            if (itemMap.get(e.getItemId()) != null && itemMap.get(e.getItemId()).getRarity() == 2) {
                double apExpect = e.getApExpect() / ("30012".equals(e.getItemId()) ? 5 : 4);
                e.setApExpect(apExpect);
            }
        });
    }


    @RedisCacheable(key = "Item:Stage.T3.V3", params = "version")
    public Map<String, Object> getT3RecommendedStageV3(String version) {

        List<Stage> stageList = stageService.getStageList(null);
        Map<String, Stage> stageMap = stageList
                .stream()
                .collect(Collectors.toMap(Stage::getStageId, Function.identity()));


        //根据itemType将关卡主要掉落信息分组
        List<StageResult> stageResultList = stageResultMapper
                .selectList(new QueryWrapper<StageResult>()
                        .eq("version", version)
                        .ge("end_time", new Date())
                        .ge("stage_efficiency", 0.5)
                        .ne("item_series", "empty"));
        Map<String, List<StageResult>> commonMapByItemType = stageResultList.stream()
                .collect(Collectors.groupingBy(StageResult::getItemSeriesId));

        for (StageResult stageResult : stageResultList){
            System.out.println(stageResult);
        }

            //查找关卡的详细掉落信息
            Map<String, StageResultDetail> detailMapByStageId = stageResultDetailMapper
                    .selectList(new QueryWrapper<StageResultDetail>()
                            .ge("end_time", new Date())
                            .eq("version", version))
                    .stream()
                    .filter(e -> e.getRatioRank() == 0)
                    .collect(Collectors.toMap((StageResultDetail::getStageId), Function.identity()));

        //要返回前端的数据集合
        List<RecommendedStageVO> recommendedStageVOList = new ArrayList<>();

        for (String itemSeriesId : commonMapByItemType.keySet()) {
            //每次材料系的推荐关卡的通用掉落信息
            List<StageResult> commonListByItemId = commonMapByItemType.get(itemSeriesId);
            String itemSeries = commonListByItemId.get(0).getItemSeries();
            //将通用掉落信息按效率倒序排列
            commonListByItemId.sort(Comparator.comparing(StageResult::getStageEfficiency).reversed());

            List<StageResultVOV2> stageResultVOV2List = new ArrayList<>();
            for (StageResult common : commonListByItemId) {
                StageResultDetail detail = detailMapByStageId.get(common.getStageId());
                if (detail == null) continue;
                //将通用结果和详细结果复制到返回结果的实体类中
                StageResultVOV2 stageResultVOV2 = new StageResultVOV2();
                stageResultVOV2.copyByStageResultCommon(common);
                stageResultVOV2.copyByStageResultDetail(detail);
                stageResultVOV2List.add(stageResultVOV2);

                Stage stage = stageMap.get(common.getStageId());
                if (stage == null) continue;

                stageResultVOV2.setZoneName(stage.getZoneName());
            }

            RecommendedStageVO recommendedStageVo = new RecommendedStageVO();
            recommendedStageVo.setItemSeriesId(itemSeriesId);
            recommendedStageVo.setItemSeries(itemSeries);
            recommendedStageVo.setItemType(itemSeries);
            recommendedStageVo.setItemTypeId(itemSeriesId);
            recommendedStageVo.setVersion(version);
            recommendedStageVo.setStageResultList(stageResultVOV2List);


            recommendedStageVOList.add(recommendedStageVo);

        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("updateTime", redisTemplate.opsForValue().get("Item:updateTime"));
        map.put("recommendedStageList", recommendedStageVOList);
        return map;
    }


    @RedisCacheable(key = "Item:Stage.T2.V2", params = "version")
    public List<RecommendedStageVO> getT2RecommendedStage(String version) {

        List<RecommendedStageVO> recommendedStageVOList = new ArrayList<>();

        Map<String, Item> itemMap = itemService.getItemListCache(version)
                .stream()
                .collect(Collectors.toMap(Item::getItemId, Function.identity()));

        //查询关卡通用掉落信息,根据物品id分组
        Map<String, StageResult> resultSampleMap = stageResultMapper
                .selectList(new QueryWrapper<StageResult>()
                        .eq("version", version))
                .stream()
                .collect(Collectors.toMap(StageResult::getStageId, Function.identity()));

        //查询详细掉落信息
        Map<String, List<StageResultDetail>> collect = stageResultDetailMapper
                .selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", version)
                        .ge("end_time", new Date()))
                .stream()
                .filter(e -> {
                    if (itemMap.get(e.getItemId()) == null) return false;
                    return itemMap.get(e.getItemId()).getRarity() == 2 && e.getItemId().startsWith("300");
                })
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
                StageResult common = resultSampleMap.get(detail.getStageId());
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
            recommendedStageVo.setItemSeriesId(itemId);
            recommendedStageVo.setItemSeries(itemName);
            recommendedStageVo.setItemType(itemName);
            recommendedStageVo.setItemTypeId(itemId);
            recommendedStageVo.setStageResultList(stageResultVOV2List);
            recommendedStageVOList.add(recommendedStageVo);
        }
        return recommendedStageVOList;
    }


    @RedisCacheable(key = "Item:Stage.Orundum.V2")
    public List<OrundumPerApResultVO> getOrundumRecommendedStage(String version) {
        List<OrundumPerApResultVO> orundumPerApResultVOList = new ArrayList<>();

        Map<String, Item> itemMap = itemService.getItemListCache(version)
                .stream()
                .collect(Collectors.toMap(Item::getItemId, Function.identity()));

        //查询关卡通用掉落信息,根据物品id分组
        Map<String, StageResult> resultCommonMap = stageResultMapper
                .selectList(new QueryWrapper<StageResult>()
                        .eq("version", version))
                .stream().collect(Collectors.toMap(StageResult::getStageId, Function.identity()));

        //查询关卡信息
        Map<String, Stage> stageMap = stageService.getStageMapKeyIsStageId();

        stageResultDetailMapper.selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", version)
                        .ge("end_time", new Date())).stream()
                .filter(e -> {
                    if (itemMap.get(e.getItemId()) == null) return false;
                    return itemMap.get(e.getItemId()).getRarity() < 3;
                })
                .collect(Collectors.groupingBy(StageResultDetail::getStageId))
                .forEach((stageId, list) -> {
                    Stage stage = stageMap.get(stageId);
                    Map<String, Double> calResult = orundumPerApCal(list, stage.getApCost());
                    if (calResult.get("orundumPerAp") > 0.2) {
                        StageResult stageResult = resultCommonMap.get(stageId);
                        OrundumPerApResultVO resultVo = OrundumPerApResultVO.builder()
                                .stageCode(stageResult.getStageCode())
                                .zoneName(stage.getZoneName())
                                .stageEfficiency(stageResult.getStageEfficiency())
                                .orundumPerAp(calResult.get("orundumPerAp"))
                                .lMDCost(calResult.get("LMDCost"))
                                .orundumPerApEfficiency(calResult.get("orundumPerAp") / 1.09)
                                .stageType(stage.getStageType())
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


    //    @RedisCacheable(key = "Item:Stage.ACT.V2", params = "version")
    public List<ActStageVO> getHistoryActStage(String version) {

        Map<String, Item> itemMap = itemService.getItemListCache(version)
                .stream()
                .collect(Collectors.toMap(Item::getItemId, Function.identity()));

        Map<String, List<Stage>> stageGroupByZoneName = stageService.getStageList(null)
                .stream()
                .filter(e -> StageType.ACT.equals(e.getStageType()) || StageType.ACT_REP.equals(e.getStageType()))
                .collect(Collectors.groupingBy(Stage::getZoneName));

        Map<String, StageResult> resultCommonMap = stageResultMapper.selectList(new QueryWrapper<StageResult>()
                        .eq("version", version))
                .stream()
                .collect(Collectors.toMap(StageResult::getStageId, Function.identity()));

        Map<String, StageResultDetail> resultDetailMap = stageResultDetailMapper.selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", version))
                .stream()
                .filter(e -> e.getRatioRank() == 0)
                .collect(Collectors.toMap(StageResultDetail::getStageId, Function.identity()));

        List<ActStageVO> actStageVOList = new ArrayList<>();
        for (String zoneName : stageGroupByZoneName.keySet()) {
            List<Stage> stageList = stageGroupByZoneName.get(zoneName);
            Stage stage = stageList.get(0);
            ActStageVO actStageVo = new ActStageVO();
            actStageVo.setZoneName(zoneName);
            actStageVo.setStageType(stage.getStageType());
            actStageVo.setEndTime(stage.getEndTime().getTime());
            actStageVo.setActStageList(getActStageResultList(stageList, resultCommonMap, resultDetailMap, itemMap));
            if (!actStageVo.getActStageList().isEmpty()) {
                actStageVOList.add(actStageVo);
            }
        }

        actStageVOList.sort(Comparator.comparing(ActStageVO::getEndTime).reversed());

        return actStageVOList;
    }


    private List<ActStageResultVO> getActStageResultList(List<Stage> stageList,
                                                         Map<String, StageResult> resultCommonMap,
                                                         Map<String, StageResultDetail> resultDetailMap,
                                                         Map<String, Item> itemMap) {


        stageList.sort(Comparator.comparing(Stage::getStageId).reversed());

        List<ActStageResultVO> actStageResultVOList = new ArrayList<>();
        for (Stage stage : stageList) {
            String stageId = stage.getStageId();
            ActStageResultVO actStageResultVO = new ActStageResultVO();
            if (resultCommonMap.get(stageId) != null && resultDetailMap.get(stageId) != null) {
                StageResult common = resultCommonMap.get(stageId);
                actStageResultVO.setStageId(common.getStageId());
                actStageResultVO.setStageCode(common.getStageCode());
                actStageResultVO.setItemName(common.getItemSeries());
                actStageResultVO.setItemId(common.getItemSeriesId());
                actStageResultVO.setStageEfficiency(common.getStageEfficiency());
                StageResultDetail detail = resultDetailMap.get(stageId);
                actStageResultVO.setApExpect(detail.getApExpect());
                actStageResultVO.setKnockRating(detail.getKnockRating());
                actStageResultVO.setKnockRating(actStageResultVO.getKnockRating());
                actStageResultVO.setApExpect(actStageResultVO.getApExpect());
                if (itemMap.get(detail.getItemId()) == null) continue;
                if (itemMap.get(detail.getItemId()).getRarity() < 3) continue;
                actStageResultVOList.add(actStageResultVO);

            }

            if (actStageResultVOList.size() > 2) break;
        }


        return actStageResultVOList;
    }


    public List<StageResultVOV2> getStageByZoneName(StageParamDTO stageParamDTO, String zone) {


        Map<String, StageResultDetail> detailMap = stageResultDetailMapper.selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", stageParamDTO.getVersion())
                        .eq("ratio_rank", 0)
                        .ge("end_time", new Date())
                        .like("stage_id", zone))
                .stream()
                .collect(Collectors.toMap(StageResultDetail::getStageId, Function.identity()));


        List<StageResultVOV2> stageResultVOV2List = new ArrayList<>();
        stageResultMapper.selectList(new LambdaQueryWrapper<StageResult>()
                        .eq(StageResult::getVersion, stageParamDTO.getVersion())
                        .ge(StageResult::getEndTime, new Date())
                        .like(StageResult::getStageId, zone)
                        .orderByDesc(StageResult::getStageEfficiency))
                .stream()
                .filter(e -> !e.getItemSeries().equals("empty"))
                .forEach(e -> {
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


    public Map<String, StageResultDetailVO> getAllStageResult(String version) {

        Map<String, Item> itemMap = itemService.getItemListCache(version)
                .stream()
                .collect(Collectors.toMap(Item::getItemId, Function.identity()));

        Map<String, List<StageResultDetail>> detailMap = stageResultDetailMapper
                .selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", version))
                .stream()
                .collect(Collectors.groupingBy(StageResultDetail::getStageId));

        Map<String, StageResultDetailVO> detailVOMap = new HashMap<>();

        stageResultMapper.selectList(new QueryWrapper<StageResult>()
                        .eq("version", version))
                .forEach(e -> {
                    StageResultDetailVO detailVO = new StageResultDetailVO();
                    detailVO.copyByStageResultCommon(e);
                    List<DropDetailVO> dropDetailVOList = new ArrayList<>();
                    if (detailMap.get(e.getStageId()) != null) {
                        detailMap.get(e.getStageId()).forEach(detail -> {
                            DropDetailVO dropDetailVO = new DropDetailVO();
                            dropDetailVO.copyByStageResultDetail(detail);
                            dropDetailVOList.add(dropDetailVO);
                        });
                    }
                    detailVO.setDropDetailList(dropDetailVOList);
                    detailVOMap.put(e.getStageId(), detailVO);
                });

        return detailVOMap;
    }


    public void backupStageResult() {
        double expCoefficient = 0.625;
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setExpCoefficient(expCoefficient);
        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        Map<String, Object> t3RecommendedStageV2 = getT3RecommendedStageV2(stageParamDTO.getVersion());
        String yyyyMMddHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()); // 设置日期格式

        List<Item> items = itemService.getItemList(stageParamDTO);

        ossService.upload(JsonMapper.toJSONString(t3RecommendedStageV2), "backup/stage/" + yyyyMMdd + "/t3—" + expCoefficient + "—" + yyyyMMddHHmm + ".json");
        ossService.upload(JsonMapper.toJSONString(items), "backup/item/" + yyyyMMdd + "/item—" + expCoefficient + "—" + yyyyMMddHHmm + ".json");
    }


    public List<StageResultVOV2> getStageDetail(String stageId) {
        return null;
    }

    public List<String> resetCache() {

        String[] keyList = new String[]{"Stage.ACT.V2.v2-public-0.625-300",
                "Stage.T2.V2.v2-public-0.625-300",
                "Stage.Orundum.V2",
                "Stage.T3.V2.v2-public-0.625-300"};
        List<String> messageList = new ArrayList<>();

        for (String key : keyList) {
            messageList.add(deleteCache(key));
        }


        return messageList;
    }

    private String deleteCache(String key) {
        if (Boolean.TRUE.equals(redisTemplate.delete(key))) {
            return key + "删除成功";
        }
        return key + "删除失败";
    }


    //--------------------------------兼容旧接口--------------------------------


    @RedisCacheable(key = "Item:Stage.T3.V1", params = "version")
    public List<List<StageResultVO>> getT3RecommendedStageV1(String version) {

        Map<String, Stage> stageMap = stageService.getStageList(null)
                .stream()
                .collect(Collectors.toMap(Stage::getStageId, Function.identity()));


        //根据itemType将关卡主要掉落信息分组
        Map<String, List<StageResult>> resultGroupByItemType = stageResultMapper
                .selectList(new QueryWrapper<StageResult>()
                        .eq("version", version)
                        .ge("end_time", new Date())
                        .ge("stage_efficiency", 0.6))
                .stream()
                .filter(e -> !"empty".equals(e.getItemSeries()))
                .collect(Collectors.groupingBy(StageResult::getItemSeriesId));

        //查找关卡的详细掉落信息
        Map<String, StageResultDetail> detailMapByStageId = stageResultDetailMapper
                .selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", version)
                        .ge("end_time", new Date()))
                .stream()
                .filter(e -> e.getRatioRank() == 0)
                .collect(Collectors.toMap((StageResultDetail::getStageId), Function.identity()));

        List<List<StageResultVO>> stageResultVOListList = new ArrayList<>();

        for (String itemTypeId : resultGroupByItemType.keySet()) {
            //每次材料系的推荐关卡的通用掉落信息
            List<StageResult> resultList = resultGroupByItemType.get(itemTypeId);
            //将通用掉落信息按效率倒序排列
            resultList.sort(Comparator.comparing(StageResult::getStageEfficiency).reversed());
            List<StageResultVO> stageResultVOList = new ArrayList<>();
            for (StageResult common : resultList) {
                StageResultDetail detail = detailMapByStageId.get(common.getStageId());
                if (detail == null) continue;
                //将通用结果和详细结果复制到返回结果的实体类中
                StageResultVO stageResultVO = new StageResultVO();
                stageResultVO.copyByStageResultCommon(common);
                stageResultVO.copyByStageResultDetail(detail);
                stageResultVOList.add(stageResultVO);

                Stage stage = stageMap.get(common.getStageId());
                if (stage == null) continue;
                if (StageType.ACT.equals(stage.getStageType()) || StageType.ACT_REP.equals(stage.getStageType())) {
                    stageResultVO.setStageColor(-1);
                    StageResultVO stageResultVOExtra = new StageResultVO();
                    stageResultVOExtra.copyByStageResultCommon(common);
                    stageResultVOExtra.copyByStageResultDetail(detail);
                    stageResultVOExtra.setStageEfficiency((stageResultVO.getStageEfficiency() - 0.072) * 100);
                    stageResultVOList.add(stageResultVOExtra);
                }
                stageResultVO.setStageEfficiency(stageResultVO.getStageEfficiency() * 100);
                //超过7个关卡退出，前端显示个数有限
                if (stageResultVOList.size() > 7) break;
            }

//            stageResultVOList.forEach(e -> System.out.println(e.getItemName() + "--" + e.getStageCode() + "--" + e.getStageEfficiency()));
            //给关卡赋予等级颜色
            setStageResultVoColor(stageResultVOList);
            stageResultVOListList.add(stageResultVOList);
        }

        return stageResultVOListList;
    }

    private static void setStageResultVoColor(List<StageResultVO> stageResultList) {
        if (stageResultList.size() < 1) return;

        stageResultList = stageResultList.stream()
                .filter(e -> e.getStageColor() == null)
                .collect(Collectors.toList());

//        for (StageResultVO stageResult : stageResultList) {
//            stageResult.setStageColor(2);
//            if (stageResult.getItemRarity() == 2) {
//                double apExpect = stageResult.getApExpect() * 4 + 200 * 0.0036 - 1.17;
//                if ("固源岩".equals(stageResult.getMain())) {
//                    apExpect = stageResult.getApExpect() * 5 + 200 * 0.0036 - 1.17;
//                }
//                stageResult.setApExpect(apExpect);
//            }
////            stageResult.setStageEfficiency(stageResult.getStageEfficiency() * 100);
//        }

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

//        for (StageResultVO stageResult : stageResultList) {
//            if (stageResult.getItemRarity() == 2) {
//                double apExpect = (stageResult.getApExpect() + 1.17 - 200 * 0.0036) / 4;
//
//                if ("固源岩".equals(stageResult.getMain())) {
//                    apExpect = (stageResult.getApExpect() + 1.17 - 200 * 0.0036) / 5;
//                }
//                stageResult.setApExpect(apExpect);
//            }
//        }
    }


    @RedisCacheable(key = "Item:Stage.T2.V1", params = "version")
    public List<List<StageResultVO>> getT2RecommendedStageV1(String version) {

        Map<String, Item> itemMap = itemService.getItemListCache(version)
                .stream()
                .collect(Collectors.toMap(Item::getItemId, Function.identity()));

        //查询关卡通用掉落信息,根据物品id分组
        Map<String, StageResult> resultSampleMap = stageResultMapper
                .selectList(new QueryWrapper<StageResult>()
                        .eq("version", version))
                .stream()
                .collect(Collectors.toMap(StageResult::getStageId, Function.identity()));

        //查询详细掉落信息
        Map<String, List<StageResultDetail>> collect = stageResultDetailMapper
                .selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", version)
                        .ge("end_time", new Date()))
                .stream()
                .filter(e -> {
                    if (itemMap.get(e.getItemId()) == null) return false;
                    return itemMap.get(e.getItemId()).getRarity() == 2 && e.getItemId().startsWith("300");
                })
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
                StageResult common = resultSampleMap.get(detail.getStageId());
                //将通用结果和详细结果复制到返回结果的实体类中
                if (common != null) {
                    stageResultVO.copyByStageResultCommon(common);
                }

                stageResultVO.copyByStageResultDetail(detail);
                stageResultVO.setStageColor(1);
                stageResultVO.setStageEfficiency(stageResultVO.getStageEfficiency() * 100);
                stageResultVOList.add(stageResultVO);
                //超过7个关卡退出，前端显示个数有限
                if (stageResultVOList.size() > 7) break;
            }
            stageResultVOListList.add(stageResultVOList);
        }

        return stageResultVOListList;
    }


}
