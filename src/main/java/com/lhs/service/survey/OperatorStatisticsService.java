package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.entity.po.survey.*;
import com.lhs.mapper.survey.AkPlayerBindInfoV2Mapper;
import com.lhs.mapper.survey.OperatorDataMapper;
import com.lhs.mapper.survey.OperatorDataVoMapper;
import com.lhs.mapper.survey.OperatorSurveyStatisticsMapper;
import com.lhs.service.util.ArknightsGameDataService;
import com.lhs.service.util.OSSService;
import com.lhs.entity.vo.survey.OperatorStatisticsResultVO;
import com.lhs.entity.dto.survey.OperatorStatisticsDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OperatorStatisticsService {


    private final OperatorSurveyStatisticsMapper operatorSurveyStatisticsMapper;

    private final AkPlayerBindInfoV2Mapper akPlayerBindInfoV2Mapper;

    private final OperatorDataMapper operatorDataMapper;

    private final ArknightsGameDataService arknightsGameDataService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final OSSService ossService;


    public OperatorStatisticsService(OperatorSurveyStatisticsMapper operatorSurveyStatisticsMapper, AkPlayerBindInfoV2Mapper akPlayerBindInfoV2Mapper, OperatorDataMapper operatorDataMapper, ArknightsGameDataService arknightsGameDataService, RedisTemplate<String, Object> redisTemplate, OSSService ossService) {
        this.operatorSurveyStatisticsMapper = operatorSurveyStatisticsMapper;
        this.akPlayerBindInfoV2Mapper = akPlayerBindInfoV2Mapper;
        this.operatorDataMapper = operatorDataMapper;
        this.arknightsGameDataService = arknightsGameDataService;
        this.redisTemplate = redisTemplate;
        this.ossService = ossService;
    }


    /**
     * 干员练度调查表统计
     */
//    @Scheduled(cron = "0 10 0/2 * * ?")
    public void operatorStatistics() {
        QueryWrapper<AkPlayerBindInfoV2> playerBindInfoQueryWrapper = new QueryWrapper<>();

        playerBindInfoQueryWrapper.eq("default_flag", true)
                .ge("last_active_time", new Date(System.currentTimeMillis() - 60 * 60 * 24 * 1000 * 30L));

        List<AkPlayerBindInfoV2> akPlayerBindInfoList = akPlayerBindInfoV2Mapper.selectList(playerBindInfoQueryWrapper);


        Logger.info("本次统计人数为：" + akPlayerBindInfoList.size() + "人");

        String updateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        redisTemplate.opsForHash().put("Survey", "UpdateTime.Operator", updateTime);
        redisTemplate.opsForHash().put("Survey", "UserCount.Operator", akPlayerBindInfoList.size());

        // 计算用户id按500个用户一组可以分成多少组
        operatorSurveyStatisticsMapper.truncate(); //清空统计表
        HashMap<String, OperatorStatisticsDTO> tmpResult = new HashMap<>();  //结果暂存对象

        List<AkPlayerBindInfoV2> tmpAkPlayerBindInfoList = new ArrayList<>();

        for (AkPlayerBindInfoV2 akPlayerBindInfo : akPlayerBindInfoList) {
            tmpAkPlayerBindInfoList.add(akPlayerBindInfo);
            if (tmpAkPlayerBindInfoList.size() > 300) {
                operatorStatistics(tmpAkPlayerBindInfoList, tmpResult);
                tmpAkPlayerBindInfoList.clear();
            }
        }

        if (!tmpAkPlayerBindInfoList.isEmpty()) {
            operatorStatistics(tmpAkPlayerBindInfoList, tmpResult);
        }

        List<OperatorStatistics> statisticsOperatorList = new ArrayList<>();  //最终结果


        //将dto对象转为数据库对象
        tmpResult.forEach((k, v) -> {

            OperatorStatistics build = OperatorStatistics.builder()
                    .charId(v.getCharId())
                    .rarity(v.getRarity())
                    .own(v.getOwn())
                    .elite(JsonMapper.toJSONString(v.getElite()))
                    .skill1(JsonMapper.toJSONString(v.getSkill1()))
                    .skill2(JsonMapper.toJSONString(v.getSkill2()))
                    .skill3(JsonMapper.toJSONString(v.getSkill3()))
                    .modX(JsonMapper.toJSONString(v.getModX()))
                    .modY(JsonMapper.toJSONString(v.getModY()))
                    .modD(JsonMapper.toJSONString(v.getModD()))
                    .potential(JsonMapper.toJSONString(v.getPotential()))
                    .build();
            statisticsOperatorList.add(build);
        });

        operatorSurveyStatisticsMapper.insertBatch(statisticsOperatorList);
        redisTemplate.expire("Survey:OperatorStatistics",10, TimeUnit.SECONDS);

    }

    private void operatorStatistics(List<AkPlayerBindInfoV2> akPlayerBindInfoList, HashMap<String, OperatorStatisticsDTO> tmpResult) {

        QueryWrapper<OperatorData> queryWrapper = new QueryWrapper<>();
        Set<Long> uidSet = akPlayerBindInfoList.stream().map(AkPlayerBindInfoV2::getUid).collect(Collectors.toSet());

        queryWrapper.in("uid", uidSet);
        List<OperatorData> operatorDataListByUidSet = operatorDataMapper.selectList(queryWrapper);

        Logger.info("本次统计数量：" + operatorDataListByUidSet.size());
        List<OperatorData> filteredList = new ArrayList<>();

        int lastOperatorDataSize = 0;

        Map<Long, List<OperatorData>> collect = operatorDataListByUidSet.stream().collect(Collectors.groupingBy(OperatorData::getUid));
        for (AkPlayerBindInfoV2 akPlayerBindInfoV2 : akPlayerBindInfoList) {
            List<OperatorData> operatorDataListByUid = collect.get(akPlayerBindInfoV2.getUid());
            if (operatorDataListByUid != null) {
                operatorDataListByUid.stream()
                        .filter(o -> o.getAkUid().equals(akPlayerBindInfoV2.getAkUid()))
                        .forEach(filteredList::add);
            }
            Logger.info("该用户名下有"+(filteredList.size()-lastOperatorDataSize)+"条干员数据");
            lastOperatorDataSize = filteredList.size();
        }



        //根据干员id分组
        Map<String, List<OperatorData>> collectByCharId = filteredList.stream()
                .collect(Collectors.groupingBy(OperatorData::getCharId));

        //计算结果
        collectByCharId.forEach((charId, list) -> {
            list = list.stream()
                    .filter(OperatorData::getOwn)
                    .collect(Collectors.toList());

            int own = list.size();  //持有人数
            int rarity = list.get(0).getRarity();  //星级

            Map<Integer, Long> collectByElite = new HashMap<>();
            Map<Integer, Long> collectByPotential = new HashMap<>();
            Map<Integer, Long> collectBySkill1 = new HashMap<>();
            Map<Integer, Long> collectBySkill2 = new HashMap<>();
            Map<Integer, Long> collectBySkill3 = new HashMap<>();
            Map<Integer, Long> collectByModX = new HashMap<>();
            Map<Integer, Long> collectByModY = new HashMap<>();
            Map<Integer, Long> collectByModD = new HashMap<>();

            for (OperatorData operatorData : list) {
                collectByElite.merge(operatorData.getElite(), 1L, Long::sum);
                collectByPotential.merge(operatorData.getPotential(), 1L, Long::sum);
                collectBySkill1.merge(operatorData.getSkill1(), 1L, Long::sum);
                collectBySkill2.merge(operatorData.getSkill2(), 1L, Long::sum);
                collectBySkill3.merge(operatorData.getSkill3(), 1L, Long::sum);
                collectByModX.merge(operatorData.getModX(), 1L, Long::sum);
                collectByModY.merge(operatorData.getModY(), 1L, Long::sum);
                collectByModD.merge(operatorData.getModD(), 1L, Long::sum);
            }


            //和上一组用户id的数据合并
            if (tmpResult.get(charId) != null) {
                OperatorStatisticsDTO lastData = tmpResult.get(charId);
                own += lastData.getOwn();
                mergeLastData(lastData.getElite(), collectByElite);
                mergeLastData(lastData.getPotential(), collectByPotential);
                mergeLastData(lastData.getSkill1(), collectBySkill1);
                mergeLastData(lastData.getSkill2(), collectBySkill2);
                mergeLastData(lastData.getSkill3(), collectBySkill3);
                mergeLastData(lastData.getModX(), collectByModX);
                mergeLastData(lastData.getModY(), collectByModY);
                mergeLastData(lastData.getModD(), collectByModD);
            }

            //存入dto对象进行暂存
            OperatorStatisticsDTO build = OperatorStatisticsDTO.builder()
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
                    .modD(collectByModD)
                    .build();
            tmpResult.put(charId, build);
        });
    }

    private void mergeLastData(Map<Integer, Long> resource, Map<Integer, Long> target) {
        for (Integer key : resource.keySet()) {
            target.merge(key, resource.get(key), Long::sum);
        }
    }

    /**
     * 干员信息统计
     *
     * @return 成功消息
     */
    @RedisCacheable(key = "Survey:OperatorStatistics")
    public HashMap<Object, Object> getCharStatisticsResult() {
        List<OperatorStatistics> statisticsOperatorList = operatorSurveyStatisticsMapper.selectList(null);

        HashMap<Object, Object> hashMap = new HashMap<>();

        Object survey = redisTemplate.opsForHash().get("Survey", "UserCount.Operator");
        String updateTime = String.valueOf(redisTemplate.opsForHash().get("Survey", "UpdateTime.Operator"));


        double userCount = Double.parseDouble(survey + ".0");
        List<OperatorStatisticsResultVO> operatorStatisticsResultVOList = new ArrayList<>();
        statisticsOperatorList.forEach(item -> {
            OperatorStatisticsResultVO build = OperatorStatisticsResultVO.builder()
                    .charId(item.getCharId())
                    .rarity(item.getRarity())
                    .own((double) item.getOwn() / userCount)
                    .elite(splitCalculation(item.getElite(), item.getOwn(), "elite"))
                    .skill1(splitCalculation(item.getSkill1(), item.getOwn(), "skill1"))
                    .skill2(splitCalculation(item.getSkill2(), item.getOwn(), "skill2"))
                    .skill3(splitCalculation(item.getSkill3(), item.getOwn(), "skill3"))
                    .modX(splitCalculation(item.getModX(), item.getOwn(), "modX"))
                    .modY(splitCalculation(item.getModY(), item.getOwn(), "modY"))
                    .modD(splitCalculation(item.getModD(), item.getOwn(), "modD"))
                    .build();
            operatorStatisticsResultVOList.add(build);
        });


        hashMap.put("userCount", userCount);
        hashMap.put("result", operatorStatisticsResultVOList);
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
    public HashMap<String, Double> splitCalculation(String result, Integer sampleSize, String property) {
        HashMap<String, Double> hashMap = new HashMap<>();
        hashMap.put("rank1", 0.0);
        hashMap.put("rank2", 0.0);
        hashMap.put("rank3", 0.0);
        if (result == null) {
            return hashMap;
        }

        JsonNode jsonNode = JsonMapper.parseJSONObject(result);
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

        double count = 0.0;
        while (fields.hasNext()) {
            String key = fields.next().getKey();
            int sum = jsonNode.get(key).intValue();
            if (Integer.parseInt(key) > 0) count += ((double) sum / sampleSize);
            hashMap.put("rank" + key, (double) sum / sampleSize);
        }
        hashMap.put("count", count);


        return hashMap;
    }


    @Scheduled(cron = "0 0 0/1 * * ? ")
    public void saveOperatorStatisticsData() {
        List<OperatorStatistics> operatorStatistics = operatorSurveyStatisticsMapper.selectList(null);
        String data = JsonMapper.toJSONString(operatorStatistics);
        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHH = new SimpleDateFormat("yyyy-MM-dd HH").format(new Date()); // 设置日期格式
        ossService.upload(data, "backup/survey/operator/statistics" + yyyyMMdd + "/operator " + yyyyMMddHH + ".json");
    }


}
