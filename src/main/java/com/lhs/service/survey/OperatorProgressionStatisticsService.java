package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.enums.RecordType;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.LogUtils;
import com.lhs.entity.dto.survey.OperatorProgressionDataDTO;
import com.lhs.entity.dto.survey.OperatorProgressionStatisticalResultDTO;
import com.lhs.entity.po.survey.*;

import com.lhs.entity.po.user.AkPlayerBindInfo;
import com.lhs.entity.vo.survey.OperatorProgressionStatisticalDataVO;
import com.lhs.entity.vo.survey.OperatorProgressionStatisticalResultVO;
import com.lhs.entity.vo.survey.OperatorProgressionStatisticalResultVOV2;
import com.lhs.mapper.survey.OperatorDataMapper;
import com.lhs.mapper.survey.OperatorProgressionDataMapper;
import com.lhs.mapper.survey.OperatorProgressionStatisticalResultMapper;
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


    public OperatorProgressionStatisticsService(OperatorProgressionStatisticalResultMapper operatorProgressionStatisticalResultMapper,
                                                RedisTemplate<String, Object> redisTemplate,
                                                OSSService ossService,
                                                OperatorDataMapper operatorDataMapper,
                                                OperatorProgressionDataMapper operatorProgressionDataMapper,
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
        operatorProgressionStatisticalResult.setStatisticalResult(jsonString);
        operatorProgressionStatisticalResultMapper.insert(operatorProgressionStatisticalResult);


    }




    /**
     * 干员信息统计
     *
     * @return 成功消息
     */
    @RedisCacheable(key = "Survey:OperatorProgressionStatistics")
    public OperatorProgressionStatisticalResultVOV2 getOperatorProgressionStatisticalResultV2() {
        LambdaUpdateWrapper<OperatorProgressionStatisticalResult> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(OperatorProgressionStatisticalResult::getRecordType, RecordType.DISPLAY.getCode());
        List<OperatorProgressionStatisticalResult> list = operatorProgressionStatisticalResultMapper.selectList(queryWrapper);
        OperatorProgressionStatisticalResult operatorProgressionStatisticalResult = list.get(0);

        List<OperatorProgressionStatisticalResultDTO> progressionStatisticalResultDTOList = JsonMapper.parseJSONArray(operatorProgressionStatisticalResult.getStatisticalResult(), new TypeReference<>() {
        });

        OperatorProgressionStatisticalResultVOV2 operatorProgressionStatisticalResultVOV2 = new OperatorProgressionStatisticalResultVOV2();
        operatorProgressionStatisticalResultVOV2.setSampleSize(operatorProgressionStatisticalResult.getSampleSize());
        operatorProgressionStatisticalResultVOV2.setRecordType(RecordType.DISPLAY.getCode());
        operatorProgressionStatisticalResultVOV2.setCreateTime(operatorProgressionStatisticalResult.getCreateTime().getTime());
        operatorProgressionStatisticalResultVOV2.setResult(progressionStatisticalResultDTOList);
        return operatorProgressionStatisticalResultVOV2;
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

        LambdaUpdateWrapper<OperatorProgressionStatisticalResult> existQueryWrapper = new LambdaUpdateWrapper<>();
        existQueryWrapper.eq(OperatorProgressionStatisticalResult::getRecordType, RecordType.ARCHIVED.getCode())
                .ge(OperatorProgressionStatisticalResult::getCreateTime, startOfDay)
                .le(OperatorProgressionStatisticalResult::getCreateTime, endOfDay);

        boolean exists = operatorProgressionStatisticalResultMapper.exists(existQueryWrapper);
        if (exists) {
            LogUtils.info("干员携带率统计结果今日已归档");
            return;
        }

        LambdaUpdateWrapper<OperatorProgressionStatisticalResult> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(OperatorProgressionStatisticalResult::getRecordType, RecordType.DISPLAY.getCode())
                .orderByDesc(OperatorProgressionStatisticalResult::getCreateTime);
        List<OperatorProgressionStatisticalResult> list = operatorProgressionStatisticalResultMapper.selectList(queryWrapper);
        OperatorProgressionStatisticalResult operatorProgressionStatisticalResult = list.get(0);
        operatorProgressionStatisticalResult.setRecordType(RecordType.ARCHIVED.getCode());
        operatorProgressionStatisticalResult.setId(idGenerator.nextId());

        int i = operatorProgressionStatisticalResultMapper.insert(operatorProgressionStatisticalResult);
        LogUtils.info("干员携带率统计结果归档成功" + i + "条");

    }

    public void deleteExpireData() {
        LambdaQueryWrapper<OperatorProgressionStatisticalResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperatorProgressionStatisticalResult::getRecordType, RecordType.EXPIRE.getCode());
        int delete = operatorProgressionStatisticalResultMapper.delete(queryWrapper);
        LogUtils.info("本次清理了" + delete + "条过期干员携带率统计数据");
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




}
