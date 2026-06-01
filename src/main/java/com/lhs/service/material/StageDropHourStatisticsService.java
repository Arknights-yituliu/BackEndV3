package com.lhs.service.material;

import com.lhs.common.enums.RecordType;
import com.lhs.common.enums.TimeGranularity;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.Logger;
import com.lhs.common.util.TimeUtil;

import com.lhs.entity.dto.drop.StageDropQuantityCountDTO;
import com.lhs.entity.dto.drop.StageDropQuantityCountRawDTO;
import com.lhs.entity.dto.drop.StageDropTimeRangeDTO;
import com.lhs.entity.dto.drop.StageDropTimesCountRawDTO;
import com.lhs.entity.po.material.StageDropStatisticsTaskLog;
import com.lhs.entity.po.material.StageDropHourStatistics;
import com.lhs.mapper.material.StageDropMapper;
import com.lhs.mapper.material.StageDropHourStatisticsMapper;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StageDropHourStatisticsService {

    private final StageDropMapper stageDropMapper;

    private final StageDropHourStatisticsMapper stageDropHourStatisticsMapper;

    private final IdGenerator idGenerator;

    public StageDropHourStatisticsService(StageDropMapper stageDropMapper,
            StageDropHourStatisticsMapper stageDropHourStatisticsMapper) {
        this.stageDropMapper = stageDropMapper;
        this.stageDropHourStatisticsMapper = stageDropHourStatisticsMapper;
        this.idGenerator = new IdGenerator(6L);

    }

    private static final Date DEFAULT_DATE = new Date(1556676000000L);

    /**
     * 统计上一个小时的关卡掉率数据
     */
    public void statisticsLastHour() {
        Date endTime = TimeUtil.getCurrentHourTime();
        long oneHour = 60 * 60 * 1000L;
        Date startTime = new Date(endTime.getTime() - oneHour);

        Calendar cal = Calendar.getInstance();
        cal.setTime(startTime);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        String tableName = String.format("stage_drop_%d_%02d", year, month);

        stageDropHourlyStatisticsV2(startTime, endTime, tableName);
    }

    /**
     * 每小时关卡掉落统计入口
     */
    public void stageDropHourlyStatisticsV2(Date startTime, Date endTime, String tableName) {
        // 如果没有执行过当前统计时段和时间粒度的任务或统计未完成，执行下面的逻辑
        Date date = new Date();
        // long startMillis = System.currentTimeMillis();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Logger.info("开始执行关卡掉率统计——表名：" + tableName + "——时段" + simpleDateFormat.format(startTime) + "-"
                + simpleDateFormat.format(endTime));

        // 查询当前统计时段的数据总量和时间边界（一次轻量查询替代 countByDate + selectListByDate）
        StageDropTimeRangeDTO timeRange = stageDropMapper.selectTimeRange(tableName, startTime, endTime);
        int countByDate = timeRange.getCnt().intValue();

        // Logger.info("查询时间范围耗时：" + (System.currentTimeMillis() - startMillis) + "ms");

        // 新建一个任务执行日志
        StageDropStatisticsTaskLog taskLog = new StageDropStatisticsTaskLog();
        taskLog.setId(idGenerator.nextId());
        taskLog.setCreateTime(date);
        taskLog.setTimeGranularity(TimeGranularity.HOUR.code());
        taskLog.setDataCount(countByDate);
        taskLog.setStartTime(new Date(startTime.getTime() + 10000));
        taskLog.setEndTime(new Date(endTime.getTime() - 10000));

        if (countByDate == 0) {
            stageDropHourStatisticsMapper.insertTaskLog(taskLog);
            Logger.info("当前时间段数据为空，本次统计任务结束");
            return;
        }

        // 先查询是否已经执行过当前统计时段和时间粒度的任务
        StageDropStatisticsTaskLog oldTaskLog = stageDropHourStatisticsMapper.getTaskLogByTimeGranularityAndDate(
                TimeGranularity.HOUR.code(),
                startTime, endTime);

        if (oldTaskLog != null) {
            taskLog.setId(oldTaskLog.getId());
            if (oldTaskLog.getDataCount() > 0) {
                int rows = stageDropHourStatisticsMapper.expireHourStatisticsByRecordId(taskLog.getId(), RecordType.EXPIRE.code());
                if (rows > 0) {
                    Logger.info("过期了" + rows + "条旧数据");
                }
            }
        }

        // Logger.info("更新旧数据过期耗时：" + (System.currentTimeMillis() - startMillis) +
        // "ms");

        Date firstDateCreateTime = timeRange.getFirstTime();
        Date lastDateCreateTime = timeRange.getLastTime();

        // 更新任务日志中的数据，第一条数据的开始时间，最后一条数据的结束时间，数据总量
        taskLog.setStartTime(firstDateCreateTime);
        taskLog.setEndTime(lastDateCreateTime);

        // SQL-1：按 stageId 聚合时段内总 times
        List<StageDropTimesCountRawDTO> stageTimesList = stageDropMapper.selectStageTimesByDate(tableName, startTime,
                endTime);
        Map<String, Long> dropTimesHashMap = new HashMap<>();
        for (StageDropTimesCountRawDTO dto : stageTimesList) {
            dropTimesHashMap.put(dto.getStageId(), dto.getTimes());
        }

        // Logger.info("查询 stageTimesList耗时：" + (System.currentTimeMillis() -
        // startMillis) + "ms");

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

        // Logger.info("查询聚合掉落数量耗时：" + (System.currentTimeMillis() - startMillis) +
        // "ms");

        List<StageDropHourStatistics> stageDropHourStatisticsList = new ArrayList<>();
        for (StageDropQuantityCountDTO stageDropQuantityCountDTO : dropQuantityMap.values()) {
            StageDropHourStatistics stageDropHourStatistics = new StageDropHourStatistics();
            stageDropHourStatistics.setId(idGenerator.nextId());
            stageDropHourStatistics.setRecordId(taskLog.getId());
            stageDropHourStatistics.setStageId(stageDropQuantityCountDTO.getStageId());
            stageDropHourStatistics.setItemId(stageDropQuantityCountDTO.getItemId());
            stageDropHourStatistics.setTimes(dropTimesHashMap.get(stageDropQuantityCountDTO.getStageId()));
            stageDropHourStatistics.setQuantity(stageDropQuantityCountDTO.getQuantity());
            stageDropHourStatistics.setStartTime(firstDateCreateTime);
            stageDropHourStatistics.setEndTime(lastDateCreateTime);
            stageDropHourStatistics.setTimeGranularity(TimeGranularity.HOUR.code());
            stageDropHourStatistics.setCreateTime(date);
            stageDropHourStatistics.setRecordCode(RecordType.ARCHIVED.code());
            stageDropHourStatisticsList.add(stageDropHourStatistics);
        }

        // Logger.info("拼接统计结果耗时：" + (System.currentTimeMillis() - startMillis) + "ms");

        if (oldTaskLog != null) {
            stageDropHourStatisticsMapper.updateTaskLog(taskLog);
            Logger.info("更新了统计日志");
        } else {
            stageDropHourStatisticsMapper.insertTaskLog(taskLog);
            Logger.info("新增了统计日志");
        }

        Logger.info("统计了" + countByDate + "条");

        if (stageDropHourStatisticsList.isEmpty()) {
            Logger.info("统计结果为空");
            return;
        }

        // Logger.info("更新统计日志耗时：" + (System.currentTimeMillis() - startMillis) + "ms");

        Integer i1 = stageDropHourStatisticsMapper.insertBatch(stageDropHourStatisticsList);
        Logger.info("新增了" + i1 + "条统计结果");
        // Logger.info("执行完毕耗时：" + (System.currentTimeMillis() - startMillis) + "ms");
    }

}
