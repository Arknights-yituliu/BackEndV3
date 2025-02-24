package com.lhs.service.material;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.enums.StageType;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.material.*;
import com.lhs.entity.vo.material.*;
import com.lhs.mapper.material.StageResultMapper;
import com.lhs.mapper.material.StageResultDetailMapper;
import com.lhs.service.user.UserService;
import com.lhs.service.util.DataCacheService;
import com.lhs.service.util.OSSService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
    private final DataCacheService dataCacheService;

    public StageResultService(StageResultDetailMapper stageResultDetailMapper,
                              StageResultMapper stageResultMapper,
                              ItemService itemService,
                              StageCalService stageCalService,
                              StageService stageService,
                              OSSService ossService,
                              RedisTemplate<String, Object> redisTemplate,
                              UserService userService, DataCacheService dataCacheService) {
        this.stageResultDetailMapper = stageResultDetailMapper;
        this.stageResultMapper = stageResultMapper;
        this.itemService = itemService;
        this.stageCalService = stageCalService;
        this.stageService = stageService;
        this.ossService = ossService;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        this.dataCacheService = dataCacheService;
    }

    /**
     *
     * @param stageConfigDTO 关卡参数
     * @return  推荐关卡数据
     */
    @RedisCacheable(key = "Item:RecommendedStage", paramOrMethod ="getVersionCode")
    public Map<String, Object> getRecommendedStage(StageConfigDTO stageConfigDTO) {

        String version = stageConfigDTO.getVersionCode();
        //根据itemType将关卡主要掉落信息分组
        List<StageResult> stageResultList = stageResultMapper
                .selectList(new QueryWrapper<StageResult>()
                        .eq("version", version)
                        .ge("end_time", new Date())
                        .ge("stage_efficiency", 0.5)
                        .ne("item_series", "empty"));

        Map<String, List<StageResult>> commonMapByItemType = stageResultList.stream()
                .collect(Collectors.groupingBy(StageResult::getItemSeriesId));

            //查找关卡的详细掉落信息
            Map<String, StageResultDetail> detailMapByStageId = stageResultDetailMapper
                    .selectList(new QueryWrapper<StageResultDetail>()
                            .ge("end_time", new Date())
                            .eq("version", version))
                    .stream()
                    .filter(e -> e.getRatioRank() == 0)
                    .collect(Collectors.toMap((StageResultDetail::getStageId), Function.identity()));

        //要返回前端的数据集合
        List<RecommendedStageVO> recommendedStageVOList = stageCalService.createRecommendedStageVOList(commonMapByItemType, detailMapByStageId, version);

        HashMap<String, Object> map = new HashMap<>();
        map.put("updateTime", redisTemplate.opsForValue().get("Item:updateTime"));
        map.put("recommendedStageList", recommendedStageVOList);
        return map;
    }









    @RedisCacheable(key = "Item:Stage.Orundum", paramOrMethod ="getVersionCode")
    public List<OrundumPerApResultVO> getOrundumRecommendedStage(StageConfigDTO stageConfigDTO) {
        String version = stageConfigDTO.getVersionCode();
        List<OrundumPerApResultVO> orundumPerApResultVOList = new ArrayList<>();

        Map<String, Item> itemMap = itemService.getItemMapCache(stageConfigDTO);
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


    @RedisCacheable(key = "Item:Stage.ACT", paramOrMethod ="getVersionCode")
    public List<ActStageVO> getHistoryActStage(StageConfigDTO stageConfigDTO) {
        String version = stageConfigDTO.getVersionCode();
        Map<String, Item> itemMap = itemService.getItemMapCache(stageConfigDTO);

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


        stageList.sort(Comparator.comparing(Stage::getStageId));

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


    public List<StageResultVOV2> getStageByZoneName(StageConfigDTO stageConfigDTO, String zone) {


        Map<String, StageResultDetail> detailMap = stageResultDetailMapper.selectList(new QueryWrapper<StageResultDetail>()
                        .eq("version", stageConfigDTO.getVersionCode())
                        .eq("ratio_rank", 0)
                        .ge("end_time", new Date())
                        .like("stage_id", zone))
                .stream()
                .collect(Collectors.toMap(StageResultDetail::getStageId, Function.identity()));


        List<StageResultVOV2> stageResultVOV2List = new ArrayList<>();
        stageResultMapper.selectList(new LambdaQueryWrapper<StageResult>()
                        .eq(StageResult::getVersion, stageConfigDTO.getVersionCode())
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


    public Map<String, StageResultDetailVO> getAllStageResult(StageConfigDTO stageConfigDTO) {
        String version = stageConfigDTO.getVersionCode();



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









}
