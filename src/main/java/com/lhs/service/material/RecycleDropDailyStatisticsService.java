package com.lhs.service.material;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lhs.common.enums.RecordType;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.Logger;
import com.lhs.common.util.TimeUtil;
import com.lhs.entity.dto.drop.RecycleDropDailyCountDTO;
import com.lhs.entity.po.material.RecycleDropDailyStatistics;
import com.lhs.entity.po.material.RecycleDropDailyStatisticsTask;
import com.lhs.entity.vo.drop.RecycleDropBackfillResultVO;
import com.lhs.entity.vo.drop.RecycleDropItemTotalVO;
import com.lhs.mapper.material.RecycleDropDailyStatisticsMapper;
import com.lhs.mapper.material.RecycleDropDailyStatisticsTaskMapper;
import com.lhs.mapper.material.RecycleDropMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 回收站每日物品产出统计服务。
 * <p>
 * 统计 recycle_drop 中每个物品在一天内的产出总数量（quantity）与上报批次数（times），
 * 按 stat_day 写入 recycle_drop_daily_statistics，同一天只保留最新一次任务的数据。
 * <p>
 * 两个定时任务共用同一份数据，统计区间统一为完整自然日：
 * <ul>
 *   <li>每小时任务：重算当天，当天数据仍在增长，下一次任务继续覆盖刷新；</li>
 *   <li>每天 4:00、22:00：重算昨天完整一天，补齐补传数据。</li>
 * </ul>
 * 重跑流程：查任务记录 → 旧 task_id 的数据置为 EXPIRE → 生成新 task_id 写入 DISPLAY
 * → 更新任务记录指向新 task_id，保证可重跑、可补偿。
 */
@Service
public class RecycleDropDailyStatisticsService {

    /** 单次回填的最大天数，防止误传过大参数长时间占用数据库连接 */
    private static final int MAX_BACKFILL_DAYS = 90;

    /** 一天的毫秒数，用于校验统计区间是否为完整自然日 */
    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L;

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RecycleDropMapper recycleDropMapper;

    private final RecycleDropDailyStatisticsMapper recycleDropDailyStatisticsMapper;

    private final RecycleDropDailyStatisticsTaskMapper recycleDropDailyStatisticsTaskMapper;

    /** 用于回填时给每一天单独开事务：类内自调用不会经过 Spring 代理，@Transactional 不生效 */
    private final TransactionTemplate transactionTemplate;

    private final IdGenerator idGenerator;

    /** 回填任务执行标记：回填接口不鉴权，用它挡住并发或重复点击，避免同一天数据被重复写入 */
    private final AtomicBoolean backfillRunning = new AtomicBoolean(false);

    public RecycleDropDailyStatisticsService(RecycleDropMapper recycleDropMapper,
                                             RecycleDropDailyStatisticsMapper recycleDropDailyStatisticsMapper,
                                             RecycleDropDailyStatisticsTaskMapper recycleDropDailyStatisticsTaskMapper,
                                             PlatformTransactionManager transactionManager) {
        this.recycleDropMapper = recycleDropMapper;
        this.recycleDropDailyStatisticsMapper = recycleDropDailyStatisticsMapper;
        this.recycleDropDailyStatisticsTaskMapper = recycleDropDailyStatisticsTaskMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        // workerId 取 7：与项目内已使用的 1/3/6 区分，避免同毫秒内主键冲突
        this.idGenerator = new IdGenerator(7L);
    }

    /**
     * 重算当天数据，统计区间 [今天00:00, 明天00:00)。
     * <p>
     * 每小时执行一次；上界取明天零点保证所有入口都是完整自然日，
     * 当天数据仍在增长，下一次任务会继续覆盖刷新。
     *
     * @return 本次统计的物品数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int statisticsToday() {
        Date startDay = TimeUtil.getCurrentDayTime();
        return statistics(startDay, plusDays(startDay, 1));
    }

    /**
     * 重算昨天完整一天的数据，统计区间 [昨天00:00, 今天00:00)。
     * <p>
     * 每天 4:00、22:00 各执行一次；22:00 的重跑用于补齐 4:00 之后补传的前一天数据。
     *
     * @return 本次统计的物品数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int statisticsYesterday() {
        Date startDay = plusDays(TimeUtil.getCurrentDayTime(), -1);
        return statistics(startDay, plusDays(startDay, 1));
    }

    /**
     * 回填过去 days 天（不含今天）的回收站物品产出统计。
     * <p>
     * 供运维手动触发补数：按天独立事务执行，某天失败只记录不中断，已完成的日期不回滚；
     * 同一天重复回填会覆盖旧结果，与定时任务的重跑逻辑一致。
     *
     * @param days 回填天数，取值 1 ~ {@link #MAX_BACKFILL_DAYS}
     * @return 回填结果，包含成功/失败天数与失败日期
     */
    public RecycleDropBackfillResultVO backfill(int days) {
        if (days <= 0 || days > MAX_BACKFILL_DAYS) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }
        // 回填接口不鉴权：同一时间只允许一个任务执行，第二个请求直接拒绝
        if (!backfillRunning.compareAndSet(false, true)) {
            throw new ServiceException(ResultCode.NOT_REPEAT_REQUESTS);
        }
        try {
            return doBackfill(days);
        } finally {
            backfillRunning.set(false);
        }
    }

    private RecycleDropBackfillResultVO doBackfill(int days) {
        // 回填区间：[今天-days 00:00, 今天 00:00)，共 days 个完整自然日，不含今天
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_YEAR, -days);
        Date firstDay = cal.getTime();

        long startMillis = System.currentTimeMillis();
        int successDays = 0;
        long totalItems = 0L;
        RecycleDropBackfillResultVO result = new RecycleDropBackfillResultVO();
        result.setTotalDays(days);

        for (int i = 0; i < days; i++) {
            // 每天的起止时间在循环内重新声明，保证 lambda 捕获的是当次迭代的值
            Date dayStart = plusDays(firstDay, i);
            Date dayEnd = plusDays(firstDay, i + 1);
            try {
                Integer itemCount = transactionTemplate.execute(
                        status -> statistics(dayStart, dayEnd));
                successDays++;
                totalItems += itemCount == null ? 0 : itemCount;
            } catch (Exception e) {
                // 单天失败不影响其他日期，记录失败日期后继续
                result.getFailedDates().add(formatDay(dayStart));
                Logger.error("回收站每日产出回填失败：" + formatDay(dayStart), e);
            }
        }

        result.setSuccessDays(successDays);
        result.setFailedDays(days - successDays);
        result.setTotalItems(totalItems);
        Logger.info("回收站每日产出回填完成：共 {} 天，成功 {} 天，失败 {} 天，物品统计行数 {}，耗时 {} ms",
                days, successDays, days - successDays, totalItems, System.currentTimeMillis() - startMillis);
        return result;
    }

    /**
     * 查询全部未过期数据中每个物品的产出总数与上报批次数总和。
     *
     * @return 按产出总数降序的物品统计，无数据时返回空列表
     */
    public List<RecycleDropItemTotalVO> getAllItemTotal() {
        List<RecycleDropDailyCountDTO> countList = recycleDropDailyStatisticsMapper.selectAllItemTotal();
        List<RecycleDropItemTotalVO> result = new ArrayList<>(countList.size());
        for (RecycleDropDailyCountDTO count : countList) {
            RecycleDropItemTotalVO vo = new RecycleDropItemTotalVO();
            vo.setItemId(count.getItemId());
            vo.setQuantity(count.getQuantity());
            vo.setTimes(count.getTimes());
            result.add(vo);
        }
        return result;
    }

    /**
     * 统计指定时间区间 [startDay, endDay) 内每个物品的产出并写入 recycle_drop_daily_statistics。
     * startDay 作为统计日期的标识，写入 stat_day 字段（强制对齐到当天零点）。
     * <p>
     * 只接受完整自然日区间：该方法会覆盖该日已有的展示数据，如果允许部分区间重算，
     * 会把完整一天的结果覆盖成残缺数据，因此非整日区间直接跳过。
     * <p>
     * 流程：
     * 1. 查询任务记录表 recycle_drop_daily_statistics_task 该日是否执行过
     * 2. 若执行过：将统计表中对应旧 task_id 的记录 recordCode 置为 EXPIRE
     * 3. 生成新 task_id，按 item_id 聚合产出并写入（recordCode=DISPLAY）
     * 4. 更新/插入任务记录表，使其 task_id 指向新值
     *
     * @param startDay 统计起始日期（含），同时作为 stat_day 标识
     * @param endDay   统计结束日期（不含），必须是 startDay 之后的整日边界
     * @return 本次统计的物品数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int statistics(Date startDay, Date endDay) {
        // 区间非法或不是整日边界时直接跳过：避免用部分区间把已有的完整数据覆盖成残缺数据
        if (startDay == null || endDay == null) {
            Logger.warn("回收站每日产出统计：统计区间为空 startDay={} endDay={}，跳过本次统计", startDay, endDay);
            return 0;
        }
        startDay = truncateToDay(startDay);
        endDay = truncateToDay(endDay);
        long rangeMillis = endDay.getTime() - startDay.getTime();
        if (rangeMillis <= 0 || rangeMillis % ONE_DAY_MILLIS != 0) {
            Logger.warn("回收站每日产出统计：只支持完整自然日区间 startDay={} endDay={}，跳过本次统计",
                    startDay, endDay);
            return 0;
        }

        // 按 item_id 聚合区间内的产出数量与上报批次数
        List<RecycleDropDailyCountDTO> countList = recycleDropMapper.selectDailyItemCount(startDay, endDay);

        // 1. 查询任务记录表：该日是否已执行过
        RecycleDropDailyStatisticsTask oldTask = recycleDropDailyStatisticsTaskMapper.selectOne(
                new LambdaQueryWrapper<RecycleDropDailyStatisticsTask>()
                        .eq(RecycleDropDailyStatisticsTask::getStatDay, startDay)
        );

        // 2. 若执行过，将旧 task_id 对应的统计数据标记为过期
        if (oldTask != null) {
            RecycleDropDailyStatistics expireUpdate = new RecycleDropDailyStatistics();
            expireUpdate.setRecordCode(RecordType.EXPIRE.code());
            int expiredRows = recycleDropDailyStatisticsMapper.update(
                    expireUpdate,
                    new LambdaQueryWrapper<RecycleDropDailyStatistics>()
                            .eq(RecycleDropDailyStatistics::getTaskId, oldTask.getTaskId())
                            .eq(RecycleDropDailyStatistics::getRecordCode, RecordType.DISPLAY.code())
            );
            if (expiredRows > 0) {
                Logger.info("回收站每日产出统计：{} 旧数据 {} 条已标记为过期", formatDay(startDay), expiredRows);
            }
        }

        // 3. 生成新 task_id，批量写入新的统计数据
        long newTaskId = idGenerator.nextId();
        if (!countList.isEmpty()) {
            List<RecycleDropDailyStatistics> batch = new ArrayList<>(countList.size());
            for (RecycleDropDailyCountDTO count : countList) {
                RecycleDropDailyStatistics stats = new RecycleDropDailyStatistics();
                stats.setId(idGenerator.nextId());
                stats.setStatDay(startDay);
                stats.setItemId(count.getItemId());
                stats.setQuantity(count.getQuantity());
                stats.setTimes(count.getTimes());
                stats.setTaskId(newTaskId);
                stats.setRecordCode(RecordType.DISPLAY.code());
                batch.add(stats);
            }
            recycleDropDailyStatisticsMapper.insertBatch(batch);
        }

        // 4. 更新/插入任务记录表，task_id 指向新值
        if (oldTask != null) {
            oldTask.setTaskId(newTaskId);
            oldTask.setDataCount((long) countList.size());
            oldTask.setUpdateTime(new Date());
            recycleDropDailyStatisticsTaskMapper.updateById(oldTask);
        } else {
            RecycleDropDailyStatisticsTask newTask = new RecycleDropDailyStatisticsTask();
            newTask.setId(idGenerator.nextId());
            newTask.setStatDay(startDay);
            newTask.setTaskId(newTaskId);
            newTask.setDataCount((long) countList.size());
            recycleDropDailyStatisticsTaskMapper.insert(newTask);
        }

        Logger.info("回收站每日产出统计完成：{} 共 {} 个物品 task_id {}",
                formatDay(startDay), countList.size(), newTaskId);
        return countList.size();
    }

    /**
     * 删除 recycle_drop_daily_statistics 表中 recordCode=EXPIRE 的过期数据
     * 由定时任务定期调用，避免重跑历史数据堆积。
     *
     * @return 删除的记录数
     */
    public int deleteExpireStatistics() {
        int deleted = recycleDropDailyStatisticsMapper.delete(
                new LambdaQueryWrapper<RecycleDropDailyStatistics>()
                        .eq(RecycleDropDailyStatistics::getRecordCode, RecordType.EXPIRE.code())
        );
        Logger.info("删除过期回收站每日产出统计数据 {} 条", deleted);
        return deleted;
    }

    /**
     * 将时间截断到当天零点（时分秒毫秒置零）
     *
     * @param date 原始时间
     * @return 当天零点
     */
    private Date truncateToDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 在指定时间上增加天数，用于按天遍历统计区间
     *
     * @param date 基准时间
     * @param days 增加的天数
     * @return 增加后的时间
     */
    private Date plusDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }

    /**
     * 将日期格式化为 yyyy-MM-dd，仅用于日志输出
     *
     * @param date 待格式化的时间
     * @return 日期字符串
     */
    private String formatDay(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(DAY_FORMATTER);
    }
}
