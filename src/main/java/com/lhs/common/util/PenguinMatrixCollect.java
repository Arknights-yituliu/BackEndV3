package com.lhs.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.enums.StageType;
import com.lhs.common.exception.ServiceException;
import com.lhs.entity.dto.material.PenguinMatrixDTO;
import com.lhs.entity.po.material.Item;
import com.lhs.entity.po.material.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PenguinMatrixCollect {

    /**
     * 企鹅物流关卡矩阵中的磨难与标准关卡合并掉落次数和样本量
     *
     * @return 合并完的企鹅数据
     */
    public static Map<String, List<PenguinMatrixDTO>> filterAndMergePenguinData(Map<String, Item> itemMap, Map<String, Stage> stageMap,Map<String, String> stageBlackMap, Integer sampleSize) {
        //获取企鹅物流关卡矩阵
        String penguinStageDataText = FileUtil.read(ConfigUtil.Penguin + "matrix auto.json");
        if (penguinStageDataText == null) {
            throw new ServiceException(ResultCode.DATA_NONE);
        }
        String matrixText = JsonMapper.parseJSONObject(penguinStageDataText).get("matrix").toPrettyString();
        List<PenguinMatrixDTO> penguinMatrixDTOList = JsonMapper.parseJSONArray(matrixText, new TypeReference<>() {
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

            if(stageBlackMap.get(stageId)!=null){
                continue;
            }

            if (stageId.contains("tough")) {
                continue;
            }

            if (toughStageMap.get(stageId + "." + element.getItemId()) != null) {
                PenguinMatrixDTO toughItem = toughStageMap.get(stageId + "." + element.getItemId());
                element.setQuantity(element.getQuantity() + toughItem.getQuantity());
                element.setTimes(element.getTimes() + toughItem.getTimes());
            }

            if (stageId.startsWith("main_14")) {
                if (element.getEnd() != null) {
                    continue;
                }
            }

            if (element.getTimes() < sampleSize) {
                continue;
            }
            //掉落为零进行下次循环
            if (element.getQuantity() == 0) {
                continue;
            }
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


        Map<String, List<PenguinMatrixDTO>> collect = mergeList.stream()
                .collect(Collectors.groupingBy(PenguinMatrixDTO::getStageId));

        collect.forEach((k,v)->{
            stageDropAddLMD(v, stageMap.get(k));
        });


        return collect;
    }


    /**
     * 由于企鹅对于关卡本身的龙门币不进行统计，手动向企鹅的关卡掉落增加龙门币和商店龙门币
     *
     * @param stageDropList 掉落集合
     * @param stage         关卡信息
     */
    public static void stageDropAddLMD(List<PenguinMatrixDTO> stageDropList, Stage stage) {

        String stageId = stage.getStageId();
        Double apCost = Double.valueOf(stage.getApCost());
        String stageType = stage.getStageType();

        //将关卡固定掉落的龙门币写入到掉落集合中
        PenguinMatrixDTO stageDropLMD = new PenguinMatrixDTO();
        stageDropLMD.setStageId(stageId);
        stageDropLMD.setItemId("4001");
        stageDropLMD.setQuantity((int) (12 * apCost));
        stageDropLMD.setTimes(1);
        stageDropList.add(stageDropLMD);


        //将商店无限兑换区的龙门币视为掉落，写入掉落集合中
        if (StageType.ACT_REP.equals(stageType) || StageType.ACT.equals(stageType)) {
            PenguinMatrixDTO stageDropStore = new PenguinMatrixDTO();
            stageDropStore.setStageId(stageId);
            stageDropStore.setItemId("4001");
            stageDropStore.setQuantity((int) (20 * apCost));
            stageDropStore.setTimes(1);
            stageDropList.add(stageDropStore);
        }
    }
}
