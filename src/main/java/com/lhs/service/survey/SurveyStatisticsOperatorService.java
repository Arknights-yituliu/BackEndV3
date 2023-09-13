package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Log;
import com.lhs.entity.survey.SurveyOperatorVo;
import com.lhs.entity.survey.SurveyStatisticsOperator;
import com.lhs.mapper.survey.SurveyOperatorVoMapper;
import com.lhs.mapper.survey.SurveyStatisticsOperatorMapper;
import com.lhs.service.dev.OSSService;
import com.lhs.vo.survey.OperatorStatisticsResult;
import com.lhs.vo.survey.SurveyStatisticsChar;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SurveyStatisticsOperatorService {


    private final SurveyStatisticsOperatorMapper surveyStatisticsOperatorMapper;

    private final SurveyUserService surveyUserService;

    private final SurveyOperatorVoMapper surveyOperatorVoMapper;

    private final OperatorBaseDataService operatorBaseDataService;

    private final RedisTemplate<String,Object> redisTemplate;

    private final OSSService ossService;


    public SurveyStatisticsOperatorService(SurveyStatisticsOperatorMapper surveyStatisticsOperatorMapper, SurveyUserService surveyUserService, SurveyOperatorVoMapper surveyOperatorVoMapper, OperatorBaseDataService operatorBaseDataService, RedisTemplate<String, Object> redisTemplate, OSSService ossService) {
        this.surveyStatisticsOperatorMapper = surveyStatisticsOperatorMapper;
        this.surveyUserService = surveyUserService;
        this.surveyOperatorVoMapper = surveyOperatorVoMapper;
        this.operatorBaseDataService = operatorBaseDataService;
        this.redisTemplate = redisTemplate;
        this.ossService = ossService;
    }

    /**
     * 干员练度调查表统计
     */
    @Scheduled(cron = "0 10 0/1 * * ?")
    public void operatorStatistics() {
        List<Long> userIds = surveyUserService.selectSurveyUserIds();


        List<List<Long>> userIdsGroup = new ArrayList<>();
        String updateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        redisTemplate.opsForHash().put("Survey", "UpdateTime.Operator", updateTime);
        redisTemplate.opsForHash().put("Survey", "UserCount.Operator", userIds.size());

        Map<String, String> mod_table = operatorBaseDataService.getHasEquipTable();

        int length = userIds.size();
        // 计算用户id按500个用户一组可以分成多少组
        int num = length / 100 + 1;
        int fromIndex = 0;   // id分组开始
        int toIndex = 100;   //id分组结束
        for (int i = 0; i < num; i++) {
            toIndex = Math.min(toIndex, userIds.size());
            userIdsGroup.add(userIds.subList(fromIndex, toIndex));
            fromIndex += 100;
            toIndex += 100;
        }

        surveyStatisticsOperatorMapper.truncate(); //清空统计表

        HashMap<String, SurveyStatisticsChar> hashMap = new HashMap<>();  //结果暂存对象

        List<SurveyStatisticsOperator> statisticsOperatorList = new ArrayList<>();  //最终结果


        for (List<Long> ids : userIdsGroup) {
            if (ids.size() == 0) continue;

            QueryWrapper<SurveyOperatorVo> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("uid",ids);
            List<SurveyOperatorVo> surveyOperatorVoByBindUser = surveyOperatorVoMapper.selectList(queryWrapper);

            Log.info("本次统计数量：" + surveyOperatorVoByBindUser.size());

            //根据干员id分组
            Map<String, List<SurveyOperatorVo>> collectByCharId = surveyOperatorVoByBindUser.stream()
                    .collect(Collectors.groupingBy(SurveyOperatorVo::getCharId));

            //计算结果
            collectByCharId.forEach((charId, list) -> {
                list = list.stream()
                        .filter(SurveyOperatorVo::getOwn)
                        .collect(Collectors.toList());

                int own = list.size();  //持有人数
                int rarity = list.get(0).getRarity();  //星级

                //根据该干员精英等级分组统计 map(精英等级,该等级的数量)
                Map<Integer, Long> collectByElite = list.stream()
                        .collect(Collectors.groupingBy(SurveyOperatorVo::getElite, Collectors.counting()));

                //根据该干员的潜能等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectByPotential = list.stream()
                        .collect(Collectors.groupingBy(SurveyOperatorVo::getPotential, Collectors.counting()));

                //根据该干员的技能专精等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectBySkill1 = list.stream()
                        .collect(Collectors.groupingBy(SurveyOperatorVo::getSkill1, Collectors.counting()));

                Map<Integer, Long> collectBySkill2 = list.stream()
                        .collect(Collectors.groupingBy(SurveyOperatorVo::getSkill2, Collectors.counting()));

                Map<Integer, Long> collectBySkill3 = list.stream()
                        .collect(Collectors.groupingBy(SurveyOperatorVo::getSkill3, Collectors.counting()));

                //根据该干员的模组等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectByModX = list.stream()
                        .filter(e->mod_table.get(charId+"_X")!=null)
                        .collect(Collectors.groupingBy(SurveyOperatorVo::getModX, Collectors.counting()));

                Map<Integer, Long> collectByModY = list.stream()
                        .filter(e->mod_table.get(charId+"_Y")!=null)
                        .collect(Collectors.groupingBy(SurveyOperatorVo::getModY, Collectors.counting()));

                //和上一组用户id的数据合并
                if (hashMap.get(charId) != null) {
                    SurveyStatisticsChar lastData = hashMap.get(charId);
                    own += lastData.getOwn();

                    lastData.getElite()
                            .forEach((k, v) -> collectByElite.merge(k, v, Long::sum));

                    lastData.getPotential()
                            .forEach((k, v) -> collectByPotential.merge(k, v, Long::sum));

                    lastData.getSkill1()
                            .forEach((k, v) -> collectBySkill1.merge(k, v, Long::sum));

                    lastData.getSkill2()
                            .forEach((k, v) -> collectBySkill2.merge(k, v, Long::sum));

                    lastData.getSkill3()
                            .forEach((k, v) -> collectBySkill3.merge(k, v, Long::sum));

                    lastData.getModX()
                            .forEach((k, v) -> collectByModX.merge(k, v, Long::sum));

                    lastData.getModY()
                            .forEach((k, v) -> collectByModY.merge(k, v, Long::sum));
                }

                //存入dto对象进行暂存
                SurveyStatisticsChar build = SurveyStatisticsChar.builder()
                        .charId(charId)
                        .own(own)
                        .elite(collectByElite)
                        .rarity(rarity)
                        .skill1(collectBySkill1)
                        .skill2(collectBySkill2)
                        .skill3(collectBySkill3)
                        .potential(collectByPotential)
                        .modX(collectByModX)
                        .modY(collectByModY)
                        .build();
                hashMap.put(charId, build);
            });
        }

        //将dto对象转为数据库对象
        hashMap.forEach((k, v) -> {

            SurveyStatisticsOperator build = SurveyStatisticsOperator.builder()
                    .charId(v.getCharId())
                    .rarity(v.getRarity())
                    .own(v.getOwn())
                    .elite(JsonMapper.toJSONString(v.getElite()))
                    .skill1(JsonMapper.toJSONString(v.getSkill1()))
                    .skill2(JsonMapper.toJSONString(v.getSkill2()))
                    .skill3(JsonMapper.toJSONString(v.getSkill3()))
                    .modX(JsonMapper.toJSONString(v.getModX()))
                    .modY(JsonMapper.toJSONString(v.getModY()))
                    .potential(JsonMapper.toJSONString(v.getPotential()))
                    .build();

            statisticsOperatorList.add(build);
        });

        surveyStatisticsOperatorMapper.insertBatch(statisticsOperatorList);

    }




    /**
     * 干员信息统计
     *
     * @return 成功消息
     */
    public HashMap<Object, Object> getCharStatisticsResult() {
        List<SurveyStatisticsOperator> statisticsOperatorList = surveyStatisticsOperatorMapper.selectList(null);

        HashMap<Object, Object> hashMap = new HashMap<>();

        Object survey = redisTemplate.opsForHash().get("Survey", "UserCount.Operator");
        String updateTime = String.valueOf(redisTemplate.opsForHash().get("Survey", "UpdateTime.Operator"));

        double userCount = Double.parseDouble(survey + ".0");
        List<OperatorStatisticsResult> operatorStatisticsResultList = new ArrayList<>();
        statisticsOperatorList.forEach(item -> {
            OperatorStatisticsResult build = OperatorStatisticsResult.builder()
                    .charId(item.getCharId())
                    .rarity(item.getRarity())
                    .own((double) item.getOwn() / userCount)
                    .elite(splitCalculation(item.getElite(), item.getOwn()))
                    .skill1(splitCalculation(item.getSkill1(), item.getOwn()))
                    .skill2(splitCalculation(item.getSkill2(), item.getOwn()))
                    .skill3(splitCalculation(item.getSkill3(), item.getOwn()))
                    .modX(splitCalculation(item.getModX(), item.getOwn()))
                    .modY(splitCalculation(item.getModY(),item.getOwn()))
                    .build();

            operatorStatisticsResultList.add(build);
        });


        hashMap.put("userCount", userCount);
        hashMap.put("result", operatorStatisticsResultList);
        hashMap.put("updateTime", updateTime);

        return hashMap;
    }

    /**
     * 计算具体结果
     *
     * @param result     旧结果
     * @param sampleSize 样本
     * @return 计算结果
     */
    public HashMap<String, Double> splitCalculation(String result, Integer sampleSize) {
        HashMap<String, Double> hashMap = new HashMap<>();
        JsonNode jsonNode = JsonMapper.parseJSONObject(result);
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

        while (fields.hasNext()) {
            String key = fields.next().getKey();

            int sum = jsonNode.get(key).intValue();

            hashMap.put("rank" + key, (double) sum / sampleSize);
        }

        return hashMap;
    }




    @Scheduled(cron = "0 0 0/1 * * ? ")
    public void saveOperatorStatisticsData(){
        List<SurveyStatisticsOperator> surveyStatisticsOperators = surveyStatisticsOperatorMapper.selectList(null);
        String data = JsonMapper.toJSONString(surveyStatisticsOperators);
        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHH = new SimpleDateFormat("yyyy-MM-dd HH").format(new Date()); // 设置日期格式
        ossService.upload(data, "backup/survey/operator/statistics" + yyyyMMdd + "/operator " + yyyyMMddHH + ".json");
    }




}
