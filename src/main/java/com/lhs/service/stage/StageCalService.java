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
import com.lhs.common.entity.ResultCode;
import com.lhs.entity.po.stage.*;

import com.lhs.mapper.QuantileMapper;
import com.lhs.mapper.StageResultMapper;
import com.lhs.entity.dto.stage.PenguinMatrixDTO;
import com.lhs.entity.dto.stage.StageParamDTO;
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

    private final RedisTemplate<String,Object> redisTemplate;

    public StageCalService(StageService stageService, StageResultMapper stageResultMapper, QuantileMapper quantileMapper, ItemService itemService, RedisTemplate<String, Object> redisTemplate) {
        this.stageService = stageService;
        this.stageResultMapper = stageResultMapper;
        this.quantileMapper = quantileMapper;
        this.itemService = itemService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 关卡效率计算
     *
     * @param items          材料表
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
                new TypeReference<List<PenguinMatrixDTO>>(){} );

        //合并企鹅物流的标准和磨难关卡的样本
        penguinMatrixDTOList = mergePenguinData(penguinMatrixDTOList);

        //将item表的各项信息转为Map  <itemId,Item>
        Map<String, Item> itemMap = items.stream().collect(Collectors.toMap(Item::getItemId, Function.identity()));

        //将stage的各项信息转为Map <stageId,stage>
        QueryWrapper<Stage> stageQueryWrapper = new QueryWrapper<>();
        stageQueryWrapper.notLike("stage_id", "tough");
        Map<String, Stage> stageMap = stageService.getStageList(stageQueryWrapper).stream()
                .collect(Collectors.toMap(Stage::getStageId, Function.identity()));

        //置信度表
        List<QuantileTable> quantileTables = quantileMapper.selectList(null);
        double gachaBoxExpectValue = 22.40;

        List<StageResult> stageResultList = new ArrayList<>();    //关卡效率的计算结果的集合
        Date date = new Date();


        Long id  = System.currentTimeMillis();


        for (PenguinMatrixDTO penguinData : penguinMatrixDTOList) {
            //样本量小进行下次循环
            if (penguinData.getTimes() < sampleSize) continue;
            //掉落为零进行下次循环
            if (penguinData.getQuantity() == 0) continue;
            //材料不在材料表继续下次循环
            if (itemMap.get(penguinData.getItemId()) == null) continue;
            //关卡不在关卡表继续下次循环
            if (stageMap.get(penguinData.getStageId()) == null) continue;

            if(penguinData.getItemId().startsWith("ap_supply")){
                continue;
            }

            if(penguinData.getItemId().startsWith("randomMaterial")){
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

            if (stage.getStageType() ==3) {
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

        Map<String, List<StageResult>> resultGroupByStageId = stageResultList.stream()
                .collect(Collectors.groupingBy(StageResult::getStageId));

        Map<String, List<StageResult>> resultGroupByItemType = new HashMap<>();

        for (String stageId : resultGroupByStageId.keySet()) {
            List<StageResult> stageResults = resultGroupByStageId.get(stageId);
            double apCost = stageResults.get(0).getApCost();
            double lmd = 0.0036 * 10 * 1.2 * apCost;
            double apValueSum = stageResults.stream().mapToDouble(StageResult::getResult).sum() +lmd;
//            LogUtil.info(stageResultNews.get(0).getStageCode()+"消耗："+apCost+"，理智，产出："+apValueSum+"理智。");
            double stageEfficiency = apValueSum / apCost;
            if (stageId.endsWith("_LMD")) stageEfficiency += 0.072;
            stageResults.sort(Comparator.comparing(StageResult::getResult).reversed());

            String main = stageResults.get(0).getItemName();
            String itemType = "empty";
            if(itemTypeTable.get(main)!=null){
                itemType = itemTypeTable.get(main).asText();
            }

            String secondary = "empty";
            String secondaryId = "empty";
            if(stageResults.size()>1){
                double ratio = stageResults.get(1).getResult()/apCost;
                if(ratio>0.1){
                    secondary = stageResults.get(1).getItemName();
                    secondaryId = stageResults.get(1).getItemId();
                }
            }


            for (int i = 0; i < stageResults.size(); i++) {
                StageResult stageResult = stageResults.get(i);
                double ratio = stageResult.getResult()/apValueSum;
                stageResult.setStageEfficiency(stageEfficiency);
                stageResult.setRatio(ratio);
                stageResult.setRatioRank(i);
                stageResult.setItemType(itemType);
                if(i==0){
                    stageResult.setMain(main);
                }

                stageResult.setSecondary(secondary);
                stageResult.setSecondaryId(secondaryId);

//                System.out.println(stageResult);

                if ("empty".equals(stageResult.getItemType()) ) {
                    continue;
                }

                if ( stageResult.getStageType() > 2) {
                    continue;
                }


                List<StageResult> list = new ArrayList<>();
                if (resultGroupByItemType.get(stageResult.getItemType()) == null) {
                    list.add(stageResult);
                } else {
                    list = resultGroupByItemType.get(stageResult.getItemType());
                    list.add(stageResult);
                }
                resultGroupByItemType.put(stageResult.getItemType(), list);
            }

            resultGroupByStageId.put(stageId, stageResults);
        }

        List<ItemIterationValue> iterationValueList = new ArrayList<>();
        Map<String, Double> itemTypeAndMaxPermStageEfficiency = new HashMap<>();
        for (String itemType : resultGroupByItemType.keySet()) {
            List<StageResult> list = resultGroupByItemType.get(itemType);
            list.sort(Comparator.comparing(StageResult::getStageEfficiency).reversed());
            itemTypeAndMaxPermStageEfficiency.put(itemType,list.get(0).getStageEfficiency());
            ItemIterationValue itemIterationValue = new ItemIterationValue();
            itemIterationValue.setItemName(itemType);
            itemIterationValue.setIterationValue(list.get(0).getStageEfficiency());
            itemIterationValue.setVersion(version);
            iterationValueList.add(itemIterationValue);
            Log.info(itemType + "的最优本是" + list.get(0).getStageCode()+"，材料的迭代系数是" + 1 / list.get(0).getStageEfficiency());

        }


        QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("version",version);
        int delete = stageResultMapper.delete(queryWrapper);

        stageResultList.forEach(e->e.setStageEfficiency(e.getStageEfficiency()*100));
        if(delete>-1){
//            stageResultMapper.insertBatch(stageResultList);
            saveBatch(stageResultList);
        }

         itemService.deleteItemIterationValue(version);


        itemService.saveItemIterationValue(iterationValueList);


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
