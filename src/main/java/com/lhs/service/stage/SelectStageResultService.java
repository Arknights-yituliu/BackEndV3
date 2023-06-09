package com.lhs.service.stage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.stage.StageResult;

import com.lhs.mapper.StageResultMapper;
import com.lhs.vo.stage.OrundumPerApResultVo;
import com.lhs.vo.stage.StageResultVo;
import com.lhs.vo.stage.StageResultClosedVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SelectStageResultService {

    @Resource
    private StageResultMapper stageResultMapper;

    /**
     * 查询蓝材料的推荐关卡
     *
     * @param expCoefficient 经验书系数，一般为0.625（还有1.0和0.0）
     * @param sampleSize     样本大小
     * @return
     */
    @RedisCacheable(key = "stageT3#expCoefficient")
    public List<List<StageResultVo>> queryStageResultData_t3(Double expCoefficient, Integer sampleSize) {
        List<List<StageResultVo>> stageResultVoList = new ArrayList<>();
        Arrays.asList("全新装置", "异铁组", "轻锰矿", "凝胶", "扭转醇", "酮凝集组", "RMA70-12", "炽合金", "研磨石", "糖组",
                "聚酸酯组", "晶体元件", "固源岩组", "半自然溶剂", "化合切削液", "转质盐组").forEach(type -> {
            //条件：可展示，符合材料类型，符合经验书系数，效率>1.0，样本大于传入参数，效率降序，限制8个结果
            QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("is_show", 1)
                    .eq("item_type", type)
                    .eq("exp_coefficient", expCoefficient)
                    .ge("efficiency", 0.6)
                    .ge("sample_size", sampleSize)
                    .orderByDesc("stage_efficiency")
                    .last("limit 8");
            List<StageResult> stageResultsByItemType = stageResultMapper.selectList(queryWrapper);

            if (stageResultsByItemType.size() == 0) throw new ServiceException(ResultCode.DATA_NONE);
            List<StageResultVo> stageResultVo_item = new ArrayList<>();
            stageResultsByItemType.forEach(stageResult -> {     //将关卡结果表的数据复制到前端返回对象上再返回
                StageResultVo stageResultVo = new StageResultVo();
                BeanUtils.copyProperties(stageResult, stageResultVo);

                stageResultVo_item.add(stageResultVo);
            });
            stageResultVoList.add(stageResultVo_item);
        });


        return stageResultVoList;
    }

    /**
     * 查询绿材料的推荐关卡
     *
     * @param expCoefficient 经验书系数，一般为0.625（还有1.0和0.0）
     * @param sampleSize     样本大小
     * @return
     */
    @RedisCacheable(key = "stageT2#expCoefficient")
    public List<List<StageResultVo>> queryStageResultData_t2(Double expCoefficient, Integer sampleSize) {
        List<List<StageResultVo>> stageResultVoList = new ArrayList<>();

        Arrays.asList("固源岩", "酮凝集", "聚酸酯", "糖", "异铁", "装置").forEach(item -> {
            QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
            //条件：可展示，符合材料名称，符合经验书系数，期望理智<50，期望理智升序，限制6个结果
            queryWrapper.eq("is_show", 1).eq("item_name", item)
                    .eq("exp_coefficient", expCoefficient)
                    .ge("sample_size", sampleSize)
                    .le("ap_expect", 50)
                    .orderByAsc("ap_expect")
                    .last("limit 6");
            List<StageResult> stageResultListByItemName = stageResultMapper.selectList(queryWrapper);
            if (stageResultListByItemName.size() == 0) throw new ServiceException(ResultCode.DATA_NONE);
            List<StageResultVo> stageResultVo_item = new ArrayList<>();
            stageResultListByItemName.forEach(stageResult -> {
                StageResultVo stageResultVo = new StageResultVo();    //将关卡结果表的数据复制到前端返回对象上再返回
                BeanUtils.copyProperties(stageResult, stageResultVo);
                stageResultVo_item.add(stageResultVo);
            });
            stageResultVoList.add(stageResultVo_item);
        });


        return stageResultVoList;
    }

    /**
     * 查询已关闭的活动
     *
     * @param expCoefficient 经验书系数，一般为0.625（还有1.0和0.0）
     * @param sampleSize     样本大小
     * @return
     */
    @RedisCacheable(key = "stageClosed#expCoefficient")
    public List<List<StageResultClosedVo>> queryStageResultData_closedActivities(Double expCoefficient, Integer sampleSize) {
        //条件：不可展示，符合材料名称，符合经验书系数，效率>1.0，材料类型不为空，样本大于传入参数，材料类型不为0，按stageId降序
        QueryWrapper<StageResult> queryWrapper = new QueryWrapper<StageResult>();
        queryWrapper.eq("is_show", 0)
                .isNotNull("item_type")
                .notLike("stage_code", "DH")
                .notLike("stage_code", "CF")
                .notLike("stage_code", "OD")
                .notLike("stage_id", "perm")
                .eq("exp_coefficient", expCoefficient)
                .ge("sample_size", sampleSize)
                .ge("item_rarity", 3)
                .ne("item_type", "0")
                .orderByDesc("stage_id")
                .orderByDesc("open_time");
        List<StageResult> stageResultListByIsShow = stageResultMapper.selectList(queryWrapper);
        List<StageResultClosedVo> stageResultListCopy = new ArrayList<>();
        stageResultListByIsShow.forEach(stageResult -> {
            StageResultClosedVo stageResultVo = new StageResultClosedVo();
            BeanUtils.copyProperties(stageResult, stageResultVo);      //将关卡结果表的数据复制到前端返回对象上再返回
            if (!stageResultVo.getStageCode().startsWith("CF"))
                stageResultVo.setStageEfficiency(stageResultVo.getStageEfficiency() + 7.2);  //给结果加上商店的无限龙门币的效率

            stageResultListCopy.add(stageResultVo);
        });


        List<List<StageResultClosedVo>> stageResultVoList = new ArrayList<>();  //返回的结果集合
        stageResultListCopy.stream()
                .collect(Collectors.groupingBy(StageResultClosedVo::getZoneName))  //根据zoneName分类结果
                .entrySet().stream()
                .sorted((p2, p1) -> p1.getValue().get(0).getOpenTime().compareTo(p2.getValue().get(0).getOpenTime())) //比较活动开启时间，倒序排列
                .forEach(entry -> stageResultVoList.add(new ArrayList<>(entry.getValue())));  //存入结果集合中


        return stageResultVoList;
    }

    /**
     * 查询搓玉推荐关卡
     *
     * @param expCoefficient 经验书系数，一般为0.625（还有1.0和0.0）
     * @param sampleSize     样本大小
     * @return
     */
//    @RedisCacheable(key = "stage/orundum")
    public List<OrundumPerApResultVo> queryStageResultData_Orundum(Double expCoefficient, Integer sampleSize) {
        List<OrundumPerApResultVo> orundumPerApResultVoList = new ArrayList<>();
        List<StageResult> stageResultByItemName = stageResultMapper.selectList(new QueryWrapper<StageResult>()
                .eq("is_show", 1)
                .eq("exp_coefficient", expCoefficient)
                .in("item_name", "固源岩", "源岩", "装置", "破损装置")
                .ge("sample_size", sampleSize)
                .orderByDesc("stage_efficiency"));

        if (stageResultByItemName.size() == 0) throw new ServiceException(ResultCode.DATA_NONE);
        stageResultByItemName.stream().collect(Collectors.groupingBy(StageResult::getStageId)).forEach((k, list) -> {
            if (!(list.get(0).getIsValue() == 0 && !(list.get(0).getStageId().endsWith("LMD")))) {
                HashMap<String, Double> calResult = orundumPerApCal(list);
                if (calResult.get("orundumPerAp") > 0.2) {

                    OrundumPerApResultVo resultVo = OrundumPerApResultVo.builder()
                            .stageCode(list.get(0).getStageCode())
                            .stageEfficiency(list.get(0).getStageEfficiency())
                            .orundumPerAp(calResult.get("orundumPerAp"))
                            .lMDCost(calResult.get("LMDCost"))
                            .orundumPerApEfficiency(calResult.get("orundumPerAp") / 1.09 * 100)
                            .build();
                    orundumPerApResultVoList.add(resultVo);
                }
            }
        });

        orundumPerApResultVoList.sort(Comparator.comparing(OrundumPerApResultVo::getOrundumPerAp).reversed());


//        log.info("搓玉效率结果数据正常");


        return orundumPerApResultVoList;
    }


    @RedisCacheable(key = "stageNewChapter#expCoefficient")
    public List<StageResultVo> queryStageResultDataByZoneName(String zone) {
        List<StageResult> stageResultsByZone = stageResultMapper.selectList(new QueryWrapper<StageResult>().eq("is_show", 1)
                .isNotNull("item_type").eq("exp_coefficient", 0.625).like("stage_code", zone)
                .orderByAsc("stage_id").last("limit 3"));

        List<StageResultVo> stageResultVoList = new ArrayList<>();
        stageResultsByZone.forEach(stageResult -> {
            StageResultVo stageResultVo = new StageResultVo();    //将关卡结果表的数据复制到前端返回对象上再返回
            BeanUtils.copyProperties(stageResult, stageResultVo);
            stageResultVoList.add(stageResultVo);
        });
        return stageResultVoList;
    }


    public List<StageResult> queryStageResultDataDetailByStageCode(String stageCode) {
        List<StageResult> stageResultsByStageCode = stageResultMapper.selectList(new QueryWrapper<StageResult>()
                .eq("exp_coefficient", 0.625).likeRight("stage_code", stageCode)
                .orderByAsc("stage_id"));
        if (stageResultsByStageCode == null || stageResultsByStageCode.size() < 1)
            throw new ServiceException(ResultCode.DATA_NONE);

        return stageResultsByStageCode;
    }

    public List<StageResult> queryStageResultDataDetailByStageId(String stageId) {
        QueryWrapper<StageResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("exp_coefficient", 0.625)
                .eq("stage_id", stageId)
                .orderByAsc("result");
        List<StageResult> stageResultsByStageCode = stageResultMapper.selectList(queryWrapper);
        if (stageResultsByStageCode == null || stageResultsByStageCode.size() < 1)
            throw new ServiceException(ResultCode.DATA_NONE);

        return stageResultsByStageCode;
    }


    /**
     * 搓玉计算
     *
     * @param stageResults 同一关里的可搓玉材料材料的结果集合
     * @return
     */
    private HashMap<String, Double> orundumPerApCal(List<StageResult> stageResults) {
        double item_30011 = 0.0, item_30012 = 0.0, item_30061 = 0.0, item_30062 = 0.0;

        for (StageResult result : stageResults) {
            switch (result.getItemId()) {
                case "30011":
                    item_30011 = result.getKnockRating() / result.getApCost();
                    break;
                case "30012":
                    item_30012 = result.getKnockRating() / result.getApCost();
                    break;
                case "30061":
                    item_30061 = result.getKnockRating() / result.getApCost();
                    break;
                case "30062":
                    item_30062 = result.getKnockRating() / result.getApCost();
                    break;
                default:
                    log.error("不是搓玉用的材料");
            }
        }

        double orundumPerAp = (item_30012 / 2 + item_30011 / 6 + item_30061 / 3 + item_30062) * 10;
        double LMDCost = ((item_30012 + item_30011 / 6) / 2 * 1600 + (item_30061 / 3 + item_30062) * 1000) * (600 / orundumPerAp) / 10000;

        HashMap<String, Double> hashMap = new HashMap<>();
        hashMap.put("orundumPerAp", orundumPerAp);
        hashMap.put("LMDCost", LMDCost);
        return hashMap;
    }


}
