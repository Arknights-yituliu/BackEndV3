package com.lhs.service.material;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.enums.StageType;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.material.PenguinMatrixDTO;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.dto.material.StageCalculationParametersDTO;
import com.lhs.entity.dto.material.StageResultTmpDTO;
import com.lhs.entity.po.common.DataCache;
import com.lhs.entity.po.material.*;
import com.lhs.entity.vo.material.RecommendedStageVO;
import com.lhs.entity.vo.material.StageResultVOV2;
import com.lhs.mapper.DataCacheMapper;
import com.lhs.mapper.material.QuantileMapper;
import com.lhs.mapper.material.StageResultMapper;
import com.lhs.mapper.material.StageResultDetailMapper;
import com.lhs.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StageCalService {

    private final StageService stageService;
    private final QuantileMapper quantileMapper;
    private final ItemService itemService;
    private final StageResultMapper stageResultMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private final StageResultDetailMapper stageResultDetailMapper;
    private final IdGenerator idGenerator;

    private final UserService userService;
    private final DataCacheMapper dataCacheMapper;



    public StageCalService(StageService stageService,
                           QuantileMapper quantileMapper,
                           ItemService itemService,
                           StageResultMapper stageResultMapper,
                           RedisTemplate<String, Object> redisTemplate,
                           StageResultDetailMapper stageResultDetailMapper,
                           UserService userService, DataCacheMapper dataCacheMapper) {
        this.stageService = stageService;
        this.quantileMapper = quantileMapper;
        this.itemService = itemService;
        this.stageResultMapper = stageResultMapper;
        this.redisTemplate = redisTemplate;
        this.stageResultDetailMapper = stageResultDetailMapper;
        this.userService = userService;
        this.dataCacheMapper = dataCacheMapper;


        idGenerator = new IdGenerator(1L);
    }


    private static final JsonNode ITEM_SERIES_TABLE = JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.Config + "item_series_table.json"));

    public void updateStageResultByTaskConfig() {
        String read = FileUtil.read(ConfigUtil.Config + "stage_task_config_v3.json");
        if (read == null) {
            LogUtils.error("更新关卡配置文件为空");
            return;
        }

        List<StageConfigDTO> stageConfigDTOList =  JsonMapper.parseObject(read, new TypeReference<>() {
        });

        for(StageConfigDTO stageConfigDTO:stageConfigDTOList){
            //计算新的新材料价值
            List<Item> itemList = itemService.calculatedItemValue(stageConfigDTO);
            //用新材料价值计算新关卡效率
            StageResultTmpDTO stageResultTmpDTO = calculatedStageEfficiency(itemList, stageConfigDTO);
            LogUtils.info("关卡效率更新成功，版本号 {} "+stageConfigDTO.getVersionCode());
            //保存到数据库
            saveResultToDB(stageResultTmpDTO,stageConfigDTO.getVersionCode());
            LogUtils.info("本次更新关卡，经验书系数为" + stageConfigDTO.getExpCoefficient() + "，样本数量为" + stageConfigDTO.getSampleSize());
        }
    }


    public List<RecommendedStageVO> createRecommendedStageVOList(Map<String, List<StageResult>> commonMapByItemType,
                                                                 Map<String, StageResultDetail> detailMapByStageId,
                                                                 String version){
        List<Stage> stageList = stageService.getStageList(null);
        Map<String, Stage> stageMap = stageList
                .stream()
                .collect(Collectors.toMap(Stage::getStageId, Function.identity()));

        List<RecommendedStageVO> recommendedStageVOList = new ArrayList<>();

        for (String itemSeriesId : commonMapByItemType.keySet()) {
            //每次材料系的推荐关卡的通用掉落信息
            List<StageResult> commonListByItemId = commonMapByItemType.get(itemSeriesId);
            String itemSeries = commonListByItemId.get(0).getItemSeries();
            //将通用掉落信息按效率倒序排列
            commonListByItemId.sort(Comparator.comparing(StageResult::getStageEfficiency).reversed());

            List<StageResultVOV2> stageResultVOV2List = new ArrayList<>();
            for (StageResult common : commonListByItemId) {
                Stage stage = stageMap.get(common.getStageId());
                if (stage == null) {
                    continue;
                }
                StageResultDetail detail = detailMapByStageId.get(common.getStageId());
                if (detail == null) {
                    continue;
                }
                //将通用结果和详细结果复制到返回结果的实体类中
                StageResultVOV2 stageResultVOV2 = new StageResultVOV2();
                stageResultVOV2.copyByStageResultCommon(common);
                stageResultVOV2.copyByStageResultDetail(detail);
                stageResultVOV2List.add(stageResultVOV2);


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


        return recommendedStageVOList;
    }



    public StageResultTmpDTO calculatedStageEfficiency(List<Item> itemList, StageConfigDTO stageConfigDTO) {

        Integer sampleSize = stageConfigDTO.getSampleSize();
        String version = stageConfigDTO.getVersionCode();

        //物品信息  <itemId,Item>
        Map<String, Item> itemMap = itemList.stream().collect(Collectors.toMap(Item::getItemId, Function.identity()));

        //关卡信息 <stageId,stage>
        Map<String, Stage> stageMap = stageService.getStageList(new QueryWrapper<Stage>()
                        .notLike("stage_id", "tough"))
                .stream()
                .collect(Collectors.toMap(Stage::getStageId, Function.identity()));


        Map<String, String> stageBlackMap = stageConfigDTO.getStageBlackMap();

        //获得一个企鹅物流掉落数据的Map对象，key为关卡id，value为关卡掉落集合，过滤掉低于样本阈值的数据，合并标准和磨难难度的关卡掉落
        Map<String, List<PenguinMatrixDTO>> groupByStageId =  PenguinMatrixCollect
                .filterAndMergePenguinData("penguin",itemMap, stageMap,stageBlackMap, sampleSize);



        StageResultTmpDTO stageResultTmpDTO = new StageResultTmpDTO();

        for (String stageId : groupByStageId.keySet()) {


            //来自企鹅物流的关卡掉落
            List<PenguinMatrixDTO> stageDropList = groupByStageId.get(stageId);

            //关卡信息
            Stage stage = stageMap.get(stageId);

            //计算中传递的参数对象
            StageCalculationParametersDTO parametersDTO = new StageCalculationParametersDTO();

            //由于企鹅对于关卡本身的龙门币不进行统计，手动向企鹅的关卡掉落增加龙门币和商店龙门币
//            PenguinMatrixCollect.stageDropAddLMD(stageDropList, stage);

            //临时关卡详情集合
            List<StageResultDetail> tempResultDetailList = new ArrayList<>();

            //计算关卡期望产出并获取一个期望产出
            countDropApValueAndListStageResultDetail(stage, tempResultDetailList,
                    stageDropList, itemMap, parametersDTO, version);

            //计算关卡每种产出的占比和判断关卡的材料系列
            calculatedDropRatioAndJudgmentItemSeries(tempResultDetailList,
                    parametersDTO, stageResultTmpDTO);

            //计算关卡每种产出的占比和判断关卡的材料系列
            saveItemIterationValue(stageResultTmpDTO, parametersDTO, stage);

            //创建关卡结果对象
            setStageResult(stageResultTmpDTO, tempResultDetailList,
                    parametersDTO, stage, version);
        }

        List<ItemIterationValue> iterationValueList = createIterationValueList(stageResultTmpDTO,
                itemMap, version);

        stageResultTmpDTO.setItemIterationValueList(iterationValueList);

        return stageResultTmpDTO;
    }



    /**
     * 计算关卡期望产出理智价值，并向临时的关卡结果集合中的元素赋予一些固定值
     *
     * @param stage                         关卡信息
     * @param temporaryResultDetailList     临时关卡详情集合
     * @param stageDropList                 关卡掉落集合
     * @param itemMap                       材料表
     * @param stageCalculationParametersDTO 关卡临时结果
     * @param version                       版本号
     */
    private void countDropApValueAndListStageResultDetail(Stage stage, List<StageResultDetail> temporaryResultDetailList,
                                                          List<PenguinMatrixDTO> stageDropList,
                                                          Map<String, Item> itemMap,
                                                          StageCalculationParametersDTO stageCalculationParametersDTO,
                                                          String version) {

        List<QuantileTable> quantileTables = quantileMapper.selectList(null);


        Double apCost = Double.valueOf(stage.getApCost());
        String stageId = stage.getStageId();

        double countStageDropApValue = 0.0;

        //计算关卡掉落的一些详细信息
        for (PenguinMatrixDTO stageDrop : stageDropList) {

            //该条掉落物品的详细信息
            Item item = itemMap.get(stageDrop.getItemId());

            //关卡掉落详情
            StageResultDetail detail = new StageResultDetail();

            //材料掉率
            Double knockRating = ((double) stageDrop.getQuantity() / (double) stageDrop.getTimes());

            //关卡置信度
            Double sampleConfidence = sampleConfidence(stageDrop.getTimes(), apCost,
                    item.getItemValueAp(), knockRating, quantileTables);

            //计算该条掉落的期望产出理智价值
            double result = item.getItemValueAp() * knockRating;

            //期望理智
            Double apExpect = apCost / knockRating;
            detail.setId(idGenerator.nextId());
            detail.setStageId(stageId);
            detail.setItemId(item.getItemId());
            detail.setItemName(item.getItemName());
            detail.setKnockRating(knockRating);
            detail.setApExpect(apExpect);
            detail.setResult(result);
            detail.setSampleSize(stageDrop.getTimes());
            detail.setSampleConfidence(sampleConfidence);
            detail.setEndTime(stage.getEndTime());
            detail.setVersion(version);
            temporaryResultDetailList.add(detail);
            countStageDropApValue += result;
        }

        stageCalculationParametersDTO.setCountStageDropApValue(countStageDropApValue);
    }




    private List<ItemIterationValue> createIterationValueList(StageResultTmpDTO stageResultTmpDTO,
                                                              Map<String, Item> itemMap,
                                                              String version) {
        List<ItemIterationValue> iterationValueList = new ArrayList<>();
        stageResultTmpDTO.getItemIterationValueMap().forEach((itemTypeId, iterationValue) -> {
            ItemIterationValue itemIterationValue = new ItemIterationValue();
            String itemName = itemMap.get(itemTypeId).getItemName();
            itemIterationValue.setItemName(itemName);
            itemIterationValue.setIterationValue(iterationValue);
            itemIterationValue.setVersion(version);
            itemIterationValue.setItemId(itemTypeId);
            iterationValueList.add(itemIterationValue);
            LogUtils.info(String.format("%-8s", itemName) + "的迭代系数是：" + 1 / iterationValue);
        });

        return iterationValueList;
    }

    private void setStageResult(StageResultTmpDTO stageResultTmpDTO,
                                List<StageResultDetail> temporaryResultDetailList,
                                StageCalculationParametersDTO stageCalculationParametersDTO,
                                Stage stage, String version) {

        String stageId = stage.getStageId();
        String secondaryItemId = stageCalculationParametersDTO.getSecondaryItemId();
        String itemSeries = stageCalculationParametersDTO.getItemSeries();
        String itemSeriesId = stageCalculationParametersDTO.getItemSeriesId();
        Double stageEfficiency = stageCalculationParametersDTO.getStageEfficiency();
        Integer apCost = stage.getApCost();


        StageResult stageResult = new StageResult();
        stageResult.setId(idGenerator.nextId());
        stageResult.setStageId(stageId);
        stageResult.setStageCode(stage.getStageCode());

        stageResult.setSecondaryItemId(secondaryItemId);
        stageResult.setEndTime(stage.getEndTime());
        stageResult.setVersion(version);
        stageResult.setItemSeries(itemSeries);
        stageResult.setItemSeriesId(itemSeriesId);
        stageResult.setStageEfficiency(stageEfficiency);
        stageResult.setEndTime(stage.getEndTime());

        stageResult.setSpm(stage.getSpm());
        calMainItemRatio(stageResult, apCost, temporaryResultDetailList);

//            Log.info(stage.getStageCode()+" {} 主产物:"+mainItemName+" {} 副产物："+secondaryItemName+" {} 关卡效率："+stageEfficiency);
        stageResultTmpDTO.addStageResult(stageResult);
    }

    /**
     * 保存每种材料系列的下一轮价值迭代值
     *
     * @param stageResultTmpDTO             计算结果
     * @param stageCalculationParametersDTO 关卡计算临时变量
     * @param stage                         关卡信息
     */
    private void saveItemIterationValue(StageResultTmpDTO stageResultTmpDTO,
                                        StageCalculationParametersDTO stageCalculationParametersDTO,
                                        Stage stage) {
        String stageType = stage.getStageType();

        Integer apCost = stage.getApCost();
        double countStageDropApValue = stageCalculationParametersDTO.getCountStageDropApValue();
        Double stageEfficiency = countStageDropApValue / apCost;


        //计算战备资源活动的效率
//        if (stageId.startsWith("main")) {
//            countStageDropApValue += apCost / (1000.0 / 6) * 11.28;
//            apCost -= apCost / 200 * 10;
//            stageEfficiency = countStageDropApValue / apCost;
//        }

        stageCalculationParametersDTO.setStageEfficiency(stageEfficiency);
        String itemSeriesId = stageCalculationParametersDTO.getItemSeriesId();

        if (StageType.MAIN.equals(stageType) || StageType.ACT_PERM.equals(stageType)) {
            if (!"empty".equals(itemSeriesId)) {
                Double maxStageEfficiency = stageResultTmpDTO.getItemIterationValueByItemSeriesId(itemSeriesId);
                if(maxStageEfficiency==null){
                    stageResultTmpDTO.putItemIterationValue(itemSeriesId, stageEfficiency);
                    return;
                }
                if (maxStageEfficiency < stageEfficiency) {
                    stageResultTmpDTO.putItemIterationValue(itemSeriesId, stageEfficiency);
                }
            }
        }
    }

    /**
     * 计算关卡每种产出的占比和判断关卡的材料系列
     *
     * @param temporaryResultDetailList     临时结果
     * @param stageCalculationParametersDTO 关卡临时结果
     * @param stageResultTmpDTO             临时结果
     */
    private void calculatedDropRatioAndJudgmentItemSeries(List<StageResultDetail> temporaryResultDetailList,
                                                          StageCalculationParametersDTO stageCalculationParametersDTO,
                                                          StageResultTmpDTO stageResultTmpDTO) {
        //将这个临时集合倒序
        temporaryResultDetailList.sort(Comparator.comparing(StageResultDetail::getResult).reversed());

        for (int i = 0; i < temporaryResultDetailList.size(); i++) {
            StageResultDetail detail = temporaryResultDetailList.get(i);
            Double countStageDropApValue = stageCalculationParametersDTO.getCountStageDropApValue();
            detail.setRatio(detail.getResult() / countStageDropApValue);
            detail.setRatioRank(i);
            String itemId = detail.getItemId();
            //占比最多的材料就是这关的主要掉落物
            if (i == 0) {
                //找到这个材料所属的材料系列
                if (ITEM_SERIES_TABLE.get(itemId) != null) {
                    stageCalculationParametersDTO.setItemSeries(ITEM_SERIES_TABLE.get(itemId).get("series").asText());
                    stageCalculationParametersDTO.setItemSeriesId(ITEM_SERIES_TABLE.get(itemId).get("seriesId").asText());
                }
            }
            if (i == 1) {
                if (detail.getResult() / countStageDropApValue > 0.1) {
                    stageCalculationParametersDTO.setSecondaryItemId(itemId);
                }
            }
            stageResultTmpDTO.addStageResultDetail(detail);
        }
    }






    /**
     * @param common     关卡部分通用计算结果
     * @param detailList 关卡每种掉落物品的详细数据
     */
    private void calMainItemRatio(StageResult common, double apCost, List<StageResultDetail> detailList) {

        String itemType = common.getItemSeries();  //关卡掉落材料所属类型   比如1-7，4-6这种同属”固源岩组“类
        if ("empty".equals(itemType)) {
            common.setLeT4Efficiency(0.0);  //等级1-4的材料占比
            common.setLeT3Efficiency(0.0); //等级1-3的材料占比
            common.setLeT2Efficiency(0.0); //等级1-2的材料占比
            return;
        }

        //获取材料的上下位材料
        JsonNode itemTypeTable = JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.Config + "item_type_table.json"));
        JsonNode itemTypeNode = itemTypeTable.get(itemType);

        //该系材料的每种等级材料在关卡中的占比
        double rarity1Ratio = 0.0;
        double rarity2Ratio = 0.0;
        double rarity3Ratio = 0.0;
        double rarity4Ratio = 0.0;


        //计算该系材料的每种等级材料在关卡中的占比
        for (StageResultDetail stageResultDetail : detailList) {
            if (itemTypeNode.get(stageResultDetail.getItemName()) != null) {
                JsonNode item = itemTypeNode.get(stageResultDetail.getItemName());
                int rarity = item.get("rarity").asInt();
                if (rarity == 1) {
                    rarity1Ratio = stageResultDetail.getResult() / apCost;
                }
                if (rarity == 2) {
                    rarity2Ratio = stageResultDetail.getResult() / apCost;
                }
                if (rarity == 3) {
                    rarity3Ratio = stageResultDetail.getResult() / apCost;
                }
                if (rarity == 4) {
                    rarity4Ratio = stageResultDetail.getResult() / apCost;
                }
            }
        }

        common.setLeT4Efficiency(rarity4Ratio + rarity3Ratio + rarity2Ratio + rarity1Ratio);  //等级1-4的材料占比
        common.setLeT3Efficiency(rarity3Ratio + rarity2Ratio + rarity1Ratio); //等级1-3的材料占比
        common.setLeT2Efficiency(rarity2Ratio + rarity1Ratio); //等级1-2的材料占比
    }




    private Double sampleConfidence(Integer penguinDataTimes, Double apCost, Double itemValue, Double probability, List<QuantileTable> quantileTables) {
        Double quantileValue = 0.03 * apCost / itemValue / Math.sqrt(1 * probability * (1 - probability) / (penguinDataTimes - 1));
        if (quantileValue >= 3.09023 || Double.isNaN(quantileValue)) return 99.9;
        List<QuantileTable> collect = quantileTables.stream()
                .filter(quantileTable -> quantileTable.getValue() <= quantileValue).toList();
        return (collect.get(collect.size() - 1).getSection() * 2 - 1) * 100;
    }

    private void saveResultToDB(StageResultTmpDTO stageResultTmpDTO,
                                String version) {

        stageResultDetailMapper.delete(new QueryWrapper<StageResultDetail>().eq("version", version));
        stageResultMapper.delete(new QueryWrapper<StageResult>().eq("version", version));
        itemService.deleteItemIterationValue(version);

        List<StageResultDetail> stageResultDetailList = stageResultTmpDTO.getStageResultDetailList();
        List<StageResult> stageResultList = stageResultTmpDTO.getStageResultList();
        List<ItemIterationValue> itemIterationValueList = stageResultTmpDTO.getItemIterationValueList();


        itemService.saveItemIterationValue(itemIterationValueList);

        List<StageResultDetail> insertList = new ArrayList<>();
        for (StageResultDetail stageResultDetail : stageResultDetailList) {
            insertList.add(stageResultDetail);
            if (insertList.size() == 2000) {
                stageResultDetailMapper.insertBatch(insertList);
                insertList.clear();
            }
        }

        if (!insertList.isEmpty()) {
            stageResultDetailMapper.insertBatch(insertList);
        }

        redisTemplate.opsForValue().set("Item:updateTime", new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));

        LogUtils.info("本次批量插入关卡掉落详细数据条数：" + stageResultDetailList.size());
        stageResultMapper.insertBatch(stageResultList);
        LogUtils.info("本次批量插入关卡通用掉落数据条数：" + stageResultList.size());
    }

}
