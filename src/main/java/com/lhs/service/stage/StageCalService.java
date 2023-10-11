package com.lhs.service.stage;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.SerializationUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Log;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.po.stage.*;

import com.lhs.mapper.item.QuantileMapper;
import com.lhs.mapper.item.StageResultMapper;
import com.lhs.entity.dto.stage.PenguinMatrixDTO;
import com.lhs.entity.dto.stage.StageParamDTO;
import com.lhs.mapper.item.StageResultSampleMapper;
import com.lhs.mapper.item.StageResultV2Mapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class StageCalService extends ServiceImpl<StageResultMapper, StageResult> {


    private final StageService stageService;
    private final StageResultMapper stageResultMapper;
    private final QuantileMapper quantileMapper;
    private final ItemService itemService;
    private final StageResultSampleMapper stageResultSampleMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private final StageResultV2Mapper stageResultV2Mapper;

    public StageCalService(StageService stageService, StageResultMapper stageResultMapper, QuantileMapper quantileMapper, ItemService itemService, StageResultSampleMapper stageResultSampleMapper, RedisTemplate<String, Object> redisTemplate, StageResultV2Mapper stageResultV2Mapper) {
        this.stageService = stageService;
        this.stageResultMapper = stageResultMapper;
        this.quantileMapper = quantileMapper;
        this.itemService = itemService;
        this.stageResultSampleMapper = stageResultSampleMapper;
        this.redisTemplate = redisTemplate;
        this.stageResultV2Mapper = stageResultV2Mapper;
    }


    /**
     * 关卡效率计算
     *
     * @param items         材料表
     * @param stageParamDTO 各种计算条件
     * @return map<蓝材料名称 ， 蓝材料对应的常驻最高关卡效率En>
     */
    @Transactional
    public Map<String, Double> stageResultCal(List<Item> items, StageParamDTO stageParamDTO) {
        //读取企鹅物流数据文件
        String penguinStageDataText = FileUtil.read(ApplicationConfig.Penguin + "matrix auto.json");
        if (penguinStageDataText == null) throw new ServiceException(ResultCode.DATA_NONE);
        String itemTypeTableText = FileUtil.read(ApplicationConfig.Item + "itemTypeTable.json");
        JsonNode itemTypeTable = JsonMapper.parseJSONObject(itemTypeTableText);

        //最低样本量
        int sampleSize = stageParamDTO.getSampleSize();
        //本次计算的关卡效率存入的版本号
        String version = stageParamDTO.getVersion();
        //企鹅物流的关卡矩阵
        String matrixText = JsonMapper.parseJSONObject(penguinStageDataText).get("matrix").toPrettyString();
        //将企鹅物流的关卡矩阵转为集合
        List<PenguinMatrixDTO> penguinMatrixDTOList = JsonMapper.parseJSONArray(matrixText,
                new TypeReference<List<PenguinMatrixDTO>>() {
                });

        //合并企鹅物流的标准和磨难关卡的样本
        penguinMatrixDTOList = mergePenguinData(penguinMatrixDTOList);

        //将item表的各项信息转为Map  <itemId,Item>
        Map<String, Item> itemMap = items.stream().collect(Collectors.toMap(Item::getItemId, Function.identity()));

        //将stage的各项信息转为Map <stageId,stage>
        QueryWrapper<Stage> stageQueryWrapper = new QueryWrapper<>();
        stageQueryWrapper.notLike("stage_id", "tough");
        Map<String, Stage> stageMap = stageService.getStageListByWrapper(stageQueryWrapper).stream()
                .collect(Collectors.toMap(Stage::getStageId, Function.identity()));

        //置信度表
        List<QuantileTable> quantileTables = quantileMapper.selectList(null);
        double gachaBoxExpectValue = 22.40;

        List<StageResult> stageResultList = new ArrayList<>();    //关卡效率的计算结果的集合
        Date date = new Date();


        long id = System.currentTimeMillis();


        for (PenguinMatrixDTO penguinData : penguinMatrixDTOList) {
            //样本量小进行下次循环
            if (penguinData.getTimes() < sampleSize) continue;
            //掉落为零进行下次循环
            if (penguinData.getQuantity() == 0) continue;
            //材料不在材料表继续下次循环
            if (itemMap.get(penguinData.getItemId()) == null) continue;
            //关卡不在关卡表继续下次循环
            if (stageMap.get(penguinData.getStageId()) == null) continue;

            if (penguinData.getItemId().startsWith("ap_supply")) {
                continue;
            }

            if (penguinData.getItemId().startsWith("randomMaterial")) {
                continue;
            }

            //关卡信息
            Stage stage = stageMap.get(penguinData.getStageId());
            //材料信息
            Item item = itemMap.get(penguinData.getItemId());

            //材料掉率
            double knockRating = ((double) penguinData.getQuantity() / (double) penguinData.getTimes());
            //关卡置信度
            double sampleConfidence = sampleConfidence(penguinData.getTimes(), stage.getApCost(),
                    item.getItemValueAp(), knockRating, quantileTables);
            //该条掉落记录的结果
            double result = item.getItemValueAp() * knockRating;
            //期望理智
            double apExpect = stage.getApCost() / knockRating;
//            if("1-7".equals(stage.getStageCode())){
//                LogUtil.info(item.getItemName()+"价值："+item.getItemValueAp()+"理智，掉率："+knockRating+"%，产出："+result+"理智");
//            }
            StageResult stageResult = StageResult.builder()
                    .id(id++)
                    .stageId(stage.getStageId())
                    .stageCode(stage.getStageCode())
                    .zoneId(stage.getZoneId())
                    .zoneName(stage.getZoneName())
                    .main("empty")
                    .itemName(item.getItemName())
                    .itemId(item.getItemId())
                    .secondary("empty")
                    .secondaryId("empty")
                    .itemRarity(item.getRarity())
                    .itemType("empty")
                    .sampleSize(penguinData.getTimes())
                    .knockRating(knockRating)
                    .result(result)
                    .apExpect(apExpect)
                    .apCost(stage.getApCost())
                    .version(version)
                    .stageType(stage.getStageType())
                    .sampleConfidence(sampleConfidence)
                    .startTime(stage.getStartTime())
                    .endTime(stage.getEndTime())
                    .updateTime(date)
                    .stageColor(2)
                    .spm(stage.getSpm())
                    .build();

            //类型3是SS关卡，要增加一个计算了商店无限龙门币的关卡效率
            if (stage.getStageType() == 3) {
                StageResult stageResultCopy = SerializationUtils.clone(stageResult);
                stageResultCopy.setId(id++);
                stageResultCopy.setStageId(stage.getStageId() + "_LMD");
                stageResultCopy.setSecondary("龙门币");
                stageResultCopy.setSecondaryId("4001");
                stageResultCopy.setResult(stageResultCopy.getResult());
                stageResultCopy.setStageColor(-1);
                stageResultList.add(stageResultCopy);
            }
            stageResultList.add(stageResult);
        }

        //将上面的计算结果根据关卡id分组
        Map<String, List<StageResult>> resultCollectByStageId = stageResultList.stream()
                .collect(Collectors.groupingBy(StageResult::getStageId));

        //根据材料类型分组
        Map<String, List<StageResult>> resultCollectByItemType = new HashMap<>();


        for (String stageId : resultCollectByStageId.keySet()) {
            List<StageResult> stageResultListByStageId = resultCollectByStageId.get(stageId);

            Stage stage = stageMap.get(stageId.replace("_LMD", ""));
            //关卡消耗体力
            double apCost = stageResultListByStageId.get(0).getApCost();
            //关卡掉落龙门币的价值
            double lmd = 0.0036 * 10 * 1.2 * apCost;
            //关卡掉落的材料等效价值总和
            double apValueSum = stageResultListByStageId.stream().mapToDouble(StageResult::getResult).sum() + lmd;
//            LogUtil.info(stageResultNews.get(0).getStageCode()+"消耗："+apCost+"，理智，产出："+apValueSum+"理智。");
            //关卡理智效率
            double stageEfficiency = apValueSum / apCost;
            //如果是计算了无限龙门币的SS关卡额外加0.072效率
            if (stageId.endsWith("_LMD")) stageEfficiency += 0.072;

            //将关卡根据掉落材料等效价值进行倒序排序
            stageResultListByStageId.sort(Comparator.comparing(StageResult::getResult).reversed());
            //排在第一的即为关卡主要产物
            String main = stageResultListByStageId.get(0).getItemName();
            //查看主产物的材料等级
            Integer itemRarity = stageResultListByStageId.get(0).getItemRarity();
            //材料系列
            String itemType = "empty";
            //如果在18种蓝色精英材料里面，这个关卡要设置材料系列
            if (itemTypeTable.get(main) != null) {
                //是蓝材料直接设置为材料类型
                if (itemRarity == 3) {
                    itemType = main;
                } else {
                    itemType = itemTypeTable.get(main).asText();
                }
            }

            //副产物
            String secondary = "empty";
            String secondaryId = "empty";
            if (stageResultListByStageId.size() > 1) {
                double ratio = stageResultListByStageId.get(1).getResult() / apCost;
                if (ratio > 0.1) {
                    secondary = stageResultListByStageId.get(1).getItemName();
                    secondaryId = stageResultListByStageId.get(1).getItemId();
                }
            }


            for (int i = 0; i < stageResultListByStageId.size(); i++) {
                StageResult stageResult = stageResultListByStageId.get(i);
                double ratio = stageResult.getResult() / apValueSum;
                stageResult.setStageEfficiency(stageEfficiency);
                stageResult.setRatio(ratio);
                stageResult.setRatioRank(i);
                stageResult.setItemType(itemType);
                if (i == 0) {
                    stageResult.setMain(main);
                }


                stageResult.setSecondary(secondary);
                stageResult.setSecondaryId(secondaryId);



                if ("empty".equals(stageResult.getItemType())) {
                    continue;
                }

                if (stageResult.getStageType() > 2) {
                    continue;
                }


                List<StageResult> list = new ArrayList<>();
                if (resultCollectByItemType.get(stageResult.getItemType()) == null) {
                    list.add(stageResult);
                } else {
                    list = resultCollectByItemType.get(stageResult.getItemType());
                    list.add(stageResult);
                }
                resultCollectByItemType.put(stageResult.getItemType(), list);
            }

            resultCollectByStageId.put(stageId, stageResultListByStageId);
        }

        List<ItemIterationValue> iterationValueList = new ArrayList<>();
        Map<String, Double> itemTypeAndMaxPermStageEfficiency = new HashMap<>();
        for (String itemType : resultCollectByItemType.keySet()) {
            List<StageResult> list = resultCollectByItemType.get(itemType);
            list.sort(Comparator.comparing(StageResult::getStageEfficiency).reversed());
            itemTypeAndMaxPermStageEfficiency.put(itemType, list.get(0).getStageEfficiency());
            ItemIterationValue itemIterationValue = new ItemIterationValue();
            itemIterationValue.setItemName(itemType);
            itemIterationValue.setIterationValue(list.get(0).getStageEfficiency());
            itemIterationValue.setVersion(version);
            iterationValueList.add(itemIterationValue);
            Log.info(itemType + "的最优本是" + list.get(0).getStageCode() + "，材料的迭代系数是" + 1 / list.get(0).getStageEfficiency());

        }


        QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("version", version);
        int delete = stageResultMapper.delete(queryWrapper);

        stageResultList.forEach(e -> e.setStageEfficiency(e.getStageEfficiency() * 100));
        if (delete > -1) {
//            stageResultMapper.insertBatch(stageResultList);
            saveBatch(stageResultList);
        }

        itemService.deleteItemIterationValue(version);

        itemService.saveItemIterationValue(iterationValueList);


        return itemTypeAndMaxPermStageEfficiency;
    }

    public Map<String, Double> stageResultCalV2(List<Item> items, StageParamDTO stageParamDTO) {
        String penguinStageDataText = FileUtil.read(ApplicationConfig.Penguin + "matrix auto.json");
        if (penguinStageDataText == null) throw new ServiceException(ResultCode.DATA_NONE);
//        JsonNode itemTypeTable = JsonMapper.parseJSONObject(FileUtil.read(ApplicationConfig.Item + "itemTypeTable.json"));
        JsonNode itemSeriesTable = JsonMapper.parseJSONObject(FileUtil.read(ApplicationConfig.Item + "item_series_table.json"));
        //最低样本量
        int sampleSize = stageParamDTO.getSampleSize();
        //本次计算的关卡效率存入的版本号
        String version = stageParamDTO.getVersion();
        //企鹅物流的关卡矩阵
        String matrixText = JsonMapper.parseJSONObject(penguinStageDataText).get("matrix").toPrettyString();

        //将企鹅物流的关卡矩阵转为集合
        List<PenguinMatrixDTO> penguinMatrixDTOList = JsonMapper.parseJSONArray(matrixText,
                new TypeReference<List<PenguinMatrixDTO>>() {
                });

        //合并企鹅物流的标准和磨难关卡的样本
        penguinMatrixDTOList = mergePenguinData(penguinMatrixDTOList);


        //将item表的各项信息转为Map  <itemId,Item>
        Map<String, Item> itemMap = items.stream().collect(Collectors.toMap(Item::getItemId, Function.identity()));

        Map<String, Stage> stageMap = stageService.getStageListByWrapper(new QueryWrapper<Stage>()
                        .notLike("stage_id", "tough"))
                .stream()
                .collect(Collectors.toMap(Stage::getStageId, Function.identity()));

        //置信度表
        List<QuantileTable> quantileTables = quantileMapper.selectList(null);

        //关卡效率的计算结果
        List<StageResultV2> stageResultList = new ArrayList<>();
        Date date = new Date();
        long id = date.getTime();

        for (PenguinMatrixDTO penguinData : penguinMatrixDTOList) {
            //样本量小进行下次循环
            if (penguinData.getTimes() < sampleSize) continue;
            //掉落为零进行下次循环
            if (penguinData.getQuantity() == 0) continue;
            //材料不在材料表继续下次循环
            if (itemMap.get(penguinData.getItemId()) == null) continue;
            //关卡不在关卡表继续下次循环
            if (stageMap.get(penguinData.getStageId()) == null) continue;
            if (penguinData.getItemId().startsWith("ap_supply")) continue;
            if (penguinData.getItemId().startsWith("randomMaterial")) continue;

            //关卡信息
            Stage stage = stageMap.get(penguinData.getStageId());
            //材料信息
            Item item = itemMap.get(penguinData.getItemId());

            //材料掉率
            double knockRating = ((double) penguinData.getQuantity() / (double) penguinData.getTimes());
            //关卡置信度
            double sampleConfidence = sampleConfidence(penguinData.getTimes(), stage.getApCost(),
                    item.getItemValueAp(), knockRating, quantileTables);
            //该条掉落记录的结果
            double result = item.getItemValueAp() * knockRating;
            //期望理智
            double apExpect = stage.getApCost() / knockRating;

            StageResultV2 stageResult = new StageResultV2();
            stageResult.setId(id++);
            stageResult.setStageId(stage.getStageId());
            stageResult.setItemId(item.getItemId());
            stageResult.setItemName(item.getItemName());
            stageResult.setKnockRating(knockRating);
            stageResult.setResult(result);
            stageResult.setApExpect(apExpect);
            stageResult.setSampleSize(penguinData.getTimes());
            stageResult.setSampleConfidence(sampleConfidence);

            if (stage.getStageType() == 3) {
                StageResultV2 stageResultExtra = new StageResultV2();
                stageResultExtra.setId(id++);
                stageResultExtra.setStageId(stage.getStageId());
                stageResultExtra.setItemId(item.getItemId());
                stageResultExtra.setItemName(item.getItemName());
                stageResultExtra.setKnockRating(knockRating);
                stageResultExtra.setResult(result);
                stageResultExtra.setApExpect(apExpect);
                stageResultExtra.setSampleSize(penguinData.getTimes());
                stageResultExtra.setSampleConfidence(sampleConfidence);
                stageResultExtra.setVersion(version);
                stageResultList.add(stageResultExtra);

            }
            stageResultList.add(stageResult);
        }

        //将上面的计算结果根据关卡id分组
        Map<String, List<StageResultV2>> resultCollectByStageId = stageResultList.stream()
                .collect(Collectors.groupingBy(StageResultV2::getStageId));
        //根据材料类型分组
        Map<String, List<StageResultSample>> resultCollectByItemType = new HashMap<>();

        List<StageResultSample> stageResultSampleList = new ArrayList<>();

        for (String stageId : resultCollectByStageId.keySet()) {
            List<StageResultV2> stageResultListByStageId = resultCollectByStageId.get(stageId);

            Stage stage = stageMap.get(stageId.replace("_LMD", ""));
            //关卡消耗体力
            double apCost = stage.getApCost();
            //关卡掉落龙门币的价值
            double lmd = 0.0036 * 10 * 1.2 * apCost;
            //关卡掉落的材料等效价值总和
            double apValueSum = stageResultListByStageId.stream().mapToDouble(StageResultV2::getResult).sum() + lmd;
//            LogUtil.info(stageResultNews.get(0).getStageCode()+"消耗："+apCost+"，理智，产出："+apValueSum+"理智。");
            //关卡理智效率
            double stageEfficiency = apValueSum / apCost;
            //如果是计算了无限龙门币的SS关卡额外加0.072效率
            if (stageId.endsWith("_LMD")) stageEfficiency += 0.072;

            //将关卡根据掉落材料等效价值进行倒序排序
            stageResultListByStageId.sort(Comparator.comparing(StageResultV2::getResult).reversed());
            //排在第一的即为关卡主要产物
            String main = stageResultListByStageId.get(0).getItemName();
            String mainItemId = stageResultListByStageId.get(0).getItemId();
            //材料系列
            String itemType = "empty";
            String itemTypeId = "empty";

            //如果在18种蓝色精英材料里面，这个关卡要设置材料系列
            if (itemSeriesTable.get(mainItemId) != null) {
                itemType = itemSeriesTable.get(mainItemId).get("series").asText();
                itemTypeId = itemSeriesTable.get(mainItemId).get("id").asText();
            }

            if("empty".equals(itemType)) continue;

            //副产物
            String secondary = "empty";
            String secondaryItemId = "empty";
            if (stageResultListByStageId.size() > 1) {
                double ratio = stageResultListByStageId.get(1).getResult() / apCost;
                if (ratio > 0.1) {
                    secondary = stageResultListByStageId.get(1).getItemName();
                    secondaryItemId = stageResultListByStageId.get(1).getItemName();
                }
            }

            for (int i = 0; i < stageResultListByStageId.size(); i++) {
                StageResultV2 stageResult = stageResultListByStageId.get(i);
                double ratio = stageResult.getResult() / apValueSum;
                stageResult.setRatio(ratio);
                stageResult.setRatioRank(i);
            }

            StageResultSample stageResultSample = new StageResultSample();
            stageResultSample.setId(id++);
            stageResultSample.setStageId(stageId);
            stageResultSample.setStageCode(stage.getStageCode());
            stageResultSample.setStageType(stage.getStageType());
            stageResultSample.setMainItemId(main);
            stageResultSample.setSecondaryItemId(secondary);
            stageResultSample.setEndTime(stage.getEndTime());
            stageResultSample.setVersion(version);
            stageResultSample.setItemType(itemType);
            stageResultSample.setItemTypeId(itemTypeId);
            stageResultSample.setStageEfficiency(stageEfficiency);
            stageResultSampleList.add(stageResultSample);

            calMainItemRatio(stageResultSample,stageResultListByStageId);

            if (stage.getStageType() < 3 ) {

                List<StageResultSample> list = new ArrayList<>();
                if (resultCollectByItemType.get(itemType) == null) {
                    list.add(stageResultSample);
                } else {
                    list = resultCollectByItemType.get(itemType);
                    list.add(stageResultSample);
                }

                resultCollectByItemType.put(itemType, list);
            }


        }


        List<ItemIterationValue> iterationValueList = new ArrayList<>();
        Map<String, Double> itemTypeAndMaxPermStageEfficiency = new HashMap<>();
        for (String itemType : resultCollectByItemType.keySet()) {
            List<StageResultSample> list = resultCollectByItemType.get(itemType);
            list.sort(Comparator.comparing(StageResultSample::getStageEfficiency).reversed());
            itemTypeAndMaxPermStageEfficiency.put(itemType, list.get(0).getStageEfficiency());
            ItemIterationValue itemIterationValue = new ItemIterationValue();
            itemIterationValue.setItemName(itemType);
            itemIterationValue.setIterationValue(list.get(0).getStageEfficiency());
            itemIterationValue.setVersion(version);
            iterationValueList.add(itemIterationValue);
            Log.info(itemType + "的最优本是" + list.get(0).getStageCode() + "，材料的迭代系数是" + 1 / list.get(0).getStageEfficiency());
        }


        stageResultV2Mapper.delete(new QueryWrapper<StageResultV2>().eq("version", version));
        stageResultSampleMapper.delete(new QueryWrapper<StageResultSample>().eq("version", version));
        itemService.deleteItemIterationValue(version);
        itemService.saveItemIterationValue(iterationValueList);


        List<StageResultV2> insertList = new ArrayList<>();



        for (StageResultV2 stageResultV2 :stageResultList){
            insertList.add(stageResultV2);
            if(insertList.size()==2000){
                Log.info("本次批量插入关卡掉落价值数据条数："+insertList.size());
                stageResultV2Mapper.insertBatch(insertList);
                insertList.clear();
            }
        }

        if(insertList.size()>0) {
            Log.info("本次批量插入关卡掉落价值数据条数："+insertList.size());
            stageResultV2Mapper.insertBatch(insertList);
        }


        stageResultSampleMapper.insertBatch(stageResultSampleList);
        Log.info("本次批量插入关卡效率数据条数："+stageResultSampleList.size());


        return itemTypeAndMaxPermStageEfficiency;
    }


    /**
     * 企鹅物流数据中的磨难与标准关卡合并掉落次数和样本量
     *
     * @param penguinDataList 企鹅物流数据
     * @return 合并完的企鹅数据
     */
    public List<PenguinMatrixDTO> mergePenguinData(List<PenguinMatrixDTO> penguinDataList) {
        Map<String, PenguinMatrixDTO> collect = penguinDataList.stream().collect(Collectors.toMap(entity -> entity.getStageId() + entity.getItemId(), Function.identity()));
        penguinDataList.stream()
                .filter(penguinData -> penguinData.getStageId().startsWith("main_10") || penguinData.getStageId().startsWith("main_11") || penguinData.getStageId().startsWith("main_12"))
                .forEach(entity -> {
                    if (collect.get(entity.getStageId().replace("main", "tough") + entity.getItemId()) != null) {
                        PenguinMatrixDTO toughData = collect.get(entity.getStageId().replace("main", "tough") + entity.getItemId());
                        entity.setTimes(entity.getTimes() + toughData.getTimes());
                        entity.setQuantity(entity.getQuantity() + toughData.getQuantity());
                    }
                });

        return penguinDataList;
    }

    /**
     * @param stageResultSample 关卡结果展示对象
     * @param stageResults  关卡每种掉落物品的详细数据
     */
    private void calMainItemRatio(StageResultSample stageResultSample, List<StageResultV2> stageResults) {

        String itemType = stageResultSample.getItemType();  //关卡掉落材料所属类型   比如1-7，4-6这种同属”固源岩组“类
        if("empty".equals(itemType)){
            stageResultSample.setLeT5Efficiency(0.0);  //等级1-4的材料占比
            stageResultSample.setLeT4Efficiency(0.0); //等级1-3的材料占比
            stageResultSample.setLeT3Efficiency(0.0); //等级1-2的材料占比
            return;
        }
        //获取材料的上下位材料
        JsonNode itemTypeTable = JsonMapper.parseJSONObject(FileUtil.read(ApplicationConfig.Item + "item_type_table.json"));
        JsonNode itemTypeNode = itemTypeTable.get(itemType);

        //该系材料的每种等级材料在关卡中的占比
        double rarity1Ratio = 0.0;
        double rarity2Ratio = 0.0;
        double rarity3Ratio = 0.0;
        double rarity4Ratio = 0.0;


        //计算该系材料的每种等级材料在关卡中的占比
        for (StageResultV2 stageResultV2 : stageResults) {
            if (itemTypeNode.get(itemType) != null) {
                JsonNode item = itemTypeNode.get(itemType);
                int rarity = item.get("rarity").asInt();
                if (rarity == 1) rarity1Ratio = stageResultV2.getRatio();
                if (rarity == 2) rarity2Ratio = stageResultV2.getRatio();
                if (rarity == 3) rarity3Ratio = stageResultV2.getRatio();
                if (rarity == 4) rarity4Ratio = stageResultV2.getRatio();
            }
        }

        stageResultSample.setLeT5Efficiency(rarity4Ratio + rarity3Ratio + rarity2Ratio + rarity1Ratio);  //等级1-4的材料占比
        stageResultSample.setLeT4Efficiency(rarity3Ratio + rarity2Ratio + rarity1Ratio); //等级1-3的材料占比
        stageResultSample.setLeT3Efficiency(rarity2Ratio + rarity1Ratio); //等级1-2的材料占比
    }

    /**
     * 置信度计算
     *
     * @param penguinDataTimes 样本量
     * @param apCost           关卡消耗理智
     * @param itemValue        材料价值
     * @param probability      掉率
     * @param quantileTables   置信度表
     * @return 置信度
     */
    public Double sampleConfidence(Integer penguinDataTimes, double apCost, double itemValue, double probability, List<QuantileTable> quantileTables) {
        double quantileValue = 0.03 * apCost / itemValue / Math.sqrt(1 * probability * (1 - probability) / (penguinDataTimes - 1));
        if (quantileValue >= 3.09023 || Double.isNaN(quantileValue)) return 99.9;
        List<QuantileTable> collect = quantileTables.stream()
                .filter(quantileTable -> quantileTable.getValue() <= quantileValue).collect(Collectors.toList());
        return (collect.get(collect.size() - 1).getSection() * 2 - 1) * 100;
    }
}
