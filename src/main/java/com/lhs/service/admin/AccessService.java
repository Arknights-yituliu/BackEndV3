package com.lhs.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.Logger;
import com.lhs.common.util.UserAgentUtil;
import com.lhs.entity.dto.AccessLogDTO;
import com.lhs.entity.dto.UrlCountDTO;
import com.lhs.common.enums.RecordType;
import com.lhs.entity.po.admin.AccessLog;
import com.lhs.entity.po.admin.AccessLogHourlyStats;
import com.lhs.entity.po.admin.AccessLogHourlyStatsTask;
import com.lhs.entity.po.admin.AccessLogUrlDailyStats;
import com.lhs.entity.po.admin.AccessLogUrlDailyStatsTask;
import com.lhs.entity.po.admin.PageViewStatistics;
import com.lhs.entity.vo.dev.UrlPeriodDataVO;
import com.lhs.entity.vo.dev.UrlTotalVisitVO;
import com.lhs.entity.vo.dev.UrlVisitGroupVO;
import com.lhs.mapper.admin.AccessLogHourlyStatsMapper;
import com.lhs.mapper.admin.AccessLogHourlyStatsTaskMapper;
import com.lhs.mapper.admin.AccessLogMapper;
import com.lhs.mapper.admin.AccessLogUrlDailyStatsMapper;
import com.lhs.mapper.admin.AccessLogUrlDailyStatsTaskMapper;
import com.lhs.mapper.admin.PageVisitsMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 访问记录服务
 * 负责访问日志的保存、流量统计、URL访问分析等功能
 */
@Service
public class AccessService {

    /** 每批查询的记录数 */
    private static final int BATCH_SIZE = 50_000;

    /** 每批插入的记录数，避免单批事务过大 */
    private static final int INSERT_BATCH_SIZE = 1_000;

    /** Top URL 数量 */
    private static final int TOP_URL_COUNT = 30;

    private static final SimpleDateFormat HOUR_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:00");
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final AccessLogMapper accessLogMapper;
    private final PageVisitsMapper pageVisitsMapper;
    private final AccessLogHourlyStatsMapper accessLogHourlyStatsMapper;
    private final AccessLogHourlyStatsTaskMapper accessLogHourlyStatsTaskMapper;
    private final AccessLogUrlDailyStatsMapper accessLogUrlDailyStatsMapper;
    private final AccessLogUrlDailyStatsTaskMapper accessLogUrlDailyStatsTaskMapper;
    private final IdGenerator idGenerator;
    private final RedisTemplate<String, Object> redisTemplate;

    /** Redis中记录上次迁移日期的键 */
    private static final String MIGRATE_LAST_DATE_KEY = "migrate:lastSyncedDate";

    /** 迁移起始标记日期（2026-07-13的后一天），首次执行时回退到 2026-07-13 */
    private static final String MIGRATE_START_DATE = "2026-07-14";

    public AccessService(AccessLogMapper accessLogMapper, PageVisitsMapper pageVisitsMapper,
            AccessLogHourlyStatsMapper accessLogHourlyStatsMapper,
            AccessLogHourlyStatsTaskMapper accessLogHourlyStatsTaskMapper,
            AccessLogUrlDailyStatsMapper accessLogUrlDailyStatsMapper,
            AccessLogUrlDailyStatsTaskMapper accessLogUrlDailyStatsTaskMapper,
            RedisTemplate<String, Object> redisTemplate) {
        this.accessLogMapper = accessLogMapper;
        this.pageVisitsMapper = pageVisitsMapper;
        this.accessLogHourlyStatsMapper = accessLogHourlyStatsMapper;
        this.accessLogHourlyStatsTaskMapper = accessLogHourlyStatsTaskMapper;
        this.accessLogUrlDailyStatsMapper = accessLogUrlDailyStatsMapper;
        this.accessLogUrlDailyStatsTaskMapper = accessLogUrlDailyStatsTaskMapper;
        this.idGenerator = new IdGenerator(3L);
        this.redisTemplate = redisTemplate;
    }

    /**
     * 保存访问日志
     *
     * @param request      HTTP请求对象
     * @param accessLogDTO 访问日志数据（url, region）
     */
    public void saveAccessLog(HttpServletRequest request, AccessLogDTO accessLogDTO) {
        AccessLog accessLog = new AccessLog();

        accessLog.setId(idGenerator.nextId());

        // Referer
        String referer = request.getHeader("Referer");
        accessLog.setReferer(referer != null ? referer : "Unknown");

        // URL（去除 ? 及后面的查询参数）
        String url = accessLogDTO.getUrl();
        if (url != null) {
            int queryIndex = url.indexOf('?');
            if (queryIndex >= 0) {
                url = url.substring(0, queryIndex);
            }
        }
        accessLog.setUrl(url != null ? url : "Empty");

        // 访问时间
        accessLog.setAccessTime(new Date());

        // IP
        accessLog.setIp(IpUtil.getIpAddress(request));

        // User-Agent 解析
        accessLog.setBrowser(UserAgentUtil.getBrowser(request));
        accessLog.setOs(UserAgentUtil.getOs(request));
        accessLog.setDevice(UserAgentUtil.getDevice(request));

        // 若浏览器/OS/设备任一为Unknown，保存完整User-Agent备查
        if ("Unknown".equals(accessLog.getBrowser()) || "Unknown".equals(accessLog.getOs()) || "Unknown".equals(accessLog.getDevice())) {
            accessLog.setUserAgent(UserAgentUtil.getUserAgent(request));
        }

        // 地域
        String region = accessLogDTO.getRegion();
        accessLog.setRegion(region != null ? region : "Unknown");

        accessLogMapper.insert(accessLog);
    }


    /**
     * 解析 visitsTime 字符串，支持格式 yyyy-MM-dd HH:mm
     */
    private Date parseVisitsTime(String visitsTime) {
        if (visitsTime == null || visitsTime.isEmpty()) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(visitsTime);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 统计指定时间范围内每个URL每天的访问次数
     * 直接读取 access_log_url_daily_stats 预聚合表中 recordCode=DISPLAY 的记录，避免实时全表扫描
     * 按URL总访问量降序返回Top 30 URL，零值补齐
     * 注意：当天数据需等次日定时任务统计后才完整，可能缺失或偏低
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return URL分组访问量列表
     */
    public List<UrlVisitGroupVO> getUrlDailyVisits(Date startTime, Date endTime) {
        long diffMillis = endTime.getTime() - startTime.getTime();
        if (diffMillis <= 0) {
            throw new ServiceException(ResultCode.START_TIME_CANNOT_BE_GREATER_THAN_END_TIME);
        }
        if (diffMillis > 30L * 24 * 60 * 60 * 1000) {
            throw new ServiceException(ResultCode.DATE_RANGE_TOO_LARGE);
        }

        // 查询区间内已统计的每日URL数据，stat_day 从起始日00:00到结束日00:00（闭区间），仅取展示数据
        Date startDay = truncateToDay(startTime);
        Date endDay = truncateToDay(endTime);

        List<AccessLogUrlDailyStats> stats = accessLogUrlDailyStatsMapper.selectList(
                new LambdaQueryWrapper<AccessLogUrlDailyStats>()
                        .ge(AccessLogUrlDailyStats::getStatDay, startDay)
                        .le(AccessLogUrlDailyStats::getStatDay, endDay)
                        .eq(AccessLogUrlDailyStats::getRecordCode, RecordType.DISPLAY.code())
        );

        // url → (day → count)
        Map<String, Map<String, Long>> urlDayCount = new HashMap<>();
        for (AccessLogUrlDailyStats stat : stats) {
            String day = DAY_FORMAT.format(stat.getStatDay());
            urlDayCount
                    .computeIfAbsent(stat.getUrl(), k -> new HashMap<>())
                    .merge(day, stat.getVisitCount(), Long::sum);
        }

        // 按总访问量降序取前 TOP_URL_COUNT 个URL
        List<String> topUrls = urlDayCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(
                        b.getValue().values().stream().mapToLong(Long::longValue).sum(),
                        a.getValue().values().stream().mapToLong(Long::longValue).sum()))
                .limit(TOP_URL_COUNT)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 生成全部日期列表
        List<String> allDays = generateAllDays(startTime, endTime);

        // 展开为VO
        List<UrlVisitGroupVO> result = new ArrayList<>(topUrls.size());
        for (String url : topUrls) {
            Map<String, Long> dayCount = urlDayCount.getOrDefault(url, Collections.emptyMap());
            List<UrlPeriodDataVO> dataList = new ArrayList<>(allDays.size());
            for (String day : allDays) {
                dataList.add(new UrlPeriodDataVO(day, dayCount.getOrDefault(day, 0L)));
            }
            result.add(new UrlVisitGroupVO(url, dataList));
        }

        return result;
    }

    /**
     * 定时统计昨天的每个URL访问量并写入 access_log_url_daily_stats 表
     * 计算昨天的起止时间，委托给 {@link #statisticsUrlDailyVisits(Date, Date)} 执行核心统计
     *
     * @return 本次统计写入的URL数量
     */
    public long statisticsYesterdayUrlDailyVisits() {
        // 计算昨天的起止时间：[昨天00:00, 今天00:00)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date endDay = cal.getTime();          // 今天00:00 = 昨天的结束边界
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Date startDay = cal.getTime();        // 昨天00:00

        return statisticsUrlDailyVisits(startDay, endDay);
    }

    /**
     * 定时统计今天的每个URL访问量并写入 access_log_url_daily_stats 表
     * 计算今天的起止时间，委托给 {@link #statisticsUrlDailyVisits(Date, Date)} 执行核心统计
     * 今天的数据仍在持续产生，定时任务会反复重跑刷新今天的最新数据
     *
     * @return 本次统计写入的URL数量
     */
    public long statisticsTodayUrlDailyVisits() {
        // 计算今天的起止时间：[今天00:00, 明天00:00)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startDay = cal.getTime();        // 今天00:00
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Date endDay = cal.getTime();          // 明天00:00 = 今天的结束边界

        return statisticsUrlDailyVisits(startDay, endDay);
    }

    /**
     * 统计指定日期区间 [startDay, endDay) 内每个URL的访问量并写入 access_log_url_daily_stats 表
     * startDay 作为统计日期的标识，写入 stat_day 字段（强制对齐到当天零点）
     * 流程（与小时统计一致）：
     * 1. 查询任务记录表 access_log_url_daily_stats_task 该日是否执行过
     * 2. 若执行过：将 access_log_url_daily_stats 中对应旧 task_id 的记录 recordCode 置为 EXPIRE
     * 3. 生成新 task_id，按URL分组统计访问量并写入（recordCode=DISPLAY）
     * 4. 更新/插入任务记录表，使其 task_id 指向新值
     *
     * @param startDay 统计起始日期（当天00:00，含），同时作为 stat_day 标识
     * @param endDay   统计结束日期（当天00:00，不含）
     * @return 本次统计写入的URL数量
     */
    public long statisticsUrlDailyVisits(Date startDay, Date endDay) {
        // 强制将 startDay 对齐到当天零点，保证 stat_day 标识的唯一性与匹配一致性
        startDay = truncateToDay(startDay);

        // 按URL分组统计区间内的访问量，URL标准化去尾部斜杠后聚合
        List<UrlCountDTO> urlCounts = accessLogMapper.countByUrl(startDay, endDay);
        Map<String, Long> urlCountMap = new HashMap<>();
        for (UrlCountDTO dto : urlCounts) {
            String url = normalizeUrl(dto.getUrl());
            urlCountMap.merge(url, dto.getCount(), Long::sum);
        }

        // 1. 查询任务记录表：该日是否已执行过
        AccessLogUrlDailyStatsTask oldTask = accessLogUrlDailyStatsTaskMapper.selectOne(
                new LambdaQueryWrapper<AccessLogUrlDailyStatsTask>()
                        .eq(AccessLogUrlDailyStatsTask::getStatDay, startDay)
        );

        // 2. 若执行过，将旧 task_id 对应的统计数据标记为过期
        if (oldTask != null) {
            AccessLogUrlDailyStats expireUpdate = new AccessLogUrlDailyStats();
            expireUpdate.setRecordCode(RecordType.EXPIRE.code());
            int expiredRows = accessLogUrlDailyStatsMapper.update(
                    expireUpdate,
                    new LambdaQueryWrapper<AccessLogUrlDailyStats>()
                            .eq(AccessLogUrlDailyStats::getTaskId, oldTask.getTaskId())
                            .eq(AccessLogUrlDailyStats::getRecordCode, RecordType.DISPLAY.code())
            );
            if (expiredRows > 0) {
                Logger.info("URL每日访问量统计：{} 旧数据 {} 条已标记为过期", DAY_FORMAT.format(startDay), expiredRows);
            }
        }

        // 3. 生成新 task_id，写入新的统计数据
        long newTaskId = idGenerator.nextId();
        for (Map.Entry<String, Long> entry : urlCountMap.entrySet()) {
            AccessLogUrlDailyStats stats = new AccessLogUrlDailyStats();
            stats.setId(idGenerator.nextId());
            stats.setUrl(entry.getKey());
            stats.setStatDay(startDay);
            stats.setVisitCount(entry.getValue());
            stats.setTaskId(newTaskId);
            stats.setRecordCode(RecordType.DISPLAY.code());
            accessLogUrlDailyStatsMapper.insert(stats);
        }

        // 4. 更新/插入任务记录表，task_id 指向新值
        if (oldTask != null) {
            oldTask.setTaskId(newTaskId);
            oldTask.setDataCount((long) urlCountMap.size());
            oldTask.setUpdateTime(new Date());
            accessLogUrlDailyStatsTaskMapper.updateById(oldTask);
        } else {
            AccessLogUrlDailyStatsTask newTask = new AccessLogUrlDailyStatsTask();
            newTask.setId(idGenerator.nextId());
            newTask.setStatDay(startDay);
            newTask.setTaskId(newTaskId);
            newTask.setDataCount((long) urlCountMap.size());
            accessLogUrlDailyStatsTaskMapper.insert(newTask);
        }

        Logger.info("URL每日访问量统计完成：{} 共 {} 个URL task_id {}", DAY_FORMAT.format(startDay), urlCountMap.size(), newTaskId);
        return urlCountMap.size();
    }

    /**
     * 删除 access_log_url_daily_stats 表中 recordCode=EXPIRE 的过期数据
     * 由定时任务定期调用，避免重跑历史数据堆积
     *
     * @return 删除的记录数
     */
    public int deleteExpireUrlDailyStats() {
        int deleted = accessLogUrlDailyStatsMapper.delete(
                new LambdaQueryWrapper<AccessLogUrlDailyStats>()
                        .eq(AccessLogUrlDailyStats::getRecordCode, RecordType.EXPIRE.code())
        );
        Logger.info("删除过期URL每日访问量统计数据 {} 条", deleted);
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
     * 统计指定时间范围内每个URL的总访问次数
     * 直接读取 access_log_url_daily_stats 预聚合表中 recordCode=DISPLAY 的记录聚合得出，避免实时全表扫描
     * Top 15 单独列出，其余合并为"其他"，第一项固定为"访问总和"
     * 注意：当天数据需等次日定时任务统计后才完整，可能缺失或偏低
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return URL总访问量列表
     */
    public List<UrlTotalVisitVO> getUrlTotalVisits(Date startTime, Date endTime) {
        long diffMillis = endTime.getTime() - startTime.getTime();
        if (diffMillis <= 0) {
            throw new ServiceException(ResultCode.START_TIME_CANNOT_BE_GREATER_THAN_END_TIME);
        }
        if (diffMillis > 30L * 24 * 60 * 60 * 1000) {
            throw new ServiceException(ResultCode.DATE_RANGE_TOO_LARGE);
        }

        // 查询区间内已统计的每日URL数据，stat_day 从起始日00:00到结束日00:00（闭区间），仅取展示数据
        Date startDay = truncateToDay(startTime);
        Date endDay = truncateToDay(endTime);

        List<AccessLogUrlDailyStats> stats = accessLogUrlDailyStatsMapper.selectList(
                new LambdaQueryWrapper<AccessLogUrlDailyStats>()
                        .ge(AccessLogUrlDailyStats::getStatDay, startDay)
                        .le(AccessLogUrlDailyStats::getStatDay, endDay)
                        .eq(AccessLogUrlDailyStats::getRecordCode, RecordType.DISPLAY.code())
        );

        // 按URL聚合区间内总访问量
        Map<String, Long> urlCount = new HashMap<>();
        for (AccessLogUrlDailyStats stat : stats) {
            urlCount.merge(stat.getUrl(), stat.getVisitCount(), Long::sum);
        }

        // 按总访问量降序排序
        List<Map.Entry<String, Long>> sortedEntries = urlCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        List<UrlTotalVisitVO> result = new ArrayList<>();

        // Top 15
        for (int i = 0; i < Math.min(15, sortedEntries.size()); i++) {
            Map.Entry<String, Long> e = sortedEntries.get(i);
            result.add(new UrlTotalVisitVO(e.getKey(), e.getValue()));
        }

        // 其余合并为"其他"
        if (sortedEntries.size() > 15) {
            long otherCount = 0;
            for (int i = 15; i < sortedEntries.size(); i++) {
                otherCount += sortedEntries.get(i).getValue();
            }
            result.add(new UrlTotalVisitVO("其他", otherCount));
        }

        return result;
    }

    /**
     * 统计指定时间范围内每小时的总访问量（所有URL聚合）
     * 直接读取 access_log_hourly_stats 预聚合表中 recordCode=DISPLAY 的记录，避免实时全表扫描，零值补齐
     * 注意：当前进行中的小时尚未被定时任务统计，可能不在结果中或为0
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 每小时总访问量列表
     */
    public List<UrlPeriodDataVO> getHourlyTotalVisits(Date startTime, Date endTime) {
        long diffMillis = endTime.getTime() - startTime.getTime();
        if (diffMillis <= 0) {
            throw new ServiceException(ResultCode.START_TIME_CANNOT_BE_GREATER_THAN_END_TIME);
        }
        if (diffMillis > 30L * 24 * 60 * 60 * 1000) {
            throw new ServiceException(ResultCode.DATE_RANGE_TOO_LARGE);
        }

        // 查询区间内已统计的小时数据，stat_hour >= 区间起始整点 且 stat_hour <= 区间结束整点，仅取展示数据
        Date startHour = truncateToHour(startTime);
        Date endHour = truncateToHour(endTime);

        List<AccessLogHourlyStats> stats = accessLogHourlyStatsMapper.selectList(
                new LambdaQueryWrapper<AccessLogHourlyStats>()
                        .ge(AccessLogHourlyStats::getStatHour, startHour)
                        .le(AccessLogHourlyStats::getStatHour, endHour)
                        .eq(AccessLogHourlyStats::getRecordCode, RecordType.DISPLAY.code())
        );

        // stat_hour → visit_count
        Map<String, Long> hourCount = new HashMap<>();
        for (AccessLogHourlyStats stat : stats) {
            hourCount.put(HOUR_FORMAT.format(stat.getStatHour()), stat.getVisitCount());
        }

        // 按区间生成所有整点小时并补齐零值
        List<String> allHours = generateAllHours(startTime, endTime);
        List<UrlPeriodDataVO> result = new ArrayList<>(allHours.size());
        for (String hour : allHours) {
            result.add(new UrlPeriodDataVO(hour, hourCount.getOrDefault(hour, 0L)));
        }

        return result;
    }

    /**
     * 将时间截断到整点小时（分秒毫秒置零）
     *
     * @param date 原始时间
     * @return 整点时间
     */
    private Date truncateToHour(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 定时统计上一完整小时的访问量并写入 access_log_hourly_stats 表
     * 计算上一完整小时的起止时间，委托给 {@link #statisticsLastHour(Date, Date)} 执行核心统计
     *
     * @return 本次统计写入的访问量
     */
    public long statisticsLastHour() {
        // 计算上一完整小时的起止时间
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date endHour = cal.getTime();          // 当前整点 = 上一小时的结束边界
        cal.add(Calendar.HOUR_OF_DAY, -1);
        Date startHour = cal.getTime();        // 上一小时起始

        return statisticsLastHour(startHour, endHour);
    }

    /**
     * 统计指定时间区间 [startHour, endHour) 的访问量并写入 access_log_hourly_stats 表
     * startHour 作为统计小时的整点标识，写入 stat_hour 字段
     * 流程：
     * 1. 查询任务记录表 access_log_hourly_stats_task 当前小时是否执行过
     * 2. 若执行过：将 access_log_hourly_stats 中对应旧 task_id 的记录 recordCode 置为 EXPIRE
     * 3. 生成新 task_id，写入新的统计数据（recordCode=DISPLAY）
     * 4. 更新/插入任务记录表，使其 task_id 指向新值
     * 该流程保证可重跑、可补偿，重跑后旧数据被标记为过期，接口仅读到最新 DISPLAY 数据
     *
     * @param startHour 统计起始时间（整点，含），同时作为 stat_hour 标识
     * @param endHour   统计结束时间（不含）
     * @return 本次统计写入的访问量
     */
    public long statisticsLastHour(Date startHour, Date endHour) {
        // 强制将 startHour 对齐到整点（分秒毫秒置零），保证 stat_hour 标识的唯一性与匹配一致性
        startHour = truncateToHour(startHour);

        // 复用现成的 PV 统计 SQL（access_time >= startHour AND access_time < endHour）
        Long count = accessLogMapper.countPageViews(startHour, endHour);
        long visitCount = count != null ? count : 0L;

        // 1. 查询任务记录表：当前小时是否已执行过
        AccessLogHourlyStatsTask oldTask = accessLogHourlyStatsTaskMapper.selectOne(
                new LambdaQueryWrapper<AccessLogHourlyStatsTask>()
                        .eq(AccessLogHourlyStatsTask::getStatHour, startHour)
        );

        // 2. 若执行过，将旧 task_id 对应的统计数据标记为过期
        if (oldTask != null) {
            AccessLogHourlyStats expireUpdate = new AccessLogHourlyStats();
            expireUpdate.setRecordCode(RecordType.EXPIRE.code());
            int expiredRows = accessLogHourlyStatsMapper.update(
                    expireUpdate,
                    new LambdaQueryWrapper<AccessLogHourlyStats>()
                            .eq(AccessLogHourlyStats::getTaskId, oldTask.getTaskId())
                            .eq(AccessLogHourlyStats::getRecordCode, RecordType.DISPLAY.code())
            );
            if (expiredRows > 0) {
                Logger.info("小时访问量统计：{} 旧数据 {} 条已标记为过期", HOUR_FORMAT.format(startHour), expiredRows);
            }
        }

        // 3. 生成新 task_id，写入新的统计数据
        long newTaskId = idGenerator.nextId();
        AccessLogHourlyStats newStats = new AccessLogHourlyStats();
        newStats.setId(idGenerator.nextId());
        newStats.setStatHour(startHour);
        newStats.setVisitCount(visitCount);
        newStats.setTaskId(newTaskId);
        newStats.setRecordCode(RecordType.DISPLAY.code());
        accessLogHourlyStatsMapper.insert(newStats);

        // 4. 更新/插入任务记录表，task_id 指向新值
        if (oldTask != null) {
            oldTask.setTaskId(newTaskId);
            oldTask.setDataCount(visitCount);
            oldTask.setUpdateTime(new Date());
            accessLogHourlyStatsTaskMapper.updateById(oldTask);
        } else {
            AccessLogHourlyStatsTask newTask = new AccessLogHourlyStatsTask();
            newTask.setId(idGenerator.nextId());
            newTask.setStatHour(startHour);
            newTask.setTaskId(newTaskId);
            newTask.setDataCount(visitCount);
            accessLogHourlyStatsTaskMapper.insert(newTask);
        }

        Logger.info("每小时访问量统计完成：{} 访问量 {} task_id {}", HOUR_FORMAT.format(startHour), visitCount, newTaskId);
        return visitCount;
    }

    /**
     * 删除 access_log_hourly_stats 表中 recordCode=EXPIRE 的过期数据
     * 由定时任务定期调用，避免重跑历史数据堆积
     *
     * @return 删除的记录数
     */
    public int deleteExpireHourlyStats() {
        int deleted = accessLogHourlyStatsMapper.delete(
                new LambdaQueryWrapper<AccessLogHourlyStats>()
                        .eq(AccessLogHourlyStats::getRecordCode, RecordType.EXPIRE.code())
        );
        Logger.info("删除过期小时访问量统计数据 {} 条", deleted);
        return deleted;
    }

    /**
     * 统计指定时间范围内每日的总访问量（所有URL聚合）
     * 直接读取 access_log_url_daily_stats 预聚合表中 recordCode=DISPLAY 的记录按天聚合，避免实时全表扫描，零值补齐
     * 与 /access-log/url/total 使用同一张日统计表，保证两接口口径一致
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 每日总访问量列表
     */
    public List<UrlPeriodDataVO> getDailyTotalVisits(Date startTime, Date endTime) {
        long diffMillis = endTime.getTime() - startTime.getTime();
        if (diffMillis <= 0) {
            throw new ServiceException(ResultCode.START_TIME_CANNOT_BE_GREATER_THAN_END_TIME);
        }
        if (diffMillis > 30L * 24 * 60 * 60 * 1000) {
            throw new ServiceException(ResultCode.DATE_RANGE_TOO_LARGE);
        }

        // 查询区间内已统计的每日URL数据，stat_day 从起始日00:00到结束日00:00（闭区间），仅取展示数据
        Date startDay = truncateToDay(startTime);
        Date endDay = truncateToDay(endTime);

        List<AccessLogUrlDailyStats> stats = accessLogUrlDailyStatsMapper.selectList(
                new LambdaQueryWrapper<AccessLogUrlDailyStats>()
                        .ge(AccessLogUrlDailyStats::getStatDay, startDay)
                        .le(AccessLogUrlDailyStats::getStatDay, endDay)
                        .eq(AccessLogUrlDailyStats::getRecordCode, RecordType.DISPLAY.code())
        );

        // 按天汇总各URL访问量
        Map<String, Long> dayCount = new HashMap<>();
        for (AccessLogUrlDailyStats stat : stats) {
            String day = DAY_FORMAT.format(stat.getStatDay());
            dayCount.merge(day, stat.getVisitCount(), Long::sum);
        }

        // 按区间生成所有日期并补齐零值
        List<String> allDays = generateAllDays(startTime, endTime);
        List<UrlPeriodDataVO> result = new ArrayList<>(allDays.size());
        for (String day : allDays) {
            result.add(new UrlPeriodDataVO(day, dayCount.getOrDefault(day, 0L)));
        }

        return result;
    }

    /**
     * 生成时间范围内所有整点小时的时间字符串列表
     */
    private List<String> generateAllHours(Date startTime, Date endTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startTime);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.setTime(endTime);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);

        List<String> hours = new ArrayList<>();
        while (!cal.after(end)) {
            hours.add(HOUR_FORMAT.format(cal.getTime()));
            cal.add(Calendar.HOUR_OF_DAY, 1);
        }
        return hours;
    }

    /**
     * 生成时间范围内所有日期的字符串列表
     */
    private List<String> generateAllDays(Date startTime, Date endTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startTime);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.setTime(endTime);
        end.set(Calendar.HOUR_OF_DAY, 0);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);

        List<String> days = new ArrayList<>();
        while (!cal.after(end)) {
            days.add(DAY_FORMAT.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return days;
    }

    /**
     * 标准化URL，去除结尾斜杠
     */
    private String normalizeUrl(String url) {
        if (url == null) {
            return "Unknown";
        }
        if (url.length() > 1 && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }


        /**
     * 将旧的 page_visits 表数据迁移到新的 access_log 表
     * 每次执行迁移一天的旧数据，从 2026-07-13 开始向更早的日期逐天推进，基于 Redis 记录进度
     *
     * @return 本次迁移的记录数，0 表示没有待迁移的数据或已全部完成
     */
    public long migrateOldVisits() {
        Logger.info("旧数据迁移任务开始执行");

        // 从Redis获取上次迁移到的日期，首次执行默认为MIGRATE_START_DATE
        Object lastDateObj = redisTemplate.opsForValue().get(MIGRATE_LAST_DATE_KEY);
        String lastDateStr = lastDateObj != null ? lastDateObj.toString() : MIGRATE_START_DATE;
        Logger.info("上次迁移日期: {}", lastDateStr);

        // 计算下一个要迁移的日期（向更早的日期回退）
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(DAY_FORMAT.parse(lastDateStr));
        } catch (Exception e) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }
        cal.add(Calendar.DAY_OF_YEAR, -1);

        Date startOfDay = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Date endOfDay = cal.getTime();
        String syncingDate = DAY_FORMAT.format(startOfDay);
        Logger.info("开始迁移日期: {} 的数据", syncingDate);

        long totalMigrated = 0;
        List<AccessLog> batch = new ArrayList<>();
        long offset = 0;

        while (true) {
            List<PageViewStatistics> oldRecords = pageVisitsMapper.selectList(
                    new LambdaQueryWrapper<PageViewStatistics>()
                            .ge(PageViewStatistics::getCreateTime, startOfDay)
                            .lt(PageViewStatistics::getCreateTime, endOfDay)
                            .orderByAsc(PageViewStatistics::getCreateTime)
                            .last("LIMIT " + offset + "," + BATCH_SIZE)
            );

            if (oldRecords.isEmpty()) {
                break;
            }

            for (PageViewStatistics old : oldRecords) {
                int count = old.getPageView() != null ? old.getPageView() : 0;
                if (count <= 0) {
                    continue;
                }

                // 解析基准时间：优先用 viewTime 字段，回退到 createTime
                Date baseTime = parseVisitsTime(old.getViewTime());
                if (baseTime == null) {
                    baseTime = old.getCreateTime();
                }
                if (baseTime == null) {
                    baseTime = new Date();
                }

                // 根据 count 生成多条访问记录，时间在基准时间到基准时间+1小时之间均匀分布
                long baseMillis = baseTime.getTime();
                long slotMillis = 3600_000L; // 1小时

                for (int i = 0; i < count; i++) {
                    AccessLog log = new AccessLog();
                    log.setId(idGenerator.nextId());
                    log.setUrl(old.getPagePath());
                    log.setIp("Migration");
                    log.setRegion("Unknown");
                    log.setReferer("Migration");
                    log.setDevice("Unknown");
                    log.setBrowser("Unknown");
                    log.setOs("Unknown");
                    // 时间在1小时范围内均匀偏移，避免所有记录时间戳完全一致
                    long timeOffset = count > 1 ? (slotMillis * i / count) : 0;
                    log.setAccessTime(new Date(baseMillis + timeOffset));
                    batch.add(log);

                    if (batch.size() >= INSERT_BATCH_SIZE) {
                        for (AccessLog accessLog : batch) {
                            accessLogMapper.insert(accessLog);
                        }
                        totalMigrated += batch.size();
                        Logger.info("已迁移 {} 条记录，累计 {} 条", batch.size(), totalMigrated);
                        batch.clear();
                    }
                }
            }

            offset += BATCH_SIZE;
        }

        // 插入剩余的最后一小批
        if (!batch.isEmpty()) {
            for (AccessLog accessLog : batch) {
                accessLogMapper.insert(accessLog);
            }
            totalMigrated += batch.size();
            Logger.info("已迁移最后一批 {} 条记录", batch.size());
            batch.clear();
        }

        // 更新Redis中的进度到本次迁移的日期
        redisTemplate.opsForValue().set(MIGRATE_LAST_DATE_KEY, syncingDate);
        Logger.info("日期 {} 迁移完成，共迁移 {} 条记录", syncingDate, totalMigrated);

        return totalMigrated;
    }

}
