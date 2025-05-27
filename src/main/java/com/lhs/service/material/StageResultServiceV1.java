package com.lhs.service.material;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.enums.StageType;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.material.Item;
import com.lhs.entity.po.material.Stage;
import com.lhs.entity.po.material.StageResult;
import com.lhs.entity.po.material.StageResultDetail;
import com.lhs.entity.vo.material.RecommendedStageVO;
import com.lhs.entity.vo.material.StageResultVO;
import com.lhs.entity.vo.material.StageResultVOV2;
import com.lhs.mapper.material.StageResultDetailMapper;
import com.lhs.mapper.material.StageResultMapper;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StageResultServiceV1 {

    private final StageResultDetailMapper stageResultDetailMapper;
    private final StageResultMapper stageResultMapper;
    private final ItemService itemService;

    private final StageService stageService;

    private final RedisTemplate<String, Object> redisTemplate;



    public StageResultServiceV1(StageResultDetailMapper stageResultDetailMapper,
                              StageResultMapper stageResultMapper,
                              ItemService itemService,
                              StageService stageService,
                              RedisTemplate<String, Object> redisTemplate) {
        this.stageResultDetailMapper = stageResultDetailMapper;
        this.stageResultMapper = stageResultMapper;
        this.itemService = itemService;
        this.stageService = stageService;
        this.redisTemplate = redisTemplate;

    }

    @RedisCacheable(key = "Item:Stage.T2.V2")
    public List<RecommendedStageVO> getT2RecommendedStage(StageConfigDTO stageConfigDTO) {

        List<RecommendedStageVO> recommendedStageVOList = new ArrayList<>();

        String version = stageConfigDTO.getVersionCode();

        Map<String, Item> itemMap = itemService.getItemMapCache(stageConfigDTO);

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

    /**
     * 旧版API
     * @param stageConfigDTO 关卡参数
     * @return 关卡推荐数据
     */
    @RedisCacheable(key = "Item:Stage.T3.V2")
    public Map<String, Object> getT3RecommendedStageV2(StageConfigDTO stageConfigDTO) {
        String version = stageConfigDTO.getVersionCode();
        Map<String, Stage> stageMap = stageService.getStageList(null)
                .stream()
                .collect(Collectors.toMap(Stage::getStageId, Function.identity()));

        Map<String, Item> itemMap = itemService.getItemMapCache(stageConfigDTO);

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

    @RedisCacheable(key = "Item:Stage.T3.V1")
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


    @RedisCacheable(key = "Item:Stage.T2.V1")
    public List<List<StageResultVO>> getT2RecommendedStageV1(StageConfigDTO stageConfigDTO) {
        String version = stageConfigDTO.getVersionCode();
        Map<String, Item> itemMap = itemService.getItemMapCache(stageConfigDTO);

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
