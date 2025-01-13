package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.LogUtils;
import com.lhs.entity.po.survey.*;
import com.lhs.entity.po.user.AkPlayerBindInfo;
import com.lhs.mapper.survey.OperatorDataMapper;
import com.lhs.mapper.survey.OperatorStatisticsMapper;
import com.lhs.mapper.survey.service.OperatorStatisticsMapperService;
import com.lhs.mapper.user.AkPlayerBindInfoMapper;
import com.lhs.service.util.OSSService;
import com.lhs.entity.vo.survey.OperatorStatisticsResultVO;
import com.lhs.entity.dto.survey.OperatorStatisticsDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OperatorStatisticsService {


    private final OperatorStatisticsMapper operatorStatisticsMapper;

    private final OperatorStatisticsMapperService operatorStatisticsMapperService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final OSSService ossService;
    private final OperatorDataMapper operatorDataMapper;

    private final AkPlayerBindInfoMapper akPlayerBindInfoMapper;


    public OperatorStatisticsService(OperatorStatisticsMapper operatorStatisticsMapper, OperatorStatisticsMapperService operatorStatisticsMapperService,
                                     RedisTemplate<String, Object> redisTemplate,
                                     OSSService ossService,
                                     OperatorDataMapper operatorDataMapper,
                                     AkPlayerBindInfoMapper akPlayerBindInfoMapper) {
        this.operatorStatisticsMapper = operatorStatisticsMapper;
        this.operatorStatisticsMapperService = operatorStatisticsMapperService;
        this.redisTemplate = redisTemplate;
        this.ossService = ossService;
        this.operatorDataMapper = operatorDataMapper;
        this.akPlayerBindInfoMapper = akPlayerBindInfoMapper;
    }


    public void statisticsOperatorData(){
        LambdaQueryWrapper<AkPlayerBindInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AkPlayerBindInfo::getDeleteFlag,false);
        List<AkPlayerBindInfo> akPlayerBindInfoList = akPlayerBindInfoMapper.selectList(null);
        //清空统计表
        operatorStatisticsMapper.truncate();
        int userCount = 0;
        //临时结果
        HashMap<String, OperatorStatisticsDTO> tmpResult = new HashMap<>();
        //每轮统计的数量
        List<AkPlayerBindInfo> tmpAkPlayerBindInfoList = new ArrayList<>();

        for (AkPlayerBindInfo akPlayerBindInfo : akPlayerBindInfoList) {
            tmpAkPlayerBindInfoList.add(akPlayerBindInfo);
            if (tmpAkPlayerBindInfoList.size() > 300) {
                Integer i = operatorStatisticsByIds(tmpAkPlayerBindInfoList, tmpResult);
                userCount+=i;
                tmpAkPlayerBindInfoList.clear();
            }
        }

        if (!tmpAkPlayerBindInfoList.isEmpty()) {
            Integer i = operatorStatisticsByIds(tmpAkPlayerBindInfoList, tmpResult);
            userCount+=i;
        }


        List<OperatorStatistics> statisticsOperatorList = new ArrayList<>();  //最终结果

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
                    .modA(JsonMapper.toJSONString(v.getModA()))
                    .potential(JsonMapper.toJSONString(v.getPotential()))
                    .build();
            statisticsOperatorList.add(build);
        });

        LogUtils.info("本次统计人数为：" + userCount + "人");
        String updateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        redisTemplate.opsForHash().put("Survey", "UpdateTime.Operator", updateTime);
        redisTemplate.opsForHash().put("Survey", "UserCount.Operator", userCount);

        operatorStatisticsMapperService.saveBatch(statisticsOperatorList);
        redisTemplate.expire("Survey:OperatorStatistics",10, TimeUnit.SECONDS);


    }

    private Integer operatorStatisticsByIds(List<AkPlayerBindInfo> akPlayerBindInfoList, HashMap<String, OperatorStatisticsDTO> tmpResult) {


        //获取传入的绑定信息akUid的去重后的set
        Set<String> akUidSet = akPlayerBindInfoList.stream().map(AkPlayerBindInfo::getAkUid).collect(Collectors.toSet());

        LambdaQueryWrapper<OperatorData> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.in(OperatorData::getAkUid, akUidSet);
        List<OperatorData> operatorDataList = operatorDataMapper.selectList(queryWrapper);

        LogUtils.info("本次统计数量：" + operatorDataList.size());

        //根据干员id分组
        Map<String, List<OperatorData>> collectByCharId = operatorDataList.stream()
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
            Map<Integer, Long> collectByModA = new HashMap<>();

            for (OperatorData surveyOperatorData : list) {
                collectByElite.merge(surveyOperatorData.getElite(), 1L, Long::sum);
                collectByPotential.merge(surveyOperatorData.getPotential(), 1L, Long::sum);
                collectBySkill1.merge(surveyOperatorData.getSkill1(), 1L, Long::sum);
                collectBySkill2.merge(surveyOperatorData.getSkill2(), 1L, Long::sum);
                collectBySkill3.merge(surveyOperatorData.getSkill3(), 1L, Long::sum);
                collectByModX.merge(surveyOperatorData.getModX(), 1L, Long::sum);
                collectByModY.merge(surveyOperatorData.getModY(), 1L, Long::sum);
                collectByModD.merge(surveyOperatorData.getModD(), 1L, Long::sum);
                collectByModA.merge(surveyOperatorData.getModA(), 1L, Long::sum);
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
                mergeLastData(lastData.getModA(), collectByModA);
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
                    .modA(collectByModA)
                    .build();
            tmpResult.put(charId, build);
        });

        return collectByCharId.get("char_002_amiya").size();
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
        List<OperatorStatistics> statisticsOperatorList = operatorStatisticsMapper.selectList(null);

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
                    .modA(splitCalculation(item.getModA(), item.getOwn(), "modA"))
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
     * @param result  旧结果
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
        List<OperatorStatistics> operatorStatistics = operatorStatisticsMapper.selectList(null);
        String data = JsonMapper.toJSONString(operatorStatistics);
        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHH = new SimpleDateFormat("yyyy-MM-dd HH").format(new Date()); // 设置日期格式
        ossService.upload(data, "backup/survey/operator/statistics" + yyyyMMdd + "/operator " + yyyyMMddHH + ".json");
    }


}
