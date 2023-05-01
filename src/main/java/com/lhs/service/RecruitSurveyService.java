package com.lhs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.*;
import com.lhs.mapper.MaaRecruitMapper;
import com.lhs.mapper.MaaRecruitStatisticalMapper;
import com.lhs.mapper.RecruitDataMapper;
import com.lhs.mapper.ResultMapper;
import com.lhs.service.dto.MaaRecruitVo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecruitSurveyService {

    @Resource
    private MaaRecruitMapper maaRecruitMapper;
    @Resource
    private MaaRecruitStatisticalMapper maaStatisticalMapper;
    @Resource
    private ResultMapper resultMapper;
    @Resource
    private RecruitDataMapper recruitDataMapper;




    public String saveMaaRecruitDataNew(MaaRecruitVo maaRecruitVo) {

        Date date = new Date();
        int end3 = new Random().nextInt(9999);

        RecruitData recruitData = RecruitData.builder()
                .id(date.getTime() * 1000 + end3)
                .uid(maaRecruitVo.getUuid())
                .level(maaRecruitVo.getLevel())
                .server(maaRecruitVo.getServer())
                .source(maaRecruitVo.getSource())
                .tag(JSON.toJSONString(maaRecruitVo.getTags()))
                .version(maaRecruitVo.getVersion())
                .createTime(date).build();

        recruitDataMapper.insertRecruitData("recruit_data_1",recruitData);

        return null;
    }

    public Map<String, Integer> recruitStatistics() {

        List<RecruitStatistics> recruitStatistics = recruitDataMapper.selectRecruitStatistics();
        Map<String, Integer> recruitStatisticsMap = recruitStatistics.stream().collect(Collectors.toMap(RecruitStatistics::getStatisticalItem, RecruitStatistics::getStatisticalResult));

        RecruitStatisticsConfig config = recruitDataMapper.selectConfigByKey("lastTime");
        long lastTime = Long.parseLong(config.getConfigValue());
        Date date = new Date();


        List<RecruitData> recruit_data_1 = recruitDataMapper.selectRecruitDataByCreateTime("recruit_data_1", new Date(lastTime), date);
        Map<Integer, List<RecruitData>> collect = recruit_data_1.stream().collect(Collectors.groupingBy(RecruitData::getLevel));

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
        if(collect.get(6)!=null){
            topOperatorResult = collect.get(6);
        }

        List<RecruitData> seniorOperatorCountResult = new ArrayList<>();
        if(collect.get(5)!=null){
            seniorOperatorCountResult = collect.get(5);
        }

        List<RecruitData> rareOperatorCountResult = new ArrayList<>();
        if(collect.get(4)!=null){
            rareOperatorCountResult = collect.get(4);
        }

        List<RecruitData> commonOperatorCountResult = new ArrayList<>();
        if(collect.get(4)!=null){
            commonOperatorCountResult = collect.get(4);
        }



        topOperator = topOperatorResult.size();
        seniorOperatorCount = seniorOperatorCountResult.size();  //五星TAG总数
        rareOperatorCount = rareOperatorCountResult.size();   //四星TAG总数
        commonOperatorCount = commonOperatorCountResult.size(); //三星TAG总数


        for(RecruitData recruitData:topOperatorResult){
            List<String> tags = JSONArray.parseArray(recruitData.getTag(), String.class);

            boolean vulcanSignMain = false; //火神标记
            boolean vulcanSignItem = false; //火神标记

            for(String tag:tags){
                if("资深干员".equals(tag)) {
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

            if(vulcanSignItem&&vulcanSignMain) {
                vulcan++;
            }
        }

        for(RecruitData recruitData:seniorOperatorCountResult){
            List<String> tags = JSONArray.parseArray(recruitData.getTag(), String.class);

            boolean vulcanSignMain = false; //火神标记
            boolean vulcanSignItem = false; //火神标记

            for(String tag:tags){
                if("资深干员".equals(tag)) {
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

            if(vulcanSignItem&&vulcanSignMain) {
                vulcan++;
            }
        }

        for(RecruitData recruitData:seniorOperatorCountResult){
            List<String> tags = JSONArray.parseArray(recruitData.getTag(), String.class);
            for(String tag:tags){
                if ("支援机械".equals(tag)) {
                    robot++;
                    robotChoice++;
                }
            }
        }

        for(RecruitData recruitData:commonOperatorCountResult){
            List<String> tags = JSONArray.parseArray(recruitData.getTag(), String.class);
            for(String tag:tags){
                if ("支援机械".equals(tag)) {
                    robot++;
                }
            }
        }


        Map<String, Integer> resultMap = new HashMap<>();
        resultMap.put("topOperator",topOperator);
        resultMap.put("seniorOperator",seniorOperator);
        resultMap.put("topAndSeniorOperator",topAndSeniorOperator);
        resultMap.put("seniorOperatorCount",seniorOperatorCount);
        resultMap.put("rareOperatorCount",rareOperatorCount);
        resultMap.put("commonOperatorCount",commonOperatorCount);
        resultMap.put("robot",robot);
        resultMap.put("robotChoice",robotChoice);
        resultMap.put("vulcan",vulcan);
        resultMap.put("maaRecruitDataCount",recruit_data_1.size());

        resultMap.forEach((k,v)->{
            recruitStatisticsMap.merge(k,v,Integer::sum);
        });

        for(String key:recruitStatisticsMap.keySet()){

            RecruitStatistics resultByItem = recruitDataMapper.selectRecruitStatisticsByItem(key);

            RecruitStatistics build = RecruitStatistics.builder()
                    .statisticalItem(key)
                    .statisticalResult(recruitStatisticsMap.get(key))
                    .build();
            if(resultByItem!=null){
                recruitDataMapper.updateRecruitStatistics(build);
            }else {
                recruitDataMapper.insertRecruitStatistics(build);
            }
        }

        recruitDataMapper.updateConfigByKey("lastTime",String.valueOf(date.getTime()) );


        return recruitStatisticsMap;
    }


    public HashMap<String,Object> statisticalResult() {

        List<RecruitStatistics> recruitStatistics = recruitDataMapper.selectRecruitStatistics();
        Map<String, Integer> recruitStatisticsMap = recruitStatistics.stream().collect(Collectors.toMap(RecruitStatistics::getStatisticalItem, RecruitStatistics::getStatisticalResult));
        RecruitStatisticsConfig config = recruitDataMapper.selectConfigByKey("lastTime");
        HashMap<String, Object> hashMap = new HashMap<>(recruitStatisticsMap);
        hashMap.put("createTime",Long.valueOf(config.getConfigValue()));

        return hashMap;
    }
}
