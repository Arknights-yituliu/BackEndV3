package com.lhs.service.maa;


import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.po.maa.RecruitData;
import com.lhs.entity.po.maa.RecruitStatistics;
import com.lhs.mapper.survey.RecruitDataMapper;
import com.lhs.entity.vo.maa.MaaRecruitVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecruitTagUploadService {


    private final RecruitDataMapper recruitDataMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    public RecruitTagUploadService(RecruitDataMapper recruitDataMapper, RedisTemplate<String, Object> redisTemplate) {
        this.recruitDataMapper = recruitDataMapper;
        this.redisTemplate = redisTemplate;
    }

    private String getDBTableIndex() {
        return "survey_recruit_6";
    }

    public String saveMaaRecruitDataNew(MaaRecruitVo maaRecruitVo) {
        Long maaRecruitId = redisTemplate.opsForValue().increment("MaaRecruitId");
        String tableName = getDBTableIndex();
        RecruitData recruitData = RecruitData.builder()
                .id(maaRecruitId)
                .uid(maaRecruitVo.getUuid())
                .level(maaRecruitVo.getLevel())
                .server(maaRecruitVo.getServer())
                .source(maaRecruitVo.getSource())
                .tag(JsonMapper.toJSONString(maaRecruitVo.getTags()))
                .version(maaRecruitVo.getVersion())
                .createTime(System.currentTimeMillis())
                .build();
        recruitDataMapper.insertRecruitData(tableName, recruitData);

        return null;
    }

    public Map<String, Integer> recruitStatistics() {

        List<RecruitStatistics> recruitStatistics = recruitDataMapper.selectRecruitStatistics();

        //上次统计的结果根据统计项目分类
        Map<String, Integer> recruitStatisticsMap = recruitStatistics.stream()
                .collect(Collectors.toMap(RecruitStatistics::getStatisticalItem, RecruitStatistics::getStatisticalResult));

        Date date = new Date();

        Object lastStatisticsTime = redisTemplate.opsForValue().get("LastRecruitStatisticsTime");
        if (lastStatisticsTime == null) lastStatisticsTime = date.getTime() - 10000000;
        long lastTime = Long.parseLong(String.valueOf(lastStatisticsTime));


        String tableName = getDBTableIndex();

        List<RecruitData> recruit_data_DB = recruitDataMapper.selectRecruitDataByCreateTime(tableName, lastTime, date.getTime());


        Map<Integer, List<RecruitData>> collect = recruit_data_DB.stream()
                .collect(Collectors.groupingBy(RecruitData::getLevel));

        int topOperator = 0;  //高级资深总数
        int seniorOperator = 0; //资深总数
        int topAndSeniorOperator = 0; //高级资深含有资深总数
        int seniorOperatorCount = 0;  //五星TAG总数
        int rareOperatorCount = 0;   //四星TAG总数
        int commonOperatorCount = 0; //三星TAG总数
        int robot = 0;                //小车TAG总数
        int robotChoice = 0;       //小车和其他组合共同出现次数
        int vulcan = 0;             //火神出现次数


        List<RecruitData> topOperatorResult = new ArrayList<>();
        if (collect.get(6) != null) {
            topOperatorResult = collect.get(6);
        }

        List<RecruitData> seniorOperatorCountResult = new ArrayList<>();
        if (collect.get(5) != null) {
            seniorOperatorCountResult = collect.get(5);
        }

        List<RecruitData> rareOperatorCountResult = new ArrayList<>();
        if (collect.get(4) != null) {
            rareOperatorCountResult = collect.get(4);
        }

        List<RecruitData> commonOperatorCountResult = new ArrayList<>();
        if (collect.get(3) != null) {
            commonOperatorCountResult = collect.get(3);
        }


        topOperator = topOperatorResult.size();
        seniorOperatorCount = seniorOperatorCountResult.size();  //五星TAG总数
        rareOperatorCount = rareOperatorCountResult.size();   //四星TAG总数
        commonOperatorCount = commonOperatorCountResult.size(); //三星TAG总数


        for (RecruitData recruitData : topOperatorResult) {
            List<String> tags = JsonMapper.parseJSONArray(recruitData.getTag(), new TypeReference<List<String>>() {
            });


            boolean vulcanSignMain = false; //火神标记
            boolean vulcanSignItem = false; //火神标记

            for (String tag : tags) {
                if ("资深干员".equals(tag)) {
                    topAndSeniorOperator++;
                    seniorOperator++;
                }

                if ("生存".equals(tag)) {
                    vulcanSignMain = true;
                }
                if ("重装干员".equals(tag) || "防护".equals(tag)) {
                    vulcanSignItem = true;
                }

                if ("支援机械".equals(tag)) {
                    robot++;
                    robotChoice++;
                }
            }

            if (vulcanSignItem && vulcanSignMain) {
                vulcan++;
            }
        }

        for (RecruitData recruitData : seniorOperatorCountResult) {
            List<String> tags = JsonMapper.parseJSONArray(recruitData.getTag(), new TypeReference<List<String>>() {
            });

            boolean vulcanSignMain = false; //火神主标签
            boolean vulcanSignItem = false; //火神副标签

            for (String tag : tags) {
                if ("资深干员".equals(tag)) {
                    seniorOperator++;
                }

                if ("生存".equals(tag)) {
                    vulcanSignMain = true;
                }
                if ("重装干员".equals(tag) || "防护".equals(tag)) {
                    vulcanSignItem = true;
                }

                if ("支援机械".equals(tag)) {
                    robotChoice++;
                    robot++;
                }
            }

            if (vulcanSignItem && vulcanSignMain) {
                vulcan++;
            }
        }

        for (RecruitData recruitData : seniorOperatorCountResult) {
            List<String> tags = JsonMapper.parseJSONArray(recruitData.getTag(), new TypeReference<List<String>>() {
            });
            for (String tag : tags) {
                if ("支援机械".equals(tag)) {
                    robot++;
                    robotChoice++;
                }
            }
        }

        for (RecruitData recruitData : commonOperatorCountResult) {
            List<String> tags = JsonMapper.parseJSONArray(recruitData.getTag(), new TypeReference<List<String>>() {
            });
            for (String tag : tags) {
                if ("支援机械".equals(tag)) {
                    robot++;
                }
            }
        }


        Map<String, Integer> resultMap = new HashMap<>();
        resultMap.put("topOperator", topOperator);
        resultMap.put("seniorOperator", seniorOperator);
        resultMap.put("topAndSeniorOperator", topAndSeniorOperator);
        resultMap.put("seniorOperatorCount", seniorOperatorCount);
        resultMap.put("rareOperatorCount", rareOperatorCount);
        resultMap.put("commonOperatorCount", commonOperatorCount);
        resultMap.put("robot", robot);
        resultMap.put("robotChoice", robotChoice);
        resultMap.put("vulcan", vulcan);
        resultMap.put("maaRecruitDataCount", recruit_data_DB.size());

        resultMap.forEach((k, v) -> {
            recruitStatisticsMap.merge(k, v, Integer::sum);
        });

        for (String key : recruitStatisticsMap.keySet()) {

            RecruitStatistics resultByItem = recruitDataMapper.selectRecruitStatisticsByItem(key);

            RecruitStatistics build = RecruitStatistics.builder()
                    .statisticalItem(key)
                    .statisticalResult(recruitStatisticsMap.get(key))
                    .build();
            if (resultByItem != null) {
                recruitDataMapper.updateRecruitStatistics(build);
            } else {
                recruitDataMapper.insertRecruitStatistics(build);
            }
        }

        redisTemplate.opsForValue().set("LastRecruitStatisticsTime", date.getTime());

        return recruitStatisticsMap;
    }


    public HashMap<String, Object> statisticalResult() {

        List<RecruitStatistics> recruitStatistics = recruitDataMapper.selectRecruitStatistics();
        Map<String, Integer> recruitStatisticsMap = recruitStatistics.stream().collect(Collectors.toMap(RecruitStatistics::getStatisticalItem, RecruitStatistics::getStatisticalResult));
        Object lastStatisticsTime = redisTemplate.opsForValue().get("LastRecruitStatisticsTime");
        long lastTime = Long.parseLong(String.valueOf(lastStatisticsTime));
        HashMap<String, Object> hashMap = new HashMap<>(recruitStatisticsMap);
        hashMap.put("createTime", Long.valueOf(String.valueOf(lastTime)));

        return hashMap;
    }


}
