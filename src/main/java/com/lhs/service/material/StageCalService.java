package com.lhs.service.material;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.material.PenguinMatrixDTO;
import com.lhs.entity.dto.material.StageParamDTO;
import com.lhs.entity.po.material.*;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.material.MaterialValueConfigMapper;
import com.lhs.mapper.material.QuantileMapper;
import com.lhs.mapper.material.StageResultMapper;
import com.lhs.mapper.material.StageResultDetailMapper;
import com.lhs.service.user.UserService;
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

    private final MaterialValueConfigMapper materialValueConfigMapper;

    public StageCalService(StageService stageService, QuantileMapper quantileMapper, ItemService itemService, StageResultMapper stageResultMapper, RedisTemplate<String, Object> redisTemplate, StageResultDetailMapper stageResultDetailMapper, UserService userService, MaterialValueConfigMapper materialValueConfigMapper) {
        this.stageService = stageService;
        this.quantileMapper = quantileMapper;
        this.itemService = itemService;
        this.stageResultMapper = stageResultMapper;
        this.redisTemplate = redisTemplate;
        this.stageResultDetailMapper = stageResultDetailMapper;
        this.userService = userService;
        this.materialValueConfigMapper = materialValueConfigMapper;
        idGenerator = new IdGenerator(1L);
    }

    public void saveMaterialValueConfig(Map<String,Object> requestParams){
        Object oToken = requestParams.get("token");
        if(oToken==null){
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        UserInfoVO userInfoByToken = userService.getUserInfoVOByToken(String.valueOf(oToken));
        Long uid = userInfoByToken.getUid();

        MaterialValueConfig materialValueConfig = new MaterialValueConfig();

    }

    public void stageResultCal(List<Item> items, StageParamDTO stageParamDTO) {

        //物品信息Map  <itemId,Item>
        Map<String, Item> itemMap = items.stream().collect(Collectors.toMap(Item::getItemId, Function.identity()));
        //关卡信息Map <stageId,stage>
        Map<String, Stage> stageMap = stageService.getStageList(new QueryWrapper<Stage>()
                        .notLike("stage_id", "tough"))
                .stream()
                .collect(Collectors.toMap(Stage::getStageId, Function.identity()));

        JsonNode itemSeriesTable = JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.Item + "item_series_table.json"));

        Integer sampleSize = stageParamDTO.getSampleSize();
        String version = stageParamDTO.getVersion();

        List<PenguinMatrixDTO> penguinMatrix = getPenguinMatrix(itemMap, stageMap, sampleSize);

        Map<String, List<PenguinMatrixDTO>> matrixByStageId = penguinMatrix.stream()
                .collect(Collectors.groupingBy(PenguinMatrixDTO::getStageId));

        List<QuantileTable> quantileTables = quantileMapper.selectList(null);

        List<StageResultDetail> detailInsertList = new ArrayList<>();
        List<StageResult> commonInsertList = new ArrayList<>();

        HashMap<String, Double> itemIterationValueMap = new HashMap<>();


        Map<String,String> stageBlacklist = stageParamDTO.getStageBlacklist();

        for (String stageId : matrixByStageId.keySet()) {

            if(stageBlacklist.get(stageId)!=null){
                Logger.info("黑名单关卡"+stageId);
                continue;
            }

            Stage stage = stageMap.get(stageId);

            List<PenguinMatrixDTO> stageDropList = matrixByStageId.get(stageId);
            //关卡消耗体力
            double apCost = stage.getApCost();
            String stageType = stage.getStageType();
            //关卡效率
            double stageEfficiency;
            //关卡掉落物品价值总和
            double dropApValueSum = 0.0;


            PenguinMatrixDTO stageDropLMD = new PenguinMatrixDTO();
            stageDropLMD.setStageId(stageId);
            stageDropLMD.setItemId("4001");
            stageDropLMD.setQuantity((int) (12 * apCost));
            stageDropLMD.setTimes(1);
            stageDropList.add(stageDropLMD);



            if (StageType.ACT_REP.equals(stageType) || StageType.ACT.equals(stageType)) {
                PenguinMatrixDTO stageDropStore = new PenguinMatrixDTO();
                stageDropStore.setStageId(stageId);
                stageDropStore.setItemId("4001");
                stageDropStore.setQuantity((int) (20 * apCost));
                stageDropStore.setTimes(1);
                stageDropList.add(stageDropStore);
            }


            List<StageResultDetail> temporaryList = new ArrayList<>();

            for (PenguinMatrixDTO stageDrop : stageDropList) {
                Item item = itemMap.get(stageDrop.getItemId());

                //关卡掉落详情
                StageResultDetail detail = new StageResultDetail();
                //材料掉率
                double knockRating = ((double) stageDrop.getQuantity() / (double) stageDrop.getTimes());

                //关卡置信度
                double sampleConfidence = sampleConfidence(stageDrop.getTimes(), apCost,
                        item.getItemValueAp(), knockRating, quantileTables);
                //该条掉落记录的结果
                double result = item.getItemValueAp() * knockRating;
                //期望理智
                double apExpect = apCost / knockRating;
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
                temporaryList.add(detail);
                dropApValueSum += result;

            }

            temporaryList.sort(Comparator.comparing(StageResultDetail::getResult).reversed());
            //材料系列
            String itemSeries = "empty";
            String itemSeriesId = "empty";
            String secondaryItemName = "empty";
            String secondaryItemId = "empty";
            String mainItemName = "empty";
            Integer itemRarity = null;
            for (int i = 0; i < temporaryList.size(); i++) {
                StageResultDetail detail = temporaryList.get(i);
                detail.setRatio(detail.getResult() / dropApValueSum);
                detail.setRatioRank(i);
                String itemId = detail.getItemId();
                String itemName = detail.getItemName();
                if (i == 0) {
                    //找到这个材料所属的材料系列
                    if (itemSeriesTable.get(itemId) != null) {
                        itemSeries = itemSeriesTable.get(itemId).get("series").asText();
                        itemSeriesId = itemSeriesTable.get(itemId).get("seriesId").asText();
                        itemRarity = itemMap.get(itemId).getRarity();
                        mainItemName = itemName;
                    }
                }
                if (i == 1) {
                    if (detail.getResult() / dropApValueSum > 0.1) {
                        secondaryItemName = itemName;
                        secondaryItemId = itemId;
                    }
                }


                detailInsertList.add(detail);
//                System.out.println(detail);
            }

            stageEfficiency = dropApValueSum / apCost;

            if (StageType.MAIN.equals(stageType) || StageType.ACT_PERM.equals(stageType)) {
                if (!"empty".equals(itemSeriesId)) {
                    Double maxStageEfficiency = itemIterationValueMap.get(itemSeriesId);
                    if (maxStageEfficiency == null) {
                        itemIterationValueMap.put(itemSeriesId, stageEfficiency);
                    } else {
                        if (maxStageEfficiency < stageEfficiency) {
                            itemIterationValueMap.put(itemSeriesId, stageEfficiency);
                        }
                    }
                }
            }

            //计算战备资源活动的效率
//            if (stageId.startsWith("main")) {
//                dropApValueSum += apCost / (1000.0 / 6) * 11.28;
//                apCost -= apCost / 200 * 10;
//                stageEfficiency = dropApValueSum / apCost;
//            }

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
            calMainItemRatio(stageResult, apCost, temporaryList);

//            Log.info(stage.getStageCode()+" {} 主产物:"+mainItemName+" {} 副产物："+secondaryItemName+" {} 关卡效率："+stageEfficiency);
            commonInsertList.add(stageResult);
        }


        List<ItemIterationValue> iterationValueList = new ArrayList<>();
        itemIterationValueMap.forEach((itemTypeId, iterationValue) -> {
            ItemIterationValue itemIterationValue = new ItemIterationValue();
            String itemName = itemMap.get(itemTypeId).getItemName();
            itemIterationValue.setItemName(itemName);
            itemIterationValue.setIterationValue(iterationValue);
            itemIterationValue.setVersion(version);
            itemIterationValue.setItemId(itemTypeId);
            iterationValueList.add(itemIterationValue);
            Logger.info(String.format("%-8s", itemName) + "的迭代系数是：" + 1 / iterationValue);
        });

        stageResultDetailMapper.delete(new QueryWrapper<StageResultDetail>().eq("version", version));
        stageResultMapper.delete(new QueryWrapper<StageResult>().eq("version", version));
        itemService.deleteItemIterationValue(version);
        itemService.saveItemIterationValue(iterationValueList);

        List<StageResultDetail> insertList = new ArrayList<>();
        for (StageResultDetail stageResultDetail : detailInsertList) {
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

        Logger.info("本次批量插入关卡掉落详细数据条数：" + detailInsertList.size());
        stageResultMapper.insertBatch(commonInsertList);
        Logger.info("本次批量插入关卡通用掉落数据条数：" + commonInsertList.size());

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
        JsonNode itemTypeTable = JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.Item + "item_type_table.json"));
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
                if (rarity == 1) rarity1Ratio = stageResultDetail.getResult() / apCost;
                if (rarity == 2) rarity2Ratio = stageResultDetail.getResult() / apCost;
                if (rarity == 3) rarity3Ratio = stageResultDetail.getResult() / apCost;
                if (rarity == 4) rarity4Ratio = stageResultDetail.getResult() / apCost;
            }
        }

        common.setLeT4Efficiency(rarity4Ratio + rarity3Ratio + rarity2Ratio + rarity1Ratio);  //等级1-4的材料占比
        common.setLeT3Efficiency(rarity3Ratio + rarity2Ratio + rarity1Ratio); //等级1-3的材料占比
        common.setLeT2Efficiency(rarity2Ratio + rarity1Ratio); //等级1-2的材料占比
    }

    /**
     * 企鹅物流关卡矩阵中的磨难与标准关卡合并掉落次数和样本量
     *
     * @return 合并完的企鹅数据
     */
    private List<PenguinMatrixDTO> getPenguinMatrix(Map<String, Item> itemMap, Map<String, Stage> stageMap, Integer sampleSize) {
        //获取企鹅物流关卡矩阵
        String penguinStageDataText = FileUtil.read(ConfigUtil.Penguin + "matrix auto.json");
        if (penguinStageDataText == null) throw new ServiceException(ResultCode.DATA_NONE);
        String matrixText = JsonMapper.parseJSONObject(penguinStageDataText).get("matrix").toPrettyString();
        List<PenguinMatrixDTO> penguinMatrixDTOList = JsonMapper.parseJSONArray(matrixText, new TypeReference<List<PenguinMatrixDTO>>() {
        });

        //将磨难关卡筛选出来
        Map<String, PenguinMatrixDTO> toughStageMap = penguinMatrixDTOList.stream()
                .filter(e -> e.getStageId().contains("tough"))
                .collect(Collectors.toMap(e -> e.getStageId()
                        .replace("tough", "main") + "." + e.getItemId(), Function.identity()));

        //将磨难关卡和标准关卡合并
        List<PenguinMatrixDTO> mergeList = new ArrayList<>();
        for (PenguinMatrixDTO element : penguinMatrixDTOList) {
            String stageId = element.getStageId();
            if (stageId.contains("tough")) {
                continue;
            }
            if (toughStageMap.get(stageId + "." + element.getItemId()) != null) {
                PenguinMatrixDTO toughItem = toughStageMap.get(stageId + "." + element.getItemId());
                element.setQuantity(element.getQuantity() + toughItem.getQuantity());
                element.setTimes(element.getTimes() + toughItem.getTimes());
            }

            if(stageId.startsWith("main_14")){
                if(element.getEnd()!=null){
                    continue;
                }
            }

            if (element.getTimes() < sampleSize) {
                continue;
            }
            //掉落为零进行下次循环
            if (element.getQuantity() == 0) continue;
            //材料不在材料表继续下次循环
            if (itemMap.get(element.getItemId()) == null) {
                continue;
            }
            //关卡不在关卡表继续下次循环
            if (stageMap.get(element.getStageId()) == null) {
                continue;
            }
            if (element.getItemId().startsWith("ap_supply")) {
                continue;
            }
            if (element.getItemId().startsWith("randomMaterial")) {
                continue;
            }

            mergeList.add(element);

        }
        return mergeList;
    }

    private Double sampleConfidence(Integer penguinDataTimes, double apCost, double itemValue, double probability, List<QuantileTable> quantileTables) {
        double quantileValue = 0.03 * apCost / itemValue / Math.sqrt(1 * probability * (1 - probability) / (penguinDataTimes - 1));
        if (quantileValue >= 3.09023 || Double.isNaN(quantileValue)) return 99.9;
        List<QuantileTable> collect = quantileTables.stream()
                .filter(quantileTable -> quantileTable.getValue() <= quantileValue).toList();
        return (collect.get(collect.size() - 1).getSection() * 2 - 1) * 100;
    }


}
