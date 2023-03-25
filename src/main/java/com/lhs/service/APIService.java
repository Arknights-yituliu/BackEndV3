package com.lhs.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.config.FileConfig;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.HttpRequestUtil;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.ResultVo;
import com.lhs.entity.StageResult;

import com.lhs.mapper.StageResultMapper;
import com.lhs.service.resultVo.OrundumPerApResultVo;
import com.lhs.service.resultVo.StageResultActVo;
import com.lhs.service.resultVo.StageResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class APIService {

    @Autowired
    private StageResultMapper stageResultMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static QueryWrapper<ResultVo> ResultVoWrapper(String path) {
        return new QueryWrapper<ResultVo>().eq("path", path).orderByDesc("create_time").last("limit 1");
    }

    /**
     * 根据key查找redis中缓存的数据的通用方法
     * @param key redis的key
     * @return  redis缓存的数据
     */
    public Object queryResultByApiPath(String key) {
        Object resultVo = redisTemplate.opsForValue().get(key);
        if (resultVo == null) throw new ServiceException(ResultCode.DATA_NONE);
        return resultVo;
    }

    /**
     * 查询蓝材料的推荐关卡
     * @param expCoefficient  经验书系数，一般为0.625（还有1.0和0.0）
     * @param sampleSize  样本大小
     * @return
     */
    public List<List<StageResultVo>> queryStageResultData_t3(Double expCoefficient, Integer sampleSize) {
        List<List<StageResultVo>> stageResultVoList = new ArrayList<>();
        Arrays.asList("全新装置", "异铁组", "轻锰矿", "凝胶", "扭转醇", "酮凝集组", "RMA70-12", "炽合金", "研磨石", "糖组",
                "聚酸酯组", "晶体元件", "固源岩组", "半自然溶剂", "化合切削液", "转质盐组").forEach(type -> {
                    List<StageResult> stageResultsByItemType = stageResultMapper.selectList(new QueryWrapper<StageResult>().eq("is_show", 1).eq("item_type", type)
                            .eq("exp_coefficient", expCoefficient).ge("efficiency", 1.0).ge("sample_size", sampleSize)
                            .orderByDesc("stage_efficiency").last("limit 8"));      //条件：可展示，符合材料类型，符合经验书系数，效率>1.0，样本大于传入参数，效率降序，限制8个结果

                    if (stageResultsByItemType.size() == 0) throw new ServiceException(ResultCode.DATA_NONE);
                    List<StageResultVo> stageResultVo_item = new ArrayList<>();
                    stageResultsByItemType.forEach(stageResult -> {     //将关卡结果表的数据复制到前端返回对象上再返回
                        StageResultVo stageResultVo = new StageResultVo();
                        BeanUtils.copyProperties(stageResult, stageResultVo);
                        stageResultVo_item.add(stageResultVo);
                    });
                    stageResultVoList.add(stageResultVo_item);
                });


        redisTemplate.opsForValue().set("stage/t3/" + expCoefficient, stageResultVoList, 1800, TimeUnit.SECONDS);
        return stageResultVoList;
    }

    /**
     * 查询绿材料的推荐关卡
     * @param expCoefficient  经验书系数，一般为0.625（还有1.0和0.0）
     * @param sampleSize  样本大小
     * @return
     */
    public List<List<StageResultVo>> queryStageResultData_t2(Double expCoefficient, Integer sampleSize) {
        List<List<StageResultVo>> stageResultVoList = new ArrayList<>();

        Arrays.asList("固源岩", "酮凝集", "聚酸酯", "糖", "异铁", "装置").forEach(item -> {
            List<StageResult> stageResultListByItemName = stageResultMapper.selectList(new QueryWrapper<StageResult>().eq("is_show", 1).eq("item_name", item)
                    .eq("exp_coefficient", expCoefficient).ge("sample_size", sampleSize).le("ap_expect", 50)
                    .orderByAsc("ap_expect").last("limit 6"));//条件：可展示，符合材料名称，符合经验书系数，期望理智<50，期望理智升序，限制6个结果
            if (stageResultListByItemName.size() == 0) throw new ServiceException(ResultCode.DATA_NONE);
            List<StageResultVo> stageResultVo_item = new ArrayList<>();
            stageResultListByItemName.forEach(stageResult -> {
                StageResultVo stageResultVo = new StageResultVo();    //将关卡结果表的数据复制到前端返回对象上再返回
                BeanUtils.copyProperties(stageResult, stageResultVo);
                stageResultVo_item.add(stageResultVo);
            });
            stageResultVoList.add(stageResultVo_item);
        });


        redisTemplate.opsForValue().set("stage/t2/" + expCoefficient, stageResultVoList, 1800, TimeUnit.SECONDS);
        return stageResultVoList;
    }

    /**
     * 查询已关闭的活动
     * @param expCoefficient  经验书系数，一般为0.625（还有1.0和0.0）
     * @param sampleSize  样本大小
     * @return
     */
    public List<List<StageResultActVo>> queryStageResultData_closedActivities(Double expCoefficient, Integer sampleSize) {
        List<StageResult> stageResultListByIsShow = stageResultMapper.selectList(new QueryWrapper<StageResult>().eq("is_show", 0)
                .isNotNull("item_type").notLike("stage_code","DH").eq("exp_coefficient", expCoefficient)
                .ge("sample_size", sampleSize).ge("item_rarity", 3).ne("item_type", "0").orderByDesc("stage_id")
                .orderByDesc("open_time")); //条件：不可展示，符合材料名称，符合经验书系数，效率>1.0，材料类型不为空，样本大于传入参数，材料类型不为0，按stageId降序
        List<StageResultActVo> stageResultListCopy = new ArrayList<>();
        stageResultListByIsShow.forEach(stageResult -> {
            StageResultActVo stageResultVo = new StageResultActVo();
            BeanUtils.copyProperties(stageResult, stageResultVo);      //将关卡结果表的数据复制到前端返回对象上再返回
            stageResultVo.setStageEfficiency(stageResultVo.getStageEfficiency()+7.2);  //给结果加上商店的无限龙门币的效率

            stageResultListCopy.add(stageResultVo);
        });


        List<List<StageResultActVo>>  stageResultVoList = new ArrayList<>();  //返回的结果集合
        stageResultListCopy.stream()
                .collect(Collectors.groupingBy(StageResultActVo::getZoneName))  //根据zoneName分类结果
                .entrySet().stream()
                .sorted((p2, p1) -> p1.getValue().get(0).getOpenTime().compareTo(p2.getValue().get(0).getOpenTime())) //比较活动开启时间，倒序排列
                .forEach(entry -> stageResultVoList.add( new ArrayList<>(entry.getValue())));  //存入结果集合中

        redisTemplate.opsForValue().set("stage/closed/" + expCoefficient, stageResultVoList, 1800, TimeUnit.SECONDS);
        return stageResultVoList;
    }

    /**
     * 查询搓玉推荐关卡
     * @param expCoefficient  经验书系数，一般为0.625（还有1.0和0.0）
     * @param sampleSize  样本大小
     * @return
     */
    public List<OrundumPerApResultVo> queryStageResultData_Orundum(Double expCoefficient, Integer sampleSize) {
        List<OrundumPerApResultVo> OrundumPerApResultVoList = new ArrayList<>();
        List<StageResult> stageResultByItemName = stageResultMapper.selectList(new QueryWrapper<StageResult>().eq("is_show", 1).eq("exp_coefficient", expCoefficient)
                .in("item_name", "固源岩", "源岩", "装置", "破损装置").ge("sample_size", sampleSize).orderByDesc("stage_efficiency"));
        if (stageResultByItemName.size() == 0) throw new ServiceException(ResultCode.DATA_NONE);
        stageResultByItemName.stream().collect(Collectors.groupingBy(StageResult::getStageId)).forEach((k, list) -> {
            HashMap<String, Double> calResult = orundumPerApCal(list);
            OrundumPerApResultVo resultVo = OrundumPerApResultVo.builder().stageCode(list.get(0).getStageCode()).stageEfficiency(list.get(0).getStageEfficiency())
                    .orundumPerAp(calResult.get("orundumPerAp")).lMDCost(calResult.get("LMDCost")).orundumPerApEfficiency(calResult.get("orundumPerAp") / 1.09*100).build();
            OrundumPerApResultVoList.add(resultVo);
        });
        OrundumPerApResultVoList.sort(Comparator.comparing(OrundumPerApResultVo::getOrundumPerAp).reversed());
        log.info("搓玉效率结果数据正常");
        redisTemplate.opsForValue().set("stage/orundum", OrundumPerApResultVoList);
        return OrundumPerApResultVoList;
    }

    /**
     * 搓玉计算
     * @param stageResults  同一关里的可搓玉材料材料的结果集合
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

    /**
     * 保存企鹅物流数据到本地
     * @param dataType  企鹅有两种数据，一种是仅MAA上传的数据，参数值auto；一种是全局数据，参数值global
     * @param url  企鹅的数据API链接
     */
    public void savePenguinData(String dataType,String url) {
        String response = HttpRequestUtil.doGet(url,new HashMap<>());
        String saveTime = new SimpleDateFormat("yyyy-MM-dd HH").format(new Date()); // 设置日期格式
        FileUtil.save(FileConfig.Penguin, "matrix " + saveTime +" " + dataType + ".json", response);

    }
}
