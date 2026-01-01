package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.enums.RecordType;
import com.lhs.common.util.HttpRequestUtil;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.LogUtils;
import com.lhs.entity.dto.survey.OperatorProgressionDataDTO;
import com.lhs.entity.dto.survey.OperatorProgressionStatisticalResultDTO;
import com.lhs.entity.po.survey.*;

import com.lhs.entity.vo.survey.OperatorProgressionStatisticalResultVOV2;
import com.lhs.mapper.survey.OperatorProgressionDataMapper;
import com.lhs.mapper.survey.OperatorProgressionStatisticalResultMapper;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OperatorProgressionStatisticsService {


    private final OperatorProgressionStatisticalResultMapper operatorProgressionStatisticalResultMapper;


    private final OperatorProgressionDataMapper operatorProgressionDataMapper;


    private final IdGenerator idGenerator;


    public OperatorProgressionStatisticsService(OperatorProgressionStatisticalResultMapper operatorProgressionStatisticalResultMapper,
                                                OperatorProgressionDataMapper operatorProgressionDataMapper) {

        this.operatorProgressionStatisticalResultMapper = operatorProgressionStatisticalResultMapper;

        this.operatorProgressionDataMapper = operatorProgressionDataMapper;

        this.idGenerator = new IdGenerator(1L);

    }


    public void statisticsOperatorProgressionDataV2() {
        HashMap<String, Date> operatorUpdateTime = new HashMap<>();
        // 定义格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        String response = HttpRequestUtil.get("https://ark.yituliu.cn/json/operator_update_time.json", new HashMap<>());
        Iterator<Map.Entry<String, JsonNode>> fields = JsonMapper.parseJSONObject(response).fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            String key = next.getKey();
            JsonNode value = next.getValue();
            String updateTime = value.get("updateTime").asText();
            LocalDateTime localDateTime = LocalDateTime.parse(updateTime, formatter);
            Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
            operatorUpdateTime.put(key, date);
        }

        HashMap<String, Integer> sampleSizeMap = new HashMap<>();


        Date date = new Date();
        LambdaUpdateWrapper<OperatorProgressionStatisticalResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(OperatorProgressionStatisticalResult::getRecordType, RecordType.EXPIRE.code())
                .eq(OperatorProgressionStatisticalResult::getRecordType, RecordType.DISPLAY.code())
                .lt(OperatorProgressionStatisticalResult::getCreateTime, date);
        operatorProgressionStatisticalResultMapper.update(null, updateWrapper);


        //干员练度统计数据统计结果
        Map<String, OperatorProgressionStatisticalResultDTO> collect = new HashMap<>();

        int count = 0;

        List<OperatorProgressionData> operatorProgressionDataList;

        for (int i = 0; i < 100; i++) {

            operatorProgressionDataList = operatorProgressionDataMapper.getOperatorProgressionData(i * 1000, 1000);

            if (operatorProgressionDataList.isEmpty()) {
                break;
            }

            count += operatorProgressionDataList.size();


            //循环统计干员练度
            for (OperatorProgressionData operatorProgressionData : operatorProgressionDataList) {
                Date createTime = operatorProgressionData.getCreateTime();
                String operatorProgression = operatorProgressionData.getOperatorProgression();
                //将json文本转为集合
                List<OperatorProgressionDataDTO> dataDTOList = JsonMapper.parseJSONArray(operatorProgression, new TypeReference<>() {
                });


                for (String charId : operatorUpdateTime.keySet()) {
                    Date updateTime = operatorUpdateTime.get(charId);
                    if (updateTime != null && createTime.compareTo(updateTime) >= 0) {
                        sampleSizeMap.merge(charId, 1, Integer::sum);
                    }
                }


                //循环每个账号的干员练度
                for (OperatorProgressionDataDTO progressionDataDTO : dataDTOList) {

                    //先判断统计结果是否有这个干员
                    OperatorProgressionStatisticalResultDTO operatorProgressionStatisticalResultDTO = collect.get(progressionDataDTO.getCharId());
                    //没有的话先创建一个对象
                    if (operatorProgressionStatisticalResultDTO == null) {
                        operatorProgressionStatisticalResultDTO = new OperatorProgressionStatisticalResultDTO();
                    }


                    operatorProgressionStatisticalResultDTO.increaseSampleSize();

                    operatorProgressionStatisticalResultDTO.increaseOwn();


                    operatorProgressionStatisticalResultDTO.setCharId(progressionDataDTO.getCharId());
                    operatorProgressionStatisticalResultDTO.mergeElite(progressionDataDTO.getElite());
                    operatorProgressionStatisticalResultDTO.mergeSkill1(progressionDataDTO.getSkill1());
                    operatorProgressionStatisticalResultDTO.mergeSkill2(progressionDataDTO.getSkill2());
                    operatorProgressionStatisticalResultDTO.mergeSkill3(progressionDataDTO.getSkill3());
                    operatorProgressionStatisticalResultDTO.mergeModA(progressionDataDTO.getModA());
                    operatorProgressionStatisticalResultDTO.mergeModB(progressionDataDTO.getModB());
                    operatorProgressionStatisticalResultDTO.mergeModX(progressionDataDTO.getModX());
                    operatorProgressionStatisticalResultDTO.mergeModY(progressionDataDTO.getModY());
                    operatorProgressionStatisticalResultDTO.mergeModD(progressionDataDTO.getModD());
                    collect.put(progressionDataDTO.getCharId(), operatorProgressionStatisticalResultDTO);
                }
            }

        }

        List<OperatorProgressionStatisticalResultDTO> list = new ArrayList<>();
        for (OperatorProgressionStatisticalResultDTO dto : collect.values()) {
            Integer i = sampleSizeMap.get(dto.getCharId());
            dto.setSampleSize(i == null ? count : i);
            list.add(dto);
        }
        OperatorProgressionStatisticalResult operatorProgressionStatisticalResult = new OperatorProgressionStatisticalResult();
        operatorProgressionStatisticalResult.setSampleSize(count);
        operatorProgressionStatisticalResult.setId(idGenerator.nextId());
        operatorProgressionStatisticalResult.setRecordType(RecordType.DISPLAY.code());
        operatorProgressionStatisticalResult.setCreateTime(new Date());
        String jsonString = JsonMapper.toJSONString(list);
        operatorProgressionStatisticalResult.setStatisticalResult(jsonString);
        operatorProgressionStatisticalResultMapper.insert(operatorProgressionStatisticalResult);

        LogUtils.info("本次统计干员练度的抽样人数为：" + count + "人次");

    }


    /**
     * 干员信息统计
     *
     * @return 成功消息
     */
    @RedisCacheable(key = "Survey:OperatorProgressionStatistics", timeout = 1200)
    public OperatorProgressionStatisticalResultVOV2 getOperatorProgressionStatisticalResultV2() {
        LambdaUpdateWrapper<OperatorProgressionStatisticalResult> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(OperatorProgressionStatisticalResult::getRecordType, RecordType.DISPLAY.code());
        List<OperatorProgressionStatisticalResult> list = operatorProgressionStatisticalResultMapper.selectList(queryWrapper);
        if (list.isEmpty()) {
            queryWrapper.clear();
            queryWrapper.orderByDesc(OperatorProgressionStatisticalResult::getCreateTime).last("limit 1");
            list = operatorProgressionStatisticalResultMapper.selectList(queryWrapper);
        }
        OperatorProgressionStatisticalResult operatorProgressionStatisticalResult = list.get(0);

        List<OperatorProgressionStatisticalResultDTO> progressionStatisticalResultDTOList = JsonMapper.parseJSONArray(operatorProgressionStatisticalResult.getStatisticalResult(), new TypeReference<>() {
        });

        OperatorProgressionStatisticalResultVOV2 operatorProgressionStatisticalResultVOV2 = new OperatorProgressionStatisticalResultVOV2();
        operatorProgressionStatisticalResultVOV2.setSampleSize(operatorProgressionStatisticalResult.getSampleSize());
        operatorProgressionStatisticalResultVOV2.setRecordType(RecordType.DISPLAY.code());
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
        existQueryWrapper.eq(OperatorProgressionStatisticalResult::getRecordType, RecordType.ARCHIVED.code())
                .ge(OperatorProgressionStatisticalResult::getCreateTime, startOfDay)
                .le(OperatorProgressionStatisticalResult::getCreateTime, endOfDay);

        boolean exists = operatorProgressionStatisticalResultMapper.exists(existQueryWrapper);
        if (exists) {
            LogUtils.info("干员携带率统计结果今日已归档");
            return;
        }

        LambdaUpdateWrapper<OperatorProgressionStatisticalResult> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(OperatorProgressionStatisticalResult::getRecordType, RecordType.DISPLAY.code())
                .orderByDesc(OperatorProgressionStatisticalResult::getCreateTime);
        List<OperatorProgressionStatisticalResult> list = operatorProgressionStatisticalResultMapper.selectList(queryWrapper);
        OperatorProgressionStatisticalResult operatorProgressionStatisticalResult = list.get(0);
        operatorProgressionStatisticalResult.setRecordType(RecordType.ARCHIVED.code());
        operatorProgressionStatisticalResult.setId(idGenerator.nextId());

        int i = operatorProgressionStatisticalResultMapper.insert(operatorProgressionStatisticalResult);
        LogUtils.info("干员携带率统计结果归档成功" + i + "条");

    }

    public void deleteExpireData() {
        LambdaQueryWrapper<OperatorProgressionStatisticalResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperatorProgressionStatisticalResult::getRecordType, RecordType.EXPIRE.code());
        int delete = operatorProgressionStatisticalResultMapper.delete(queryWrapper);
        LogUtils.info("本次清理了" + delete + "条过期干员携带率统计数据");
    }


    public void backup() {

    }


}
