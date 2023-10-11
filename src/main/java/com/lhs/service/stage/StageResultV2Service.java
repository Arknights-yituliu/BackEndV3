package com.lhs.service.stage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.entity.dto.stage.StageParamDTO;
import com.lhs.entity.po.stage.StageResultSample;
import com.lhs.entity.po.stage.StageResultV2;
import com.lhs.mapper.item.StageResultSampleMapper;
import com.lhs.mapper.item.StageResultV2Mapper;
import com.lhs.service.dev.OSSService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StageResultV2Service {

    private final StageResultV2Mapper stageResultV2Mapper;
    private final StageResultSampleMapper stageResultSampleMapper;
    private final ItemService itemService;
    private final StageService stageService;
    private final OSSService ossService;
    private final RedisTemplate<String, Object> redisTemplate;



    public StageResultV2Service(StageResultV2Mapper stageResultV2Mapper, StageResultSampleMapper stageResultSampleMapper, ItemService itemService, StageService stageService, OSSService ossService, RedisTemplate<String, Object> redisTemplate) {
        this.stageResultV2Mapper = stageResultV2Mapper;
        this.stageResultSampleMapper = stageResultSampleMapper;
        this.itemService = itemService;
        this.stageService = stageService;
        this.ossService = ossService;
        this.redisTemplate = redisTemplate;
    }

    public List<List<StageResultSample>> getStageResultDataT3V3(StageParamDTO stageParamDTO) {
        QueryWrapper<StageResultV2> resultV2QueryWrapper = new QueryWrapper<>();
        resultV2QueryWrapper.eq("version", stageParamDTO.getVersion())
                .eq("ratio_rank",0)
                .ge("end_time", new Date());

        List<StageResultV2> stageResultV2List = stageResultV2Mapper.selectList(resultV2QueryWrapper);
        Map<String, StageResultV2> collectByStageId = stageResultV2List.stream().filter(e -> e.getRatioRank() == 0).collect(Collectors.toMap((StageResultV2::getStageId), Function.identity()));

        QueryWrapper<StageResultSample> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("version", stageParamDTO.getVersion())
                .ge("end_time", new Date());
        List<StageResultSample> stageResultSampleList = stageResultSampleMapper.selectList(queryWrapper);

        Map<String, List<StageResultSample>> collect = stageResultSampleList
                .stream().collect(Collectors.groupingBy(StageResultSample::getItemType));

        List<List<StageResultSample>> list = new ArrayList<>();
        collect.forEach((k,v)->{
            v.sort(Comparator.comparing(StageResultSample::getStageEfficiency).reversed());
            List<StageResultSample> limitList = v.stream().limit(8).collect(Collectors.toList());
            list.add(limitList);
        });

        return list;
    }

//    private static void setStageColor(List<StageResultSample> stageResultSampleList,Map<String, StageResultV2> stageResultV2Map) {
//        if (stageResultSampleList.size() < 1) return;
//
//        stageResultSampleList = stageResultSampleList.stream()
//                .filter(e -> e.getStageColor() > 0).collect(Collectors.toList());
//
//        for (StageResultSample stageResultSample : stageResultSampleList) {
//            StageResultV2 stageResultV2 = stageResultV2Map.get(stageResultSample.getStageId());
//            stageResultSample.setStageColor(2);
//            if (stageResultSample.getItemRarity() == 2) {
//                double apExpect = stageResultV2.getApExpect() * 4 + 200 * 0.0036 - 1.17;
//                if ("30012".equals(stageResultSample.getMainItemId())) {
//                    apExpect = stageResultV2.getApExpect() * 5 + 200 * 0.0036 - 1.17;
//                }
//                stageResultV2.setApExpect(apExpect);
//            }
////            stageResult.setStageEfficiency(stageResult.getStageEfficiency() * 100);
//        }
//
//        String stageId_effMax = stageResultSampleList.get(0).getStageId();   //拿到效率最高的关卡id
//        stageResultSampleList.get(0).setStageColor(3);  //效率最高为3
//
//        stageResultSampleList = stageResultSampleList.stream()
//                .limit(8)  //限制个数
//                .sorted(Comparator.comparing(StageResultSample::getApExpect))  //根据期望理智排序
//                .collect(Collectors.toList());  //流转为集合
//
//        String stageId_expectMin = stageResultSampleList.get(0).getStageId(); //拿到期望理智最低的关卡id
//
//        if (stageId_effMax.equals(stageId_expectMin)) {  //对比俩个id是否一致
//            stageResultSampleList.get(0).setStageColor(4); // 一致为4
//        } else {
//            stageResultSampleList.get(0).setStageColor(1); // 不一致为1
//        }
//
//        for (StageResultSample stageResultSample : stageResultSampleList) {
//            StageResultV2 stageResultV2 = stageResultV2Map.get(stageResultSample.getStageId());
//            if (stageResultSample.getItemRarity() == 2) {
//                double apExpect = (stageResultV2.getApExpect() + 1.17 - 200 * 0.0036) / 4;
//
//                if ("30012".equals(stageResultSample.getMainItemId())) {
//                    apExpect = (stageResultV2.getApExpect() + 1.17 - 200 * 0.0036) / 5;
//                }
//                stageResultSample.setApExpect(apExpect);
//            }
//        }
//    }
}
