package com.lhs.service.material;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.enums.RecordType;
import com.lhs.common.enums.TimeGranularity;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.entity.dto.drop.StageDropCount;
import com.lhs.entity.dto.material.StageDropDetailDTO;
import com.lhs.entity.dto.material.StageDropTimesStatistics;
import com.lhs.entity.po.material.StageDrop;
import com.lhs.entity.po.material.StageDropStatisticalTaskLog;
import com.lhs.entity.po.material.StageDropStatistics;
import com.lhs.mapper.material.StageDropMapper;
import com.lhs.mapper.material.StageDropStatisticsMapper;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StageDropStatisticsService {

    private final StageDropMapper stageDropMapper;

    private final StageDropStatisticsMapper stageDropStatisticsMapper;

    private final IdGenerator idGenerator;

    public StageDropStatisticsService(StageDropMapper stageDropMapper,
            StageDropStatisticsMapper stageDropStatisticsMapper) {
        this.stageDropMapper = stageDropMapper;
        this.stageDropStatisticsMapper = stageDropStatisticsMapper;
        this.idGenerator = new IdGenerator(1L);

    }

    private static final Date DEFAULT_DATE = new Date(1556676000000L);

    /**
     * 每小时关卡掉落统计入口
     */
    public void stageDropHourlyStatistics(Date startTime, Date endTime,String tableName)  {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Logger.info("开始执行关卡掉率统计——表名：" + tableName + "——时段" + simpleDateFormat.format(startTime) + "-" + simpleDateFormat.format(endTime));
        int HOUR = TimeGranularity.HOUR.code();
        // 先查询是否已经执行过当前统计时段和时间粒度的任务
        StageDropStatisticalTaskLog oldTaskLog = stageDropStatisticsMapper.getTaskLog(HOUR, startTime, endTime);
        // 查询当前统计时段的数据总量
        Integer countByDate = stageDropMapper.countFromTableByDate(tableName, startTime, endTime);

        // 判断是否有旧日志,如果数量总量和任务日志中的总量相同，认为当前时间的数据已统计完成，不再执行统计
        if (oldTaskLog != null && oldTaskLog.getDataCount().equals(countByDate)) {
            Logger.info("该时段已归档，不再进行统计");
            return;
        }

        LambdaUpdateWrapper<StageDropStatistics> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StageDropStatistics::getTimeGranularity, HOUR)
                .ge(StageDropStatistics::getStartTime, startTime)
                .lt(StageDropStatistics::getEndTime, endTime);

        StageDropStatistics updateEntity = new StageDropStatistics();
        updateEntity.setRecordCode(RecordType.EXPIRE.code());
        int update = stageDropStatisticsMapper.update(updateEntity, updateWrapper);
        if (update > 0) {
            Logger.info("将" + update + "条旧数据过期");
        }

        // 如果没有执行过当前统计时段和时间粒度的任务或统计未完成，执行下面的逻辑
        Date date = new Date();
        // 新建一个任务执行日志
        StageDropStatisticalTaskLog taskLog = new StageDropStatisticalTaskLog();
        taskLog.setId(idGenerator.nextId());
        taskLog.setCreateTime(date);
        taskLog.setTimeGranularity(HOUR);

        // 如果数据总量为空，直接结束任务
        if (countByDate == 0) {
            taskLog.setEndTime(DEFAULT_DATE);
            taskLog.setStartTime(DEFAULT_DATE);
            taskLog.setDataCount(0);
            // System.out.println(taskLog);
            stageDropStatisticsMapper.insertTaskLog(taskLog);

            Logger.info("数据总量为空，本次统计任务结束");
            return;
        }

        // 查询当前时段的数据
        List<StageDrop> stageDropList = stageDropMapper.selectFromTableListByDate(tableName, startTime, endTime);

        // 补充任务日志中的数据，第一条数据的开始时间，最后一条数据的结束时间，数据总量
        taskLog.setEndTime(stageDropList.get(countByDate - 1).getCreateTime());
        taskLog.setStartTime(stageDropList.get(0).getCreateTime());
        taskLog.setDataCount(countByDate);

        Map<String, StageDropCount> dropCollectHashMap = new HashMap<>();
        for (StageDrop stageDrop : stageDropList) {
            String stageId = stageDrop.getStageId();
            Integer times = stageDrop.getTimes();

            String drops = stageDrop.getDrops();
            if (drops != null && drops.length() > 5) {
                List<StageDropDetailDTO> stageDropDetailDTOList = JsonMapper.parseJSONArray(drops,
                        new TypeReference<>() {
                        });
                for (StageDropDetailDTO stageDropDetailDTO : stageDropDetailDTOList) {
                    String itemId = stageDropDetailDTO.getItemId();
                    Integer quantity = stageDropDetailDTO.getQuantity();
                    String collectKey = stageId + "_" + itemId;
                    StageDropCount stageDropCount = dropCollectHashMap.get(collectKey);
                    if (stageDropCount == null) {
                        stageDropCount = new StageDropCount(stageId, itemId, 0, 0);
                        dropCollectHashMap.put(collectKey, stageDropCount);
                    }
                    stageDropCount.addQuantity(quantity);
                    stageDropCount.addTimes(times);
                }
            }
        }

        List<StageDropStatistics> stageDropStatisticsList = new ArrayList<>();
        for (StageDropCount stageDropCount : dropCollectHashMap.values()) {
            StageDropStatistics stageDropStatistics = new StageDropStatistics();
            stageDropStatistics.setId(idGenerator.nextId());
            stageDropStatistics.setStageId(stageDropCount.getStageId());
            stageDropStatistics.setItemId(stageDropCount.getItemId());
            stageDropStatistics.setTimes(stageDropCount.getTimes());
            stageDropStatistics.setQuantity(stageDropCount.getQuantity());
            stageDropStatistics.setStartTime(startTime);
            stageDropStatistics.setEndTime(endTime);
            stageDropStatistics.setTimeGranularity(HOUR);
            stageDropStatistics.setCreateTime(date);
            stageDropStatisticsList.add(stageDropStatistics);
        }

        Logger.info("统计了" + countByDate + "条");

        if (stageDropStatisticsList.isEmpty()) {
            Logger.info("统计结果为空");
            return;
        }

        if (oldTaskLog != null) {
            taskLog.setId(oldTaskLog.getId());
            stageDropStatisticsMapper.updateTaskLog(taskLog);
            Logger.info("更新了统计日志" + taskLog);
        } else {
            stageDropStatisticsMapper.insertTaskLog(taskLog);
            Logger.info("新增了统计日志" + taskLog);
        }

        Integer i1 = stageDropStatisticsMapper.insertBatch(stageDropStatisticsList);
        Logger.info("新增了" + i1 + "条统计结果");
    }


}
