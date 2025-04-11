package com.lhs.service.material;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.enums.RecordType;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.enums.TimeGranularity;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.LogUtils;
import com.lhs.entity.dto.material.QueryStageDropDTO;
import com.lhs.entity.dto.material.StageDropCollect;
import com.lhs.entity.dto.material.StageDropDetailDTO;
import com.lhs.entity.po.material.StageDrop;
import com.lhs.entity.po.material.StageDropStatisticalTaskLog;
import com.lhs.entity.po.material.StageDropStatistics;
import com.lhs.entity.vo.material.StageDropStatisticsVO;
import com.lhs.mapper.material.StageDropMapper;
import com.lhs.mapper.material.StageDropStatisticsMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class StageDropService {

    private final StageDropMapper stageDropMapper;

    private final StageDropStatisticsMapper stageDropStatisticsMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private final IdGenerator idGenerator;


    public StageDropService(StageDropMapper stageDropMapper,
                            StageDropStatisticsMapper stageDropStatisticsMapper,
                            RedisTemplate<String, Object> redisTemplate) {
        this.stageDropMapper = stageDropMapper;
        this.stageDropStatisticsMapper = stageDropStatisticsMapper;
        this.redisTemplate = redisTemplate;
        this.idGenerator = new IdGenerator(1L);
    }

    public List<StageDropStatisticsVO> getStageDropByStageId(QueryStageDropDTO queryStageDropDTO) {
        List<StageDropStatistics> stageDropStatisticsList = stageDropStatisticsMapper.
                listByStageId(queryStageDropDTO.getStageId(), queryStageDropDTO.getTimeGranularity(),
                        new Date(queryStageDropDTO.getStart()), new Date(queryStageDropDTO.getEnd()));

        if (stageDropStatisticsList.isEmpty()) {
            throw new ServiceException(ResultCode.DATA_NONE);
        }

        List<StageDropStatisticsVO> voList = new ArrayList<>();
        for (StageDropStatistics po : stageDropStatisticsList) {
            StageDropStatisticsVO stageDropStatisticsVO = new StageDropStatisticsVO();
            stageDropStatisticsVO.setStageId(po.getStageId());
            stageDropStatisticsVO.setItemId(po.getItemId());
            stageDropStatisticsVO.setTimes(po.getTimes());
            stageDropStatisticsVO.setQuantity(po.getQuantity());
            stageDropStatisticsVO.setStart(po.getStartTime().getTime());
            stageDropStatisticsVO.setEnd(po.getEndTime().getTime());
            voList.add(stageDropStatisticsVO);
        }
        return voList;
    }


    public void stageDropHourlyStatistics(Long start) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH");
        long hour = 60 * 60 * 1000L;
        for (int i = 0; i < 10000; i++) {
            Date startTime = new Date(start);
            Date endTime = new Date(start + hour);
            stageDropHourlyStatistics(startTime, endTime, simpleDateFormat);
            //查询后将start递增
            start += hour;
        }
    }

    public void stageDropHourlyStatistics(Date startTime, Date endTime, SimpleDateFormat simpleDateFormat) {

        LogUtils.info("{}开始执行关卡掉率统计——时段" + simpleDateFormat.format(startTime) + "-" + simpleDateFormat.format(endTime) + "{}");

        Date date = new Date();
        Date defaultDate = new Date(1556676000000L);
        int HOUR = TimeGranularity.HOUR.code();

        //先查询是否已经执行过当前统计时段和时间粒度的任务
        StageDropStatisticalTaskLog oldTaskLog = stageDropStatisticsMapper.getTaskLog(HOUR, startTime, endTime);

        //查询当前统计时段的数据总量
        Integer countByDate = stageDropMapper.countByDate(startTime, endTime);

        //判断是否有旧日志,如果数量总量和任务日志中的总量相同，认为当前时间的数据已统计完成，不再执行

        if (oldTaskLog != null && oldTaskLog.getDataCount().equals(countByDate)) {
            LogUtils.info("该时段已归档，不再进行统计");
            return;
        }

        LambdaUpdateWrapper<StageDropStatistics> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StageDropStatistics::getTimeGranularity, HOUR)
                .ge(StageDropStatistics::getStartTime, startTime)
                .lt(StageDropStatistics::getStartTime, endTime);

        StageDropStatistics updateEntity = new StageDropStatistics();
        updateEntity.setRecordCode(RecordType.EXPIRE.code());
        int update = stageDropStatisticsMapper.update(updateEntity, updateWrapper);
        if (update > 0) {
            LogUtils.info("将" + update + "条旧数据过期");
        }

        //如果没有执行过当前统计时段和时间粒度的任务或统计未完成，执行下面的逻辑

        //新建一个任务执行日志
        StageDropStatisticalTaskLog taskLog = new StageDropStatisticalTaskLog();
        taskLog.setId(idGenerator.nextId());
        taskLog.setCreateTime(date);
        taskLog.setTimeGranularity(HOUR);

        //如果数据总量为空
        if (countByDate == 0) {
            taskLog.setEndTime(defaultDate);
            taskLog.setStartTime(defaultDate);
            taskLog.setDataCount(0);
//            System.out.println(taskLog);
            stageDropStatisticsMapper.insertTaskLog(taskLog);

            LogUtils.info("数据总量为空，本次统计任务结束");
            return;
        }

        //查询当前时段的数据
        List<StageDrop> stageDropList = stageDropMapper.listStageDropByDate(startTime, endTime);

        taskLog.setEndTime(stageDropList.get(countByDate - 1).getCreateTime());
        taskLog.setStartTime(stageDropList.get(0).getCreateTime());
        taskLog.setDataCount(countByDate);


        Map<String, StageDropCollect> dropCollectHashMap = new HashMap<>();

        for (StageDrop stageDrop : stageDropList) {
            String stageId = stageDrop.getStageId();
            String server = stageDrop.getServer();
            if (!"CN".equals(server)) {
                continue;
            }
            Integer times = stageDrop.getTimes();
            if (times > 1) {
                continue;
            }

            StageDropCollect item = dropCollectHashMap.get(stageId);

            if (item == null) {
                item = new StageDropCollect();
                // 将新对象放入 map
                dropCollectHashMap.put(stageId, item);
            }

            item.addTimes(1);
            String drops = stageDrop.getDrops();
            if (drops != null && drops.length() > 5) {
                List<StageDropDetailDTO> stageDropDetailDTOList = JsonMapper.parseJSONArray(drops, new TypeReference<>() {
                });
                for (StageDropDetailDTO dropDetail : stageDropDetailDTOList) {
                    Integer quantity = dropDetail.getQuantity();
                    String itemId = dropDetail.getItemId();
                    item.getDropMap().merge(itemId, quantity, Integer::sum);
                }
            }
        }

        List<StageDropStatistics> stageDropStatisticsList = new ArrayList<>();
        for (String stageId : dropCollectHashMap.keySet()) {
            StageDropCollect stageDropCollect = dropCollectHashMap.get(stageId);
            Integer times = stageDropCollect.getTimes();
            Map<String, Integer> drops = stageDropCollect.getDropMap();
            for (String itemId : drops.keySet()) {
                Integer quantity = drops.get(itemId);
                StageDropStatistics stageDropStatistics = new StageDropStatistics();
                stageDropStatistics.setId(idGenerator.nextId());
                stageDropStatistics.setTimes(times);
                stageDropStatistics.setStageId(stageId);
                stageDropStatistics.setItemId(itemId);
                stageDropStatistics.setQuantity(quantity);
                stageDropStatistics.setStartTime(startTime);
                stageDropStatistics.setEndTime(endTime);
                stageDropStatistics.setTimeGranularity(HOUR);
                stageDropStatistics.setCreateTime(date);
                stageDropStatisticsList.add(stageDropStatistics);
            }
        }

        LogUtils.info("统计了" + countByDate + "条");

        if (stageDropStatisticsList.isEmpty()) {
            return;
        }

        if (oldTaskLog != null) {
            taskLog.setId(oldTaskLog.getId());
            Integer i = stageDropStatisticsMapper.updateTaskLog(taskLog);
            LogUtils.info("更新了统计日志" + taskLog);
        } else {
            Integer i = stageDropStatisticsMapper.insertTaskLog(taskLog);
            LogUtils.info("新增了统计日志" + taskLog);
        }


        Integer i1 = stageDropStatisticsMapper.insertBatch(stageDropStatisticsList);
    }


    public void stageDropDailyStatistics(Long start) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH");
        long hour = 60 * 60 * 24 * 1000L;
        for (int i = 0; i < 500; i++) {
            Date startTime = new Date(start);
            Date endTime = new Date(start + hour);
            stageDropDailyStatistics(startTime, endTime, simpleDateFormat);
            //查询后将start递增
            start += hour;
        }

    }


    public void stageDropDailyStatistics(Date startTime, Date endTime, SimpleDateFormat simpleDateFormat) {
        List<StageDropStatistics> stageDropStatisticsList = stageDropStatisticsMapper.listByDate(TimeGranularity.HOUR.code(), startTime, endTime);
        Map<String, StageDropCollect> dropCollectHashMap = new HashMap<>();
        for(StageDropStatistics stageDropStatistics:stageDropStatisticsList){
            String itemId = stageDropStatistics.getItemId();
            String stageId = stageDropStatistics.getStageId();
            Integer times = stageDropStatistics.getTimes();
            StageDropCollect item = dropCollectHashMap.get(stageId);
            if (item == null) {
                item = new StageDropCollect();
                // 将新对象放入 map
                dropCollectHashMap.put(stageId, item);
            }

            item.getTimesMap().merge(itemId,stageDropStatistics.getQuantity(),Integer::sum);
        }

    }


    private void setStartAndEnd(StageDropStatisticsVO vo, Date period, Integer timeGranularity) {
        if (timeGranularity == 2) {
            vo.setStart(period.getTime() - 60 * 60 * 1000);
            vo.setEnd(period.getTime() + 60 * 60);
        }

        vo.setStart(period.getTime() - 60 * 60 * 1000);
        vo.setEnd(period.getTime());
    }
}
