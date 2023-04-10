package com.lhs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.SerializationUtils;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lhs.common.util.HttpRequestUtil;
import com.lhs.entity.*;
import com.lhs.mapper.QuantileMapper;
import com.lhs.mapper.StageResultMapper;
import com.lhs.common.util.FileUtil;
import com.lhs.common.config.FileConfig;
import com.lhs.service.resultVo.PenguinDataResponseVo;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
public class StageResultService extends ServiceImpl<StageResultMapper, StageResult> {

    @Autowired
    private StageService stageService;
    @Autowired
    private StageResultMapper stageResultMapper;
    @Autowired
    private QuantileMapper quantileMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 用企鹅物流的数据和第一次迭代值Vn进行第一次关卡效率En计算
     *
     * @param items          材料表
     * @param expCoefficient 经验书系数，一般为0.625（还有1.0和0.0）
     * @param sampleSize     最低样本大小
     * @return map<蓝材料名称 ， 蓝材料对应的常驻最高关卡效率En>
     */
    @Transactional
    public HashMap<String, Double> stageResultCal(List<Item> items, Double expCoefficient, Integer sampleSize) {

        //将企鹅物流的数据转成集合

        String response = FileUtil.read(FileConfig.Penguin + "matrix global.json");  //读取企鹅物流数据文件
        List<PenguinDataResponseVo> penguinDataResponseVos = JSONArray.parseArray(JSONObject.parseObject(response).getString("matrix"), PenguinDataResponseVo.class);//将企鹅物流文件的内容转为集合
        penguinDataResponseVos = mergePenguinData(penguinDataResponseVos);  //合并企鹅物流的标准和磨难关卡的样本

        Map<String, Item> itemValueMap = items.stream().collect(Collectors.toMap(Item::getItemId, Function.identity())); //将item表的各项信息转为Map  <itemId,Item类 >
        Map<String, Stage> stageInfoMap = stageService.findAll(new QueryWrapper<Stage>().notLike("stage_id", "tough")).stream().collect(Collectors.toMap(Stage::getStageId, Function.identity()));  //将stage的各项信息转为Map <stageId,stage类 >
        List<QuantileTable> quantileTables = quantileMapper.selectList(null);
//        Double gachaBoxExpectValue = gachaBoxExpectValue(penguinDataResponseVos, itemValueMap);
//        penguinDataResponseVos.forEach(e-> System.out.println(e));
        penguinDataResponseVos = penguinDataResponseVos.stream()
                .filter(penguinData -> penguinData.getTimes() > sampleSize && itemValueMap.get(penguinData.getItemId()) != null  //过滤掉（该条记录的样本低于300 & 该条记录的掉落材料不存在于材料表中 & 该条记录的关卡ID不存在于关卡表中）的数据
                        && stageInfoMap.get(penguinData.getStageId()) != null)
                .collect(Collectors.toList());

        HashMap<String, Double> ApSupplyAndRandomMaterialAndApCostMap = new HashMap<>();



//      保存企鹅物流每一条记录的计算结果
        List<StageResult> stageResultList = new ArrayList<>();    //关卡效率的计算结果的集合
        long id = new Date().getTime() * 100000;   //id为时间戳后加00001至99999

        log.info("开始计算，企鹅数据" + penguinDataResponseVos.size() + "条，关卡数据有" + stageInfoMap.keySet().size() + "条，材料数据有" + itemValueMap.keySet().size() + "条");


        Date createTime = new Date();
        for (PenguinDataResponseVo penguinData : penguinDataResponseVos) {
            StageResult.StageResultBuilder stageResultBuilder = StageResult.builder();


            Stage stage = stageInfoMap.get(penguinData.getStageId());
            Item item = itemValueMap.get(penguinData.getItemId());
//            System.out.println(stage.getStageCode());
            double knockRating = ((double) penguinData.getQuantity() / (double) penguinData.getTimes());  //材料掉率
            if (knockRating == 0) continue; //÷0就跳过
            double sampleConfidence = sampleConfidence(penguinData.getTimes(), stage, item.getItemValueAp(), knockRating, quantileTables); //置信度
            StageResult stageResult = stageResultBuilder.sampleSize(penguinData.getTimes()).sampleConfidence(sampleConfidence).itemRarity(item.getRarity()).knockRating(knockRating).stageColor(2)
                    .apExpect(stage.getApCost() / knockRating).result(item.getItemValueAp() * knockRating).spm(stage.getSpm()).updateTime(createTime)
                    .openTime(stage.getOpenTime()).build();   //写入关卡表的各种信息

            if("randomMaterial_8".equals(item.getItemId())) {
                ApSupplyAndRandomMaterialAndApCostMap.put("result_"+stage.getStageId(),item.getItemValueAp() * knockRating);
//                log.info("物资箱期望价值："+(item.getItemValueAp() * knockRating));
                stageResult.setResult(0.0);
            }
            if("ap_supply_lt_010".equals(item.getItemId())) {
                ApSupplyAndRandomMaterialAndApCostMap.put("apCost_"+stage.getStageId(),stage.getApCost()-10*knockRating);
//                log.info(stage.getStageCode()+"的体力消耗扣除后是："+(stage.getApCost()-10*knockRating));
                stageResult.setResult(0.0);
            }



            BeanUtils.copyProperties(stage, stageResult);  //复制关卡表的信息
            BeanUtils.copyProperties(item, stageResult);   //复制材料表的信息
            if (!item.getItemName().equals(stage.getMain()))
                stageResult.setItemType(null); // 只有该关卡的主产物的计算结果才保存材料类型，否则是null
            stageResult.setId(id++);
            stageResultList.add(stageResult);
//            if("落叶逐火".equals(stageResult.getZoneName())) System.out.println(stageResult);
            //判断是否为活动本（展示状态为1，但不参与定价），复制一条该关卡的结果，但加上商店无限龙门币的价值
            if (stage.getIsShow() == 1 && stage.getIsValue() == 0 && !(stage.getStageId().startsWith("act24side"))) {
                StageResult efficiencyResultCopy = SerializationUtils.clone(stageResult);
                efficiencyResultCopy.setId(id + 100000);
                efficiencyResultCopy.setStageId(stage.getStageId() + "_LMD");
                efficiencyResultCopy.setResult(efficiencyResultCopy.getResult() + stage.getApCost() * 0.72);
                efficiencyResultCopy.setStageColor(-1);
                stageResultList.add(efficiencyResultCopy);
            }

        }

        stageResultList.stream()
                .collect(Collectors.groupingBy(StageResult::getStageId))   //把计算结果根据stageId分组
                .forEach((stageId, list) -> {      //list是相同关卡的所有材料的单条计算结果
//                    System.out.println(stageId);
                    double sum = list.stream().mapToDouble(StageResult::getResult).sum();   //计算关卡的材料产出价值之和V
                    double result_randomMaterial = ApSupplyAndRandomMaterialAndApCostMap.get("result_"+stageId)==null?0.0:ApSupplyAndRandomMaterialAndApCostMap.get("result_"+stageId);
                    double apCostDeductedApSupply = ApSupplyAndRandomMaterialAndApCostMap.get("apCost_"+stageId)==null?0.0:ApSupplyAndRandomMaterialAndApCostMap.get("apCost_"+stageId);

                    double sampleConfidence = list.stream().mapToDouble(StageResult::getSampleConfidence).min().orElse(0.0);
                    Double apCost = list.get(0).getApCost(); //拿到关卡的消耗
                    list.forEach(result -> {
                        double efficiency = sum / apCost + 0.0432;  //计算效率之后保存到该关卡的每一条结果，效率公式为   sum(Vn)/apCost+0.0045*1.2
                        double efficiencyAddRandomMaterial = (sum+result_randomMaterial)/apCostDeductedApSupply + 0.0432;

//                        if (result.getStageId().startsWith("act24side")) {
//                            efficiency = (efficiency - 0.054) / 40 * gachaBoxExpectValue + 0.054;
//                        }
                        result.setEfficiency(efficiency);
                        result.setStageEfficiency(efficiency  * 100);
//                      if((apCostDeductedApSupply>1)) result.setStageEfficiency((efficiencyAddRandomMaterial  * 100)); //效率的百分比  因为材料单位是绿票，最高转化率为1.25理智（1.25理智=1绿票）

                        result.setSampleConfidence(sampleConfidence);
                    });
                });


        HashMap<String, Double> itemNameAndStageEffMap = new HashMap<>();  //  <蓝材料名称 , 蓝材料对应的常驻最高关卡效率En>  value用于下面蓝材料的计算

        stageResultList.stream()   //将关卡效率计算结果根据材料类别分组，存入上面的 itemNameAndStageEffMap
                .filter(stageResult -> stageResult.getItemType() != null && stageResult.getIsValue() == 1)  //过滤掉材料类型为空和不参与定价的关卡，活动本不参与定价，只有关卡中主产物计算结果的材料类型不为空
                .sorted(Comparator.comparing(StageResult::getEfficiency).reversed())  //根据关卡效率降序排列结果
                .collect(Collectors.groupingBy(StageResult::getItemType))  //根据材料类型分类结果
                .forEach((itemName, list) -> {
                    setStageColor(list);   //设置该材料的前8个关卡的颜色级别
                    itemNameAndStageEffMap.put(itemName, list.get(0).getEfficiency());  //拿到该种材料的最优关卡
                    log.info(itemName + "的最优本是" + list.get(0).getStageCode());
                    log.info(itemName + "的回归系数是" + 1 / list.get(0).getEfficiency());
                });


        stageResultMapper.deleteTableTemp();   //清空数据库
        boolean b = saveBatch(stageResultList);//保存结果到数据库
        FileUtil.save(FileConfig.Item,"itemAndBestStageEff.json", JSON.toJSONString(itemNameAndStageEffMap));
        return itemNameAndStageEffMap;

    }

    /**
     * 设置当前相同材料的关卡集合在前端显示的颜色,橙色(双最优):4，紫色(综合效率最优):3，蓝色(普通关卡):2，绿色(主产物期望最优):1，红色(活动):-1
     *
     * @param stageResultList 相同材料的关卡集合
     */
    private static void setStageColor(List<StageResult> stageResultList) {
        String stageId_effMax = stageResultList.get(0).getStageId();   //拿到效率最高的关卡id
        stageResultList.get(0).setStageColor(3);  //效率最高为3

        stageResultList = stageResultList.stream()
                .filter(stageResult -> stageResult.getIsShow() == 1)  //过滤掉已经关闭的关卡
                .limit(8)  //限制个数
                .sorted(Comparator.comparing(StageResult::getApExpect))  //根据期望理智排序
                .collect(Collectors.toList());  //流转为集合
        String stageId_expectMin = stageResultList.get(0).getStageId(); //拿到期望理智最低的关卡id

        if (stageId_effMax.equals(stageId_expectMin)) {  //对比俩个id是否一致
            stageResultList.get(0).setStageColor(4); // 一致为4
        } else {
            stageResultList.get(0).setStageColor(1); // 不一致为1
        }
    }

    /**
     * 炼金池每抽期望价值计算
     * @param penguinDataResponseVoList  企鹅物流源石数据
     * @param itemValueMap  材料价值表的map
     * @return    炼金池每抽期望价值
     */
    private Double gachaBoxExpectValue(List<PenguinDataResponseVo> penguinDataResponseVoList, Map<String, Item> itemValueMap) {
        double gachaBoxItemExpect = 0.0;
        List<PenguinDataResponseVo> collect = penguinDataResponseVoList.stream().filter(penguinData -> "act24side_gacha".equals(penguinData.getStageId())).collect(Collectors.toList());

        for (PenguinDataResponseVo data : collect) {
            switch (data.getItemId()) {
                case "4001_2000":
                    gachaBoxItemExpect += 0.0045 * 2000 * data.getQuantity() / data.getTimes();
//                    log.info("龙门币概率：" + (double) data.getQuantity() / data.getTimes() * 100 + "%，期望价值：" + 0.0045 * 2000 * data.getQuantity() / data.getTimes());
                    break;
                case "31063":
                    gachaBoxItemExpect += itemValueMap.get(data.getItemId()).getItemValueAp() * data.getQuantity() / data.getTimes();
//                    log.info("转质盐组概率：" + (double) data.getQuantity() / data.getTimes() * 100 + "%，期望价值：" + itemValueMap.get(data.getItemId()).getItemValue() * data.getQuantity() / data.getTimes());
                    break;
                case "30063":
                    gachaBoxItemExpect += itemValueMap.get(data.getItemId()).getItemValueAp() * data.getQuantity() / data.getTimes();
//                    log.info("全新装置概率：" + (double) data.getQuantity() / data.getTimes() * 100 + "%，期望价值：" + itemValueMap.get(data.getItemId()).getItemValue() * data.getQuantity() / data.getTimes());
                    break;
                case "30033":
                    gachaBoxItemExpect += itemValueMap.get(data.getItemId()).getItemValueAp() * data.getQuantity() / data.getTimes();
//                    log.info("聚酸酯组概率：" + (double) data.getQuantity() / data.getTimes() * 100 + "%，期望价值：" + itemValueMap.get(data.getItemId()).getItemValue() * data.getQuantity() / data.getTimes());
                    break;
                case "furni":
                    break;
                default:
//                    System.out.println("非法id" + data.getItemId());
            }
        }

        log.info("炼金池期望:" + gachaBoxItemExpect);
        return gachaBoxItemExpect;
    }

    /**
     * 企鹅物流数据中的磨难与标准关卡合并掉落次数和样本量
     * @param penguinDataList 企鹅物流数据
     * @return 合并完的企鹅数据
     */
    public List<PenguinDataResponseVo> mergePenguinData(List<PenguinDataResponseVo> penguinDataList) {
        Map<String, PenguinDataResponseVo> collect = penguinDataList.stream().collect(Collectors.toMap(entity -> entity.getStageId() + entity.getItemId(), Function.identity()));
        penguinDataList.stream()
                .filter(penguinData -> penguinData.getStageId().startsWith("main_10") || penguinData.getStageId().startsWith("main_11")|| penguinData.getStageId().startsWith("main_12"))
                .forEach(entity -> {
//                    System.out.println("合并前："+entity);
                    if(collect.get(entity.getStageId().replace("main","tough") + entity.getItemId())!=null) {
                       PenguinDataResponseVo toughData = collect.get(entity.getStageId().replace("main","tough") + entity.getItemId());
                        entity.setTimes(entity.getTimes() + toughData.getTimes());
                        entity.setQuantity(entity.getQuantity() + toughData.getQuantity());
                    }
//                    System.out.println("合并后："+entity);
                });

        return penguinDataList;
    }

    /**
     * 置信度计算
     * @param penguinDataTimes 样本量
     * @param stage            关卡信息
     * @param itemValue        材料价值
     * @param probability      掉率
     * @param quantileTables   置信度表
     * @return 置信度
     */
    public Double sampleConfidence(Integer penguinDataTimes, Stage stage, double itemValue, double probability, List<QuantileTable> quantileTables) {
        double quantileValue = 0.03 * stage.getApCost()  / itemValue / Math.sqrt(1 * probability * (1 - probability) / (penguinDataTimes - 1));
        if (quantileValue >= 3.09023 || Double.isNaN(quantileValue)) return 99.9;
        List<QuantileTable> collect = quantileTables.stream()
                .filter(quantileTable -> quantileTable.getValue() <= quantileValue).collect(Collectors.toList());
        return (collect.get(collect.size() - 1).getSection() * 2 - 1) * 100;
    }


}
