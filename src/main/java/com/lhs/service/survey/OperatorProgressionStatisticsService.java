package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.enums.RecordType;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.dto.survey.OperatorProgressionDataDTO;
import com.lhs.entity.dto.survey.OperatorProgressionStatisticalResultDTO;
import com.lhs.entity.po.survey.*;

import com.lhs.entity.po.user.AkPlayerBindInfo;
import com.lhs.entity.vo.survey.OperatorProgressionStatisticalResultVO;
import com.lhs.mapper.survey.OperatorDataMapper;
import com.lhs.mapper.survey.OperatorProgressionDataMapper;
import com.lhs.mapper.survey.OperatorProgressionStatisticalResultMapper;
import com.lhs.mapper.survey.OperatorProgressionStatisticsMapper;
import com.lhs.mapper.user.AkPlayerBindInfoMapper;
import com.lhs.service.util.OSSService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OperatorProgressionStatisticsService {


    private final OperatorProgressionStatisticalResultMapper operatorProgressionStatisticalResultMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final OSSService ossService;

    private final OperatorDataMapper operatorDataMapper;

    private final OperatorProgressionDataMapper operatorProgressionDataMapper;

    private final AkPlayerBindInfoMapper akPlayerBindInfoMapper;
    private final IdGenerator idGenerator;


    public OperatorProgressionStatisticsService(OperatorProgressionStatisticsMapper operatorProgressionStatisticsMapper,
                                                OperatorProgressionStatisticalResultMapper operatorProgressionStatisticalResultMapper,
                                                RedisTemplate<String, Object> redisTemplate,
                                                OSSService ossService,
                                                OperatorDataMapper operatorDataMapper, OperatorProgressionDataMapper operatorProgressionDataMapper,
                                                AkPlayerBindInfoMapper akPlayerBindInfoMapper) {

        this.operatorProgressionStatisticalResultMapper = operatorProgressionStatisticalResultMapper;
        this.redisTemplate = redisTemplate;
        this.ossService = ossService;
        this.operatorDataMapper = operatorDataMapper;
        this.operatorProgressionDataMapper = operatorProgressionDataMapper;

        this.idGenerator = new IdGenerator(1L);
        this.akPlayerBindInfoMapper = akPlayerBindInfoMapper;
    }


    public void statisticsOperatorProgressionDataV2() {
        Date date = new Date();
        LambdaUpdateWrapper<OperatorProgressionStatisticalResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(OperatorProgressionStatisticalResult::getRecordType, RecordType.EXPIRE.getCode())
                .eq(OperatorProgressionStatisticalResult::getRecordType, RecordType.DISPLAY.getCode())
                .lt(OperatorProgressionStatisticalResult::getCreateTime, date);
        operatorProgressionStatisticalResultMapper.update(null, updateWrapper);

        //查询数据库中最近两个月的干员练度数据
        long timeStamp = new Date().getTime() - 60 * 60 * 24 * 60 * 1000L;
        LambdaQueryWrapper<OperatorProgressionData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(OperatorProgressionData::getCreateTime, new Date(timeStamp));
        List<OperatorProgressionData> operatorProgressionDataList = operatorProgressionDataMapper.selectList(queryWrapper);
        //干员练度统计数据统计结果
        Map<String, OperatorProgressionStatisticalResultDTO> collect = new HashMap<>();


        //循环统计干员练度
        for (OperatorProgressionData operatorProgressionData : operatorProgressionDataList) {
            String operatorProgression = operatorProgressionData.getOperatorProgression();
            //将json文本转为集合
            List<OperatorProgressionDataDTO> dataDTOList = JsonMapper.parseJSONArray(operatorProgression, new TypeReference<>() {
            });

            //循环每个账号的干员练度
            for (OperatorProgressionDataDTO progressionDataDTO : dataDTOList) {
                //先判断统计结果是否有这个干员
                OperatorProgressionStatisticalResultDTO operatorProgressionStatisticalResultDTO = collect.get(progressionDataDTO.getCharId());
                //没有的话先创建一个对象
                if (operatorProgressionStatisticalResultDTO == null) {
                    operatorProgressionStatisticalResultDTO = new OperatorProgressionStatisticalResultDTO();
                }
                if (progressionDataDTO.getOwn()) {
                    operatorProgressionStatisticalResultDTO.increaseOwn();
                }
                operatorProgressionStatisticalResultDTO.setCharId(progressionDataDTO.getCharId());
                operatorProgressionStatisticalResultDTO.mergeElite(progressionDataDTO.getElite());
                operatorProgressionStatisticalResultDTO.mergeSkill1(progressionDataDTO.getSkill1());
                operatorProgressionStatisticalResultDTO.mergeSkill2(progressionDataDTO.getSkill2());
                operatorProgressionStatisticalResultDTO.mergeSkill3(progressionDataDTO.getSkill3());
                operatorProgressionStatisticalResultDTO.mergeModA(progressionDataDTO.getModA());
                operatorProgressionStatisticalResultDTO.mergeModX(progressionDataDTO.getModX());
                operatorProgressionStatisticalResultDTO.mergeModY(progressionDataDTO.getModY());
                operatorProgressionStatisticalResultDTO.mergeModD(progressionDataDTO.getModD());
                collect.put(progressionDataDTO.getCharId(), operatorProgressionStatisticalResultDTO);
            }
        }

        List<OperatorProgressionStatisticalResultDTO> list = collect.values().stream().toList();
        OperatorProgressionStatisticalResult operatorProgressionStatisticalResult = new OperatorProgressionStatisticalResult();
        operatorProgressionStatisticalResult.setSampleSize(operatorProgressionDataList.size());
        operatorProgressionStatisticalResult.setId(idGenerator.nextId());
        operatorProgressionStatisticalResult.setRecordType(RecordType.DISPLAY.getCode());
        operatorProgressionStatisticalResult.setCreateTime(new Date());
        String jsonString = JsonMapper.toJSONString(list);
        System.out.println(jsonString.length());
        operatorProgressionStatisticalResult.setStatisticalResult(jsonString);

        operatorProgressionStatisticalResultMapper.insert(operatorProgressionStatisticalResult);


    }

//    public void statisticsOperatorProgressionData() {
//
//        LambdaQueryWrapper<AkPlayerBindInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
//        Long timeStamp = new Date().getTime()-60*60*24*120*1000L;
//        lambdaQueryWrapper.eq(AkPlayerBindInfo::getDeleteFlag, false).ge(AkPlayerBindInfo::getUpdateTime,timeStamp);
//        List<AkPlayerBindInfo> akPlayerBindInfoList = akPlayerBindInfoMapper.selectList(lambdaQueryWrapper);
//
//        int userCount = 0;
//        //临时结果
//        HashMap<String, OperatorStatisticsDTO> tmpResult = new HashMap<>();
//        //每轮统计的数量
//        List<AkPlayerBindInfo> tmpAkPlayerBindInfoList = new ArrayList<>();
//
//        for (AkPlayerBindInfo akPlayerBindInfo : akPlayerBindInfoList) {
//            tmpAkPlayerBindInfoList.add(akPlayerBindInfo);
//            if (tmpAkPlayerBindInfoList.size() > 300) {
//                Integer i = operatorStatisticsByIds(tmpAkPlayerBindInfoList, tmpResult);
//                userCount += i;
//                tmpAkPlayerBindInfoList.clear();
//            }
//        }
//
//        if (!tmpAkPlayerBindInfoList.isEmpty()) {
//            Integer i = operatorStatisticsByIds(tmpAkPlayerBindInfoList, tmpResult);
//            userCount += i;
//        }
//
//
//        List<OperatorProgressionStatistics> progressionStatisticsList = new ArrayList<>();  //最终结果
//
//        Date date = new Date();
//        tmpResult.forEach((k, v) -> {
//            OperatorProgressionStatistics progression = new OperatorProgressionStatistics();
//            progression.setId(idGenerator.nextId());
//            progression.setCharId(v.getCharId());
//            progression.setRarity(v.getRarity());
//            progression.setOwn(v.getOwn());
//            progression.setElite(JsonMapper.toJSONString(v.getElite()));
//            progression.setSkill1(JsonMapper.toJSONString(v.getSkill1()));
//            progression.setSkill2(JsonMapper.toJSONString(v.getSkill2()));
//            progression.setSkill3(JsonMapper.toJSONString(v.getSkill3()));
//            progression.setModX(JsonMapper.toJSONString(v.getModX()));
//            progression.setModY(JsonMapper.toJSONString(v.getModY()));
//            progression.setModD(JsonMapper.toJSONString(v.getModD()));
//            progression.setModA(JsonMapper.toJSONString(v.getModA()));
//            progression.setPotential(JsonMapper.toJSONString(v.getPotential()));
//            progression.setRecordType(RecordType.DISPLAY.getCode());
//            progression.setCreateTime(date);
//            progressionStatisticsList.add(progression);
//        });
//
//        LogUtils.info("本次干员练度统计样本量为：" + userCount + "人");
//        String updateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
//        redisTemplate.opsForHash().put("Survey", "UpdateTime.Operator", updateTime);
//        redisTemplate.opsForHash().put("Survey", "UserCount.Operator", userCount);
//
//        operatorProgressionStatisticsMapper.insertBatch(progressionStatisticsList);
//        redisTemplate.expire("Survey:OperatorStatistics", 10, TimeUnit.SECONDS);
//    }
//
//    private Integer operatorStatisticsByIds(List<AkPlayerBindInfo> akPlayerBindInfoList, HashMap<String, OperatorStatisticsDTO> tmpResult) {
//
//
//        //获取传入的绑定信息akUid的去重后的set
//        Set<String> akUidSet = akPlayerBindInfoList.stream().map(AkPlayerBindInfo::getAkUid).collect(Collectors.toSet());
//
//        LambdaQueryWrapper<OperatorData> queryWrapper = new LambdaQueryWrapper<>();
//
//        queryWrapper.in(OperatorData::getAkUid, akUidSet);
//
//        List<OperatorData> operatorDataList = operatorDataMapper.selectList(queryWrapper);
//
//        LogUtils.info("本次统计数量：" + operatorDataList.size());
//
//        //根据干员id分组
//        Map<String, List<OperatorData>> collectByCharId = operatorDataList.stream()
//                .collect(Collectors.groupingBy(OperatorData::getCharId));
//
//        //计算结果
//        collectByCharId.forEach((charId, list) -> {
//            list = list.stream()
//                    .filter(OperatorData::getOwn)
//                    .collect(Collectors.toList());
//
//            int own = list.size();  //持有人数
//            int rarity = list.get(0).getRarity();  //星级
//
//            Map<Integer, Long> collectByElite = new HashMap<>();
//            Map<Integer, Long> collectByPotential = new HashMap<>();
//            Map<Integer, Long> collectBySkill1 = new HashMap<>();
//            Map<Integer, Long> collectBySkill2 = new HashMap<>();
//            Map<Integer, Long> collectBySkill3 = new HashMap<>();
//            Map<Integer, Long> collectByModX = new HashMap<>();
//            Map<Integer, Long> collectByModY = new HashMap<>();
//            Map<Integer, Long> collectByModD = new HashMap<>();
//            Map<Integer, Long> collectByModA = new HashMap<>();
//
//            for (OperatorData surveyOperatorData : list) {
//                collectByElite.merge(surveyOperatorData.getElite(), 1L, Long::sum);
//                collectByPotential.merge(surveyOperatorData.getPotential(), 1L, Long::sum);
//                collectBySkill1.merge(surveyOperatorData.getSkill1(), 1L, Long::sum);
//                collectBySkill2.merge(surveyOperatorData.getSkill2(), 1L, Long::sum);
//                collectBySkill3.merge(surveyOperatorData.getSkill3(), 1L, Long::sum);
//                collectByModX.merge(surveyOperatorData.getModX(), 1L, Long::sum);
//                collectByModY.merge(surveyOperatorData.getModY(), 1L, Long::sum);
//                collectByModD.merge(surveyOperatorData.getModD(), 1L, Long::sum);
//                collectByModA.merge(surveyOperatorData.getModA(), 1L, Long::sum);
//            }
//
//
//            //和上一组用户id的数据合并
//            if (tmpResult.get(charId) != null) {
//                OperatorStatisticsDTO lastData = tmpResult.get(charId);
//                own += lastData.getOwn();
//                mergeLastData(lastData.getElite(), collectByElite);
//                mergeLastData(lastData.getPotential(), collectByPotential);
//                mergeLastData(lastData.getSkill1(), collectBySkill1);
//                mergeLastData(lastData.getSkill2(), collectBySkill2);
//                mergeLastData(lastData.getSkill3(), collectBySkill3);
//                mergeLastData(lastData.getModX(), collectByModX);
//                mergeLastData(lastData.getModY(), collectByModY);
//                mergeLastData(lastData.getModD(), collectByModD);
//                mergeLastData(lastData.getModA(), collectByModA);
//            }
//
//            //存入dto对象进行暂存
//            OperatorStatisticsDTO build = OperatorStatisticsDTO.builder()
//                    .charId(charId)
//                    .own(own)
//                    .elite(collectByElite)
//                    .rarity(rarity)
//                    .skill1(collectBySkill1)
//                    .skill2(collectBySkill2)
//                    .skill3(collectBySkill3)
//                    .potential(collectByPotential)
//                    .modX(collectByModX)
//                    .modY(collectByModY)
//                    .modD(collectByModD)
//                    .modA(collectByModA)
//                    .build();
//
//            tmpResult.put(charId, build);
//        });
//
//        return collectByCharId.get("char_002_amiya").size();
//    }
//
//    private void mergeLastData(Map<Integer, Long> resource, Map<Integer, Long> target) {
//        for (Integer key : resource.keySet()) {
//            target.merge(key, resource.get(key), Long::sum);
//        }
//    }
//
//
//    public void archivedOperatorProgressionResult() {
//
//        // 获取今天的开始时间和结束时间
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(Calendar.HOUR_OF_DAY, 0);
//        calendar.set(Calendar.MINUTE, 0);
//        calendar.set(Calendar.SECOND, 0);
//        calendar.set(Calendar.MILLISECOND, 0);
//        Date startOfDay = calendar.getTime();
//
//        calendar.set(Calendar.HOUR_OF_DAY, 23);
//        calendar.set(Calendar.MINUTE, 59);
//        calendar.set(Calendar.SECOND, 59);
//        calendar.set(Calendar.MILLISECOND, 999);
//        Date endOfDay = calendar.getTime();
//
//        LambdaUpdateWrapper<OperatorProgressionStatistics> existQueryWrapper = new LambdaUpdateWrapper<>();
//        existQueryWrapper.eq(OperatorProgressionStatistics::getRecordType, RecordType.ARCHIVED.getCode())
//                .ge(OperatorProgressionStatistics::getCreateTime, startOfDay)
//                .le(OperatorProgressionStatistics::getCreateTime, endOfDay);
//
//        boolean exists = operatorProgressionStatisticsMapper.exists(existQueryWrapper);
//        if (exists) {
//            LogUtils.info("干员携带率统计结果今日已归档");
//            return;
//        }
//
//        LambdaUpdateWrapper<OperatorProgressionStatistics> queryWrapper = new LambdaUpdateWrapper<>();
//        queryWrapper.eq(OperatorProgressionStatistics::getRecordType, RecordType.DISPLAY.getCode());
//        List<OperatorProgressionStatistics> list = operatorProgressionStatisticsMapper.selectList(queryWrapper);
//        for (OperatorProgressionStatistics item : list) {
//            item.setId(idGenerator.nextId());
//            item.setRecordType(RecordType.ARCHIVED.getCode());
//        }
//
//        Integer i = operatorProgressionStatisticsMapper.insertBatch(list);
//        LogUtils.info("干员携带率统计结果归档成功" + i + "条");
//
//    }
//
//    public void deleteExpireData(){
//        LambdaQueryWrapper<OperatorProgressionStatistics> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(OperatorProgressionStatistics::getRecordType,RecordType.EXPIRE.getCode());
//        int delete = operatorProgressionStatisticsMapper.delete(queryWrapper);
//        LogUtils.info("本次清理了"+delete+"条过期干员携带率统计数据");
//    }


    /**
     * 干员信息统计
     *
     * @return 成功消息
     */
    @RedisCacheable(key = "Survey:OperatorProgressionStatistics")
    public OperatorProgressionStatisticalResultVO getOperatorProgressionStatisticalResult() {
        LambdaUpdateWrapper<OperatorProgressionStatisticalResult> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(OperatorProgressionStatisticalResult::getRecordType, RecordType.DISPLAY.getCode());
        List<OperatorProgressionStatisticalResult> list = operatorProgressionStatisticalResultMapper.selectList(queryWrapper);
        OperatorProgressionStatisticalResult operatorProgressionStatisticalResult = list.get(0);

        List<OperatorProgressionStatisticalResultDTO> progressionStatisticalResultDTOList = JsonMapper.parseJSONArray(operatorProgressionStatisticalResult.getStatisticalResult(), new TypeReference<>() {
        });

        OperatorProgressionStatisticalResultVO operatorProgressionStatisticalResultVO = new OperatorProgressionStatisticalResultVO();
        operatorProgressionStatisticalResultVO.setSampleSize(operatorProgressionStatisticalResult.getSampleSize());
        operatorProgressionStatisticalResultVO.setRecordType(RecordType.DISPLAY.getCode());
        operatorProgressionStatisticalResultVO.setCreateTime(operatorProgressionStatisticalResult.getCreateTime().getTime());
        operatorProgressionStatisticalResultVO.setResult(progressionStatisticalResultDTOList);
        return operatorProgressionStatisticalResultVO;
    }

    /**
     * 干员信息统计
     *
     * @return 成功消息
     */
    @RedisCacheable(key = "Survey:OperatorProgressionStatistics")
    public OperatorProgressionStatisticalResultVO getOperatorProgressionStatisticalResultV2() {
        LambdaUpdateWrapper<OperatorProgressionStatisticalResult> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(OperatorProgressionStatisticalResult::getRecordType, RecordType.DISPLAY.getCode());
        List<OperatorProgressionStatisticalResult> list = operatorProgressionStatisticalResultMapper.selectList(queryWrapper);
        OperatorProgressionStatisticalResult operatorProgressionStatisticalResult = list.get(0);

        List<OperatorProgressionStatisticalResultDTO> progressionStatisticalResultDTOList = JsonMapper.parseJSONArray(operatorProgressionStatisticalResult.getStatisticalResult(), new TypeReference<>() {
        });

        OperatorProgressionStatisticalResultVO operatorProgressionStatisticalResultVO = new OperatorProgressionStatisticalResultVO();
        operatorProgressionStatisticalResultVO.setSampleSize(operatorProgressionStatisticalResult.getSampleSize());
        operatorProgressionStatisticalResultVO.setRecordType(RecordType.DISPLAY.getCode());
        operatorProgressionStatisticalResultVO.setCreateTime(operatorProgressionStatisticalResult.getCreateTime().getTime());
        operatorProgressionStatisticalResultVO.setResult(progressionStatisticalResultDTOList);
        return operatorProgressionStatisticalResultVO;
    }


    public void move() {
        List<AkPlayerBindInfo> akPlayerBindInfos = akPlayerBindInfoMapper.selectList(null);
        for (AkPlayerBindInfo akPlayerBindInfo : akPlayerBindInfos) {
            String akUid = akPlayerBindInfo.getAkUid();
            LambdaQueryWrapper<OperatorData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(OperatorData::getAkUid, akUid);
            List<OperatorData> operatorDataList = operatorDataMapper.selectList(queryWrapper);
            if (operatorDataList.isEmpty()) {
                continue;

            }
            List<OperatorProgressionDataDTO> dataDTOList = new ArrayList<>();
            for (OperatorData operatorData : operatorDataList) {
                OperatorProgressionDataDTO operatorProgressionDataDTO = new OperatorProgressionDataDTO();
                operatorProgressionDataDTO.setCharId(operatorData.getCharId());
                operatorProgressionDataDTO.setOwn(operatorData.getOwn());
                operatorProgressionDataDTO.setLevel(operatorData.getLevel());
                operatorProgressionDataDTO.setElite(operatorData.getElite());
                operatorProgressionDataDTO.setPotential(operatorData.getPotential());
                operatorProgressionDataDTO.setRarity(operatorData.getRarity());
                operatorProgressionDataDTO.setMainSkill(operatorData.getMainSkill());
                operatorProgressionDataDTO.setSkill1(operatorData.getSkill1());
                operatorProgressionDataDTO.setSkill2(operatorData.getSkill2());
                operatorProgressionDataDTO.setSkill3(operatorData.getSkill3());
                operatorProgressionDataDTO.setModX(operatorData.getModX());
                operatorProgressionDataDTO.setModY(operatorData.getModY());
                operatorProgressionDataDTO.setModD(operatorData.getModD());
                operatorProgressionDataDTO.setModA(operatorData.getModA());
                dataDTOList.add(operatorProgressionDataDTO);
            }


            OperatorProgressionData operatorProgressionData = new OperatorProgressionData();
            operatorProgressionData.setAkUid(akUid);
            operatorProgressionData.setCreateTime(new Date(akPlayerBindInfo.getUpdateTime()));
            operatorProgressionData.setOperatorProgression(JsonMapper.toJSONString(dataDTOList));
            operatorProgressionDataMapper.insert(operatorProgressionData);
        }
    }

    /**
     * 计算具体结果
     *
     * @param result     旧结果
     * @param sampleSize 样本
     * @return 计算结果
     */
    public Map<String, Double> splitCalculation(String result, Integer sampleSize) {
        Map<String, Double> map = new HashMap<>();
        map.put("rank1", 0.0);
        map.put("rank2", 0.0);
        map.put("rank3", 0.0);
        if (result == null) {
            return map;
        }

        JsonNode jsonNode = JsonMapper.parseJSONObject(result);
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

        double count = 0.0;
        while (fields.hasNext()) {
            String key = fields.next().getKey();
            int sum = jsonNode.get(key).intValue();
            if (Integer.parseInt(key) > 0) count += ((double) sum / sampleSize);
            map.put("rank" + key, (double) sum / sampleSize);
        }
        map.put("count", count);


        return map;
    }


}
