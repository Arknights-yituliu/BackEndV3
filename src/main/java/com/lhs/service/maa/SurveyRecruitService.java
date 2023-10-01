package com.lhs.service.maa;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.lhs.entity.po.maa.SurveyRecruit;
import com.lhs.entity.po.maa.RecruitStatistics;
import com.lhs.mapper.survey.SurveyRecruitMapper;
import com.lhs.entity.vo.maa.MaaRecruitVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SurveyRecruitService {



    private final SurveyRecruitMapper surveyRecruitMapper;

    private final RedisTemplate<String,Object> redisTemplate;

    public SurveyRecruitService(SurveyRecruitMapper surveyRecruitMapper, RedisTemplate<String, Object> redisTemplate) {
        this.surveyRecruitMapper = surveyRecruitMapper;
        this.redisTemplate = redisTemplate;
    }

    public String saveMaaRecruitDataNew(MaaRecruitVo maaRecruitVo) {

        Date date = new Date();
        Long maaRecruitId = redisTemplate.opsForValue().increment("MaaRecruitId");
        Object surveyRecruitTable = redisTemplate.opsForValue().get("SurveyRecruitTable");
        if(surveyRecruitTable==null) {
            redisTemplate.opsForValue().set("SurveyRecruitTable",3);
            surveyRecruitTable = 3;
        }
        String tableName = "survey_recruit_"+surveyRecruitTable;

        SurveyRecruit surveyRecruit = SurveyRecruit.builder()
                .id(maaRecruitId)
                .uid(maaRecruitVo.getUuid())
                .level(maaRecruitVo.getLevel())
                .server(maaRecruitVo.getServer())
                .source(maaRecruitVo.getSource())
                .tag(JSON.toJSONString(maaRecruitVo.getTags()))
                .version(maaRecruitVo.getVersion())
                .createTime(date).build();

        surveyRecruitMapper.insertRecruitData(tableName, surveyRecruit);

        return null;
    }

    public Map<String, Integer> recruitStatistics() {

        List<RecruitStatistics> recruitStatistics = surveyRecruitMapper.selectRecruitStatistics();

        //上次统计的结果根据统计项目分类
        Map<String, Integer> recruitStatisticsMap = recruitStatistics.stream()
                .collect(Collectors.toMap(RecruitStatistics::getStatisticalItem, RecruitStatistics::getStatisticalResult));

        Date date = new Date();

        Object lastStatisticsTime = redisTemplate.opsForValue().get("LastRecruitStatisticsTime");
        if(lastStatisticsTime==null) lastStatisticsTime = date.getTime();
        long lastTime = Long.parseLong(String.valueOf(lastStatisticsTime));


        Object surveyRecruitTable = redisTemplate.opsForValue().get("SurveyRecruitTable");
        if(surveyRecruitTable==null) {
            redisTemplate.opsForValue().set("SurveyRecruitTable",3);
            surveyRecruitTable = 3;
        }

        String tableName = "survey_recruit_"+surveyRecruitTable;

        List<SurveyRecruit> recruit_data_DB = surveyRecruitMapper.selectRecruitDataByCreateTime(tableName, new Date(lastTime), date);

        Map<Integer, List<SurveyRecruit>> collect = recruit_data_DB.stream()
                .collect(Collectors.groupingBy(SurveyRecruit::getLevel));

        int topOperator = 0;  //高级资深总数
        int seniorOperator = 0; //资深总数
        int topAndSeniorOperator = 0; //高级资深含有资深总数
        int seniorOperatorCount = 0;  //五星TAG总数
        int rareOperatorCount = 0;   //四星TAG总数
        int commonOperatorCount = 0; //三星TAG总数
        int robot = 0;                //小车TAG总数
        int robotChoice = 0;       //小车和其他组合共同出现次数
        int vulcan = 0;             //火神出现次数


        List<SurveyRecruit> topOperatorResult = new ArrayList<>();
        if(collect.get(6)!=null){
            topOperatorResult = collect.get(6);
        }

        List<SurveyRecruit> seniorOperatorCountResult = new ArrayList<>();
        if(collect.get(5)!=null){
            seniorOperatorCountResult = collect.get(5);
        }

        List<SurveyRecruit> rareOperatorCountResult = new ArrayList<>();
        if(collect.get(4)!=null){
            rareOperatorCountResult = collect.get(4);
        }

        List<SurveyRecruit> commonOperatorCountResult = new ArrayList<>();
        if(collect.get(3)!=null){
            commonOperatorCountResult = collect.get(3);
        }



        topOperator = topOperatorResult.size();
        seniorOperatorCount = seniorOperatorCountResult.size();  //五星TAG总数
        rareOperatorCount = rareOperatorCountResult.size();   //四星TAG总数
        commonOperatorCount = commonOperatorCountResult.size(); //三星TAG总数


        for(SurveyRecruit surveyRecruit :topOperatorResult){
            List<String> tags = JSONArray.parseArray(surveyRecruit.getTag(), String.class);

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

        for(SurveyRecruit surveyRecruit :seniorOperatorCountResult){
            List<String> tags = JSONArray.parseArray(surveyRecruit.getTag(), String.class);

            boolean vulcanSignMain = false; //火神主标签
            boolean vulcanSignItem = false; //火神副标签

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

        for(SurveyRecruit surveyRecruit :seniorOperatorCountResult){
            List<String> tags = JSONArray.parseArray(surveyRecruit.getTag(), String.class);
            for(String tag:tags){
                if ("支援机械".equals(tag)) {
                    robot++;
                    robotChoice++;
                }
            }
        }

        for(SurveyRecruit surveyRecruit :commonOperatorCountResult){
            List<String> tags = JSONArray.parseArray(surveyRecruit.getTag(), String.class);
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
        resultMap.put("maaRecruitDataCount",recruit_data_DB.size());

        resultMap.forEach((k,v)->{
            recruitStatisticsMap.merge(k,v,Integer::sum);
        });

        for(String key:recruitStatisticsMap.keySet()){

            RecruitStatistics resultByItem = surveyRecruitMapper.selectRecruitStatisticsByItem(key);

            RecruitStatistics build = RecruitStatistics.builder()
                    .statisticalItem(key)
                    .statisticalResult(recruitStatisticsMap.get(key))
                    .build();
            if(resultByItem!=null){
                surveyRecruitMapper.updateRecruitStatistics(build);
            }else {
                surveyRecruitMapper.insertRecruitStatistics(build);
            }
        }

        redisTemplate.opsForValue().set("LastRecruitStatisticsTime",date.getTime());

        return recruitStatisticsMap;
    }


    public HashMap<String,Object> statisticalResult() {

        List<RecruitStatistics> recruitStatistics = surveyRecruitMapper.selectRecruitStatistics();
        Map<String, Integer> recruitStatisticsMap = recruitStatistics.stream().collect(Collectors.toMap(RecruitStatistics::getStatisticalItem, RecruitStatistics::getStatisticalResult));
        Object lastStatisticsTime = redisTemplate.opsForValue().get("LastRecruitStatisticsTime");
        long lastTime = Long.parseLong(String.valueOf(lastStatisticsTime));
        HashMap<String, Object> hashMap = new HashMap<>(recruitStatisticsMap);
        hashMap.put("createTime",Long.valueOf(String.valueOf(lastTime)));

        return hashMap;
    }


}
