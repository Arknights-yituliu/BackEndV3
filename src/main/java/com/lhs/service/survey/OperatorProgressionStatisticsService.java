package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.enums.RecordType;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.LogUtils;
import com.lhs.entity.po.survey.*;
import com.lhs.entity.po.user.AkPlayerBindInfo;
import com.lhs.mapper.survey.OperatorDataMapper;
import com.lhs.mapper.survey.OperatorProgressionStatisticsMapper;
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
public class OperatorProgressionStatisticsService {


    private final OperatorProgressionStatisticsMapper operatorProgressionStatisticsMapper;


    private final RedisTemplate<String, Object> redisTemplate;

    private final OSSService ossService;
    private final OperatorDataMapper operatorDataMapper;

    private final AkPlayerBindInfoMapper akPlayerBindInfoMapper;
    private final IdGenerator idGenerator;


    public OperatorProgressionStatisticsService(OperatorProgressionStatisticsMapper operatorProgressionStatisticsMapper,
                                                RedisTemplate<String, Object> redisTemplate,
                                                OSSService ossService,
                                                OperatorDataMapper operatorDataMapper,
                                                AkPlayerBindInfoMapper akPlayerBindInfoMapper) {
        this.operatorProgressionStatisticsMapper = operatorProgressionStatisticsMapper;
        this.redisTemplate = redisTemplate;
        this.ossService = ossService;
        this.operatorDataMapper = operatorDataMapper;
        this.idGenerator = new IdGenerator(1L);
        this.akPlayerBindInfoMapper = akPlayerBindInfoMapper;
    }


    public void statisticsOperatorProgressionData() {

        operatorProgressionStatisticsMapper.expireOldData(RecordType.EXPIRE.getCode(), RecordType.DISPLAY.getCode());

        LambdaQueryWrapper<AkPlayerBindInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        Long timeStamp = new Date().getTime()-60*60*24*120*1000L;
        lambdaQueryWrapper.eq(AkPlayerBindInfo::getDeleteFlag, false).ge(AkPlayerBindInfo::getUpdateTime,timeStamp);
        List<AkPlayerBindInfo> akPlayerBindInfoList = akPlayerBindInfoMapper.selectList(lambdaQueryWrapper);

        int userCount = 0;
        //临时结果
        HashMap<String, OperatorStatisticsDTO> tmpResult = new HashMap<>();
        //每轮统计的数量
        List<AkPlayerBindInfo> tmpAkPlayerBindInfoList = new ArrayList<>();

        for (AkPlayerBindInfo akPlayerBindInfo : akPlayerBindInfoList) {
            tmpAkPlayerBindInfoList.add(akPlayerBindInfo);
            if (tmpAkPlayerBindInfoList.size() > 300) {
                Integer i = operatorStatisticsByIds(tmpAkPlayerBindInfoList, tmpResult);
                userCount += i;
                tmpAkPlayerBindInfoList.clear();
            }
        }

        if (!tmpAkPlayerBindInfoList.isEmpty()) {
            Integer i = operatorStatisticsByIds(tmpAkPlayerBindInfoList, tmpResult);
            userCount += i;
        }


        List<OperatorProgressionStatistics> progressionStatisticsList = new ArrayList<>();  //最终结果

        Date date = new Date();
        tmpResult.forEach((k, v) -> {
            OperatorProgressionStatistics progression = new OperatorProgressionStatistics();
            progression.setId(idGenerator.nextId());
            progression.setCharId(v.getCharId());
            progression.setRarity(v.getRarity());
            progression.setOwn(v.getOwn());
            progression.setElite(JsonMapper.toJSONString(v.getElite()));
            progression.setSkill1(JsonMapper.toJSONString(v.getSkill1()));
            progression.setSkill2(JsonMapper.toJSONString(v.getSkill2()));
            progression.setSkill3(JsonMapper.toJSONString(v.getSkill3()));
            progression.setModX(JsonMapper.toJSONString(v.getModX()));
            progression.setModY(JsonMapper.toJSONString(v.getModY()));
            progression.setModD(JsonMapper.toJSONString(v.getModD()));

            progression.setModA(JsonMapper.toJSONString(v.getModA()));
            progression.setPotential(JsonMapper.toJSONString(v.getPotential()));
            progression.setRecordType(RecordType.DISPLAY.getCode());
            progression.setCreateTime(date);
            progressionStatisticsList.add(progression);
        });

        LogUtils.info("本次干员练度统计样本量为：" + userCount + "人");
        String updateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        redisTemplate.opsForHash().put("Survey", "UpdateTime.Operator", updateTime);
        redisTemplate.opsForHash().put("Survey", "UserCount.Operator", userCount);

        operatorProgressionStatisticsMapper.insertBatch(progressionStatisticsList);
        redisTemplate.expire("Survey:OperatorStatistics", 10, TimeUnit.SECONDS);
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


    public void archivedOperatorProgressionResult() {

        // 获取今天的开始时间和结束时间
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calendar.getTime();

        LambdaUpdateWrapper<OperatorProgressionStatistics> existQueryWrapper = new LambdaUpdateWrapper<>();
        existQueryWrapper.eq(OperatorProgressionStatistics::getRecordType, RecordType.ARCHIVED.getCode())
                .ge(OperatorProgressionStatistics::getCreateTime, startOfDay)
                .le(OperatorProgressionStatistics::getCreateTime, endOfDay);

        boolean exists = operatorProgressionStatisticsMapper.exists(existQueryWrapper);
        if (exists) {
            LogUtils.info("干员携带率统计结果今日已归档");
            return;
        }

        LambdaUpdateWrapper<OperatorProgressionStatistics> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(OperatorProgressionStatistics::getRecordType, RecordType.DISPLAY.getCode());
        List<OperatorProgressionStatistics> list = operatorProgressionStatisticsMapper.selectList(queryWrapper);
        for (OperatorProgressionStatistics item : list) {
            item.setId(idGenerator.nextId());
            item.setRecordType(RecordType.ARCHIVED.getCode());
        }

        Integer i = operatorProgressionStatisticsMapper.insertBatch(list);
        LogUtils.info("干员携带率统计结果归档成功" + i + "条");

    }

    public void deleteExpireData(){
        LambdaQueryWrapper<OperatorProgressionStatistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperatorProgressionStatistics::getRecordType,RecordType.EXPIRE.getCode());
        int delete = operatorProgressionStatisticsMapper.delete(queryWrapper);
        LogUtils.info("本次清理了"+delete+"条过期干员携带率统计数据");
    }


    /**
     * 干员信息统计
     *
     * @return 成功消息
     */
    @RedisCacheable(key = "Survey:OperatorProgressionStatistics")
    public HashMap<Object, Object> getOperatorProgressionStatisticsResult() {
        LambdaUpdateWrapper<OperatorProgressionStatistics> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(OperatorProgressionStatistics::getRecordType, RecordType.DISPLAY.getCode());
        List<OperatorProgressionStatistics> statisticsOperatorList = operatorProgressionStatisticsMapper.selectList(queryWrapper);

        HashMap<Object, Object> hashMap = new HashMap<>();

        Object userCountText = redisTemplate.opsForHash().get("Survey", "UserCount.Operator");
        String updateTime = String.valueOf(redisTemplate.opsForHash().get("Survey", "UpdateTime.Operator"));

        double userCount = Double.parseDouble(userCountText + ".0");
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





}
