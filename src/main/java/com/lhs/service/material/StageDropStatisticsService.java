package com.lhs.service.material;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.enums.RecordType;
import com.lhs.common.enums.TimeGranularity;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;

import com.lhs.entity.dto.drop.StageDropQuantityCountDTO;
import com.lhs.entity.dto.drop.StageDropQuantityCountRawDTO;
import com.lhs.entity.dto.drop.StageDropTimeRangeDTO;
import com.lhs.entity.dto.drop.StageDropTimesCountRawDTO;
import com.lhs.entity.dto.material.StageDropDetailDTO;
import com.lhs.entity.po.material.StageDrop;
import com.lhs.entity.po.material.StageDropStatisticsTaskLog;
import com.lhs.entity.po.material.StageDropStatistics;
import com.lhs.mapper.material.StageDropMapper;
import com.lhs.mapper.material.StageDropStatisticsMapper;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
        this.idGenerator = new IdGenerator(5L);

    }

    private static final Date DEFAULT_DATE = new Date(1556676000000L);

    // /**
    // * 每小时关卡掉落统计入口
    // */
    // public void stageDropHourlyStatistics(Date startTime, Date endTime, String
    // tableName) {
    // SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd
    // HH:mm:ss");
    // Logger.info("开始执行关卡掉率统计——表名：" + tableName + "——时段" +
    // simpleDateFormat.format(startTime) + "-"
    // + simpleDateFormat.format(endTime));
    //
    // // 查询当前统计时段的数据总量
    // Integer countByDate = stageDropMapper.countByDate(tableName, startTime,
    // endTime);
    //
    // // 如果数据总量为空，直接结束任务
    // if (countByDate == 0) {
    // Logger.info("当前时间段数据为空，本次统计任务结束");
    // return;
    // }
    //
    // // 如果没有执行过当前统计时段和时间粒度的任务或统计未完成，执行下面的逻辑
    // Date date = new Date();
    //
    // LambdaUpdateWrapper<StageDropStatistics> updateWrapper = new
    // LambdaUpdateWrapper<>();
    // updateWrapper.eq(StageDropStatistics::getTimeGranularity,
    // TimeGranularity.HOUR.code())
    // .ge(StageDropStatistics::getStartTime, startTime)
    // .le(StageDropStatistics::getEndTime, endTime);
    //
    // StageDropStatistics updateEntity = new StageDropStatistics();
    // updateEntity.setRecordCode(RecordType.EXPIRE.code());
    // int update = stageDropStatisticsMapper.update(updateEntity, updateWrapper);
    // if (update > 0) {
    // Logger.info("将" + update + "条旧数据过期");
    // }
    //
    // // 查询当前时段的数据
    // List<StageDrop> stageDropList = stageDropMapper.selectListByDate(tableName,
    // startTime, endTime);
    //
    // Date firstDateCreateTime = stageDropList.get(0).getCreateTime();
    // Date lastDateCreateTime = stageDropList.get(countByDate - 1).getCreateTime();
    //
    // Map<String, StageDropQuantityCountDTO> dropQuantityMap = new HashMap<>();
    // Map<String, Long> dropTimesHashMap = new HashMap<>();
    //
    // for (StageDrop stageDrop : stageDropList) {
    // String stageId = stageDrop.getStageId();
    // long times = stageDrop.getTimes().longValue();
    // if (dropTimesHashMap.get(stageId) == null) { // 判空
    // dropTimesHashMap.put(stageId, 0L); // 初始化
    // }
    //
    // dropTimesHashMap.put(stageId, dropTimesHashMap.get(stageId) + times); // get
    // + 计算 + put
    // String drops = stageDrop.getDrops();
    // if (drops != null && drops.length() > 5) {
    // List<StageDropDetailDTO> stageDropDetailDTOList =
    // JsonMapper.parseJSONArray(drops,
    // new TypeReference<>() {
    // });
    //
    // for (StageDropDetailDTO stageDropDetailDTO : stageDropDetailDTOList) {
    // String itemId = stageDropDetailDTO.getItemId();
    // long quantity = stageDropDetailDTO.getQuantity().longValue();
    // String collectKey = stageId + "_" + itemId;
    // StageDropQuantityCountDTO stageDropQuantityCountDTO =
    // dropQuantityMap.get(collectKey);
    //
    // if (stageDropQuantityCountDTO == null) {
    // stageDropQuantityCountDTO = new StageDropQuantityCountDTO(stageId, itemId,
    // firstDateCreateTime,
    // lastDateCreateTime, 0L);
    // dropQuantityMap.put(collectKey, stageDropQuantityCountDTO);
    // }
    //
    // stageDropQuantityCountDTO.addQuantity(quantity);
    //
    // }
    // }
    // }
    //
    // List<StageDropStatistics> stageDropStatisticsList = new ArrayList<>();
    // for (StageDropQuantityCountDTO stageDropQuantityCountDTO :
    // dropQuantityMap.values()) {
    // StageDropStatistics stageDropStatistics = new StageDropStatistics();
    // stageDropStatistics.setId(idGenerator.nextId());
    // stageDropStatistics.setStageId(stageDropQuantityCountDTO.getStageId());
    // stageDropStatistics.setItemId(stageDropQuantityCountDTO.getItemId());
    // stageDropStatistics.setTimes(dropTimesHashMap.get(stageDropQuantityCountDTO.getStageId()));
    // stageDropStatistics.setQuantity(stageDropQuantityCountDTO.getQuantity());
    // stageDropStatistics.setStartTime(firstDateCreateTime);
    // stageDropStatistics.setEndTime(lastDateCreateTime);
    // stageDropStatistics.setTimeGranularity(TimeGranularity.HOUR.code());
    // stageDropStatistics.setCreateTime(date);
    // stageDropStatistics.setRecordCode(RecordType.ARCHIVED.code());
    // stageDropStatisticsList.add(stageDropStatistics);
    // }
    //
    // Logger.info("统计了" + countByDate + "条");
    //
    // if (stageDropStatisticsList.isEmpty()) {
    // Logger.info("统计结果为空");
    // return;
    // }
    //
    // // 先查询是否已经执行过当前统计时段和时间粒度的任务
    // StageDropStatisticsTaskLog oldTaskLog =
    // stageDropStatisticsMapper.getTaskLogByTimeGranularityAndDate(
    // TimeGranularity.HOUR.code(),
    // startTime, endTime);
    //
    // // 新建一个任务执行日志
    // StageDropStatisticsTaskLog taskLog = new StageDropStatisticsTaskLog();
    // taskLog.setId(idGenerator.nextId());
    // taskLog.setCreateTime(date);
    // taskLog.setTimeGranularity(TimeGranularity.HOUR.code());
    // // 补充任务日志中的数据，第一条数据的开始时间，最后一条数据的结束时间，数据总量
    // taskLog.setStartTime(firstDateCreateTime);
    // taskLog.setEndTime(lastDateCreateTime);
    // taskLog.setDataCount(countByDate);
    //
    // if (oldTaskLog != null) {
    // taskLog.setId(oldTaskLog.getId());
    // stageDropStatisticsMapper.updateTaskLog(taskLog);
    // Logger.info("更新了统计日志");
    // } else {
    // stageDropStatisticsMapper.insertTaskLog(taskLog);
    // Logger.info("新增了统计日志");
    // }
    //
    // Integer i1 = stageDropStatisticsMapper.insertBatch(stageDropStatisticsList);
    // Logger.info("新增了" + i1 + "条统计结果");
    // }

    /**
     * 每小时关卡掉落统计入口
     */
    public void stageDropHourlyStatisticsV2(Date startTime, Date endTime, String tableName) {
        long startMillis = System.currentTimeMillis();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Logger.info("开始执行关卡掉率统计——表名：" + tableName + "——时段" + simpleDateFormat.format(startTime) + "-"
                + simpleDateFormat.format(endTime));

        // 查询当前统计时段的数据总量和时间边界（一次轻量查询替代 countByDate + selectListByDate）
        StageDropTimeRangeDTO timeRange = stageDropMapper.selectTimeRange(tableName, startTime, endTime);
        Integer countByDate = timeRange.getCnt().intValue();

        Logger.info("查询时间范围耗时：" + (System.currentTimeMillis() - startMillis) + "ms");

        if (countByDate == 0) {
            Logger.info("当前时间段数据为空，本次统计任务结束");
            return;
        }

        // 如果没有执行过当前统计时段和时间粒度的任务或统计未完成，执行下面的逻辑
        Date date = new Date();

        // 先查询是否已经执行过当前统计时段和时间粒度的任务
        StageDropStatisticsTaskLog oldTaskLog = stageDropStatisticsMapper.getTaskLogByTimeGranularityAndDate(
                TimeGranularity.HOUR.code(),
                startTime, endTime);

        if (oldTaskLog != null) {
            int expire = stageDropStatisticsMapper.expireByTimeRange(
                    RecordType.EXPIRE.code(), TimeGranularity.HOUR.code(), startTime, endTime);
            if (expire > 0) {
                Logger.info("将" + expire + "条旧数据过期");
            }
        }

        Logger.info("更新旧数据过期耗时：" + (System.currentTimeMillis() - startMillis) + "ms");

        Date firstDateCreateTime = timeRange.getFirstTime();
        Date lastDateCreateTime = timeRange.getLastTime();

        // SQL-1：按 stageId 聚合时段内总 times
        List<StageDropTimesCountRawDTO> stageTimesList = stageDropMapper.selectStageTimesByDate(tableName, startTime,
                endTime);
        Map<String, Long> dropTimesHashMap = new HashMap<>();
        for (StageDropTimesCountRawDTO dto : stageTimesList) {
            dropTimesHashMap.put(dto.getStageId(), dto.getTimes());
        }

        Logger.info("查询 stageTimesList耗时：" + (System.currentTimeMillis() - startMillis) + "ms");

        // SQL-2：按 stageId + itemId 聚合掉落数量（JSON_TABLE 在 DB 内展开）
        List<StageDropQuantityCountRawDTO> dropStatsList = stageDropMapper.selectDropStatsByDate(tableName, startTime,
                endTime);
        Map<String, StageDropQuantityCountDTO> dropQuantityMap = new HashMap<>();
        for (StageDropQuantityCountRawDTO dto : dropStatsList) {
            String collectKey = dto.getStageId() + "_" + dto.getItemId();
            StageDropQuantityCountDTO dto2 = new StageDropQuantityCountDTO(
                    dto.getStageId(), dto.getItemId(), startTime, endTime, dto.getQuantity());
            dropQuantityMap.put(collectKey, dto2);
        }

        Logger.info("查询聚合掉落数量耗时：" + (System.currentTimeMillis() - startMillis) + "ms");

        List<StageDropStatistics> stageDropStatisticsList = new ArrayList<>();
        for (StageDropQuantityCountDTO stageDropQuantityCountDTO : dropQuantityMap.values()) {
            StageDropStatistics stageDropStatistics = new StageDropStatistics();
            stageDropStatistics.setId(idGenerator.nextId());
            stageDropStatistics.setStageId(stageDropQuantityCountDTO.getStageId());
            stageDropStatistics.setItemId(stageDropQuantityCountDTO.getItemId());
            stageDropStatistics.setTimes(dropTimesHashMap.get(stageDropQuantityCountDTO.getStageId()));
            stageDropStatistics.setQuantity(stageDropQuantityCountDTO.getQuantity());
            stageDropStatistics.setStartTime(firstDateCreateTime);
            stageDropStatistics.setEndTime(lastDateCreateTime);
            stageDropStatistics.setTimeGranularity(TimeGranularity.HOUR.code());
            stageDropStatistics.setCreateTime(date);
            stageDropStatistics.setRecordCode(RecordType.ARCHIVED.code());
            stageDropStatisticsList.add(stageDropStatistics);
        }

        Logger.info("拼接统计结果耗时：" + (System.currentTimeMillis() - startMillis) + "ms");

        Logger.info("统计了" + countByDate + "条");

        if (stageDropStatisticsList.isEmpty()) {
            Logger.info("统计结果为空");
            return;
        }

        // 新建一个任务执行日志
        StageDropStatisticsTaskLog taskLog = new StageDropStatisticsTaskLog();
        taskLog.setId(idGenerator.nextId());
        taskLog.setCreateTime(date);
        taskLog.setTimeGranularity(TimeGranularity.HOUR.code());
        // 补充任务日志中的数据，第一条数据的开始时间，最后一条数据的结束时间，数据总量
        taskLog.setStartTime(firstDateCreateTime);
        taskLog.setEndTime(lastDateCreateTime);
        taskLog.setDataCount(countByDate);

        if (oldTaskLog != null) {
            taskLog.setId(oldTaskLog.getId());
            stageDropStatisticsMapper.updateTaskLog(taskLog);
            Logger.info("更新了统计日志");
        } else {
            stageDropStatisticsMapper.insertTaskLog(taskLog);
            Logger.info("新增了统计日志");
        }

        Logger.info("更新统计日志耗时：" + (System.currentTimeMillis() - startMillis) + "ms");

        Integer i1 = stageDropStatisticsMapper.insertBatch(stageDropStatisticsList);
        Logger.info("新增了" + i1 + "条统计结果");
        Logger.info("执行完毕耗时：" + (System.currentTimeMillis() - startMillis) + "ms");
    }

}
