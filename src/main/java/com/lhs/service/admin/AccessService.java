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
import com.lhs.entity.vo.dev.UrlPeriodDataVO;
import com.lhs.entity.vo.dev.UrlTotalVisitVO;
import com.lhs.entity.vo.dev.UrlVisitGroupVO;
import com.lhs.mapper.admin.AccessLogHourlyStatsMapper;
import com.lhs.mapper.admin.AccessLogHourlyStatsTaskMapper;
import com.lhs.mapper.admin.AccessLogMapper;
import com.lhs.mapper.admin.AccessLogUrlDailyStatsMapper;
import com.lhs.mapper.admin.AccessLogUrlDailyStatsTaskMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 访问记录服务
 * 负责访问日志的保存、流量统计、URL访问分析等功能
 */
@Service
public class AccessService {

    /** Top URL 数量 */
    private static final int TOP_URL_COUNT = 30;

    /** 允许查询的最大时间范围（35 天），覆盖整月 31 天的边界情况 */
    private static final long MAX_RANGE_MILLIS = 35L * 24 * 60 * 60 * 1000;

    /**
     * 小时维度格式：yyyy-MM-dd HH:00
     * <p>
     * 使用 DateTimeFormatter（不可变、线程安全）而非 SimpleDateFormat：
     * 后者内部持有可变 Calendar，被静态共享时并发 format 会把 A 天的时间格式化成 B 天
     */
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");

    /** 天维度格式：yyyy-MM-dd */
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 日志字段最大长度，与 access_log.url / referer 的 varchar(500) 对齐 */
    private static final int LOG_FIELD_MAX_LENGTH = 500;

    /** 地域字段最大长度，与 access_log.region 的 varchar(100) 对齐 */
    private static final int REGION_MAX_LENGTH = 100;

    /** IP 字段最大长度，与 access_log.ip 的 varchar(50) 对齐 */
    private static final int IP_MAX_LENGTH = 50;

    /**
     * User-Agent 最大长度
     * <p>
     * 字段是 text（理论上限 65535 字节），但不做截断等于让客户端决定单行大小：
     * 几条 MB 级 UA 就能把整表撑爆，且 text 超长会退化为行外存储影响读取
     */
    private static final int USER_AGENT_MAX_LENGTH = 512;

    /**
     * 单日单个URL统计的最大落库条数
     * <p>
     * url 由客户端上报，基数不受控；若全量落库，日统计表与区间查询（最长 35 天）
     * 的内存占用随上报方的想象力线性增长。
     * 取 200 而不是接口展示的 30：接口取的是"区间内累计 Top30"，
     * 只留 30 会让"每天都排 31 名、但每天榜单都换人"的 URL 在跨区间累计时被误删。
     */
    private static final int URL_DAILY_STATS_MAX_ROWS = 200;

    /**
     * 按天格式化（yyyy-MM-dd）
     *
     * @param date 时间
     * @return 格式化结果，按 JVM 默认时区换算
     */
    private static String formatDay(Date date) {
        return DAY_FORMAT.format(toLocalDateTime(date));
    }

    /**
     * 裁剪 URL / Referer 的查询参数并限制长度
     * <p>
     * 只保留 "?" 之前的部分：query 里可能带 token 等凭证，也会让字段长度不可控
     *
     * @param value    原始值
     * @param fallback 值为 null 时的占位内容
     * @return 裁剪后的值
     */
    private static String sanitizeUrlField(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        int queryIndex = value.indexOf('?');
        String trimmed = queryIndex >= 0 ? value.substring(0, queryIndex) : value;
        if (trimmed.length() > LOG_FIELD_MAX_LENGTH) {
            trimmed = trimmed.substring(0, LOG_FIELD_MAX_LENGTH);
        }
        return trimmed;
    }

    /**
     * 按列宽截断任意字符串
     *
     * @param value     原始值
     * @param maxLength 目标列的最大长度
     * @param fallback  值为 null/空白时的占位内容
     * @return 截断后的值
     */
    private static String truncate(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    /**
     * 归一化入库的 URL：去查询参数、去结尾斜杠、绝对地址只留 path
     * <p>
     * 归一化放在写入侧而不是统计侧：统计 SQL 一旦用函数包裹 url 列就无法用索引，
     * 且"同一页面的不同写法"会在 GROUP BY 里被拆成多行。
     * 兼容前端可能上报的三种形态：{@code /a/b}、{@code /a/b/}、{@code https://host/a/b}。
     *
     * @param value 上报的 URL
     * @return 归一化后的 URL，始终以 "/" 开头（空值返回 "Empty"）
     */
    static String normalizeUrlForStore(String value) {
        if (value == null || value.isBlank()) {
            return "Empty";
        }
        String trimmed = sanitizeUrlField(value.trim(), "Empty");

        // 绝对地址（location.href 形态）只保留 path 部分
        int schemeIndex = trimmed.indexOf("://");
        if (schemeIndex >= 0) {
            int pathIndex = trimmed.indexOf('/', schemeIndex + 3);
            trimmed = pathIndex >= 0 ? trimmed.substring(pathIndex) : "/";
        }

        if (trimmed.length() > 1 && trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            return "/";
        }
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    /**
     * 按小时格式化（yyyy-MM-dd HH:00）
     *
     * @param date 时间
     * @return 格式化结果，按 JVM 默认时区换算
     */
    private static String formatHour(Date date) {
        return HOUR_FORMAT.format(toLocalDateTime(date));
    }

    /**
     * java.util.Date 转 LocalDateTime，时区与 SimpleDateFormat 默认行为保持一致
     *
     * @param date 时间
     * @return 本地时间
     */
    private static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private final AccessLogMapper accessLogMapper;
    private final AccessLogHourlyStatsMapper accessLogHourlyStatsMapper;
    private final AccessLogHourlyStatsTaskMapper accessLogHourlyStatsTaskMapper;
    private final AccessLogUrlDailyStatsMapper accessLogUrlDailyStatsMapper;
    private final AccessLogUrlDailyStatsTaskMapper accessLogUrlDailyStatsTaskMapper;
    private final IdGenerator idGenerator;

    public AccessService(AccessLogMapper accessLogMapper,
            AccessLogHourlyStatsMapper accessLogHourlyStatsMapper,
            AccessLogHourlyStatsTaskMapper accessLogHourlyStatsTaskMapper,
            AccessLogUrlDailyStatsMapper accessLogUrlDailyStatsMapper,
            AccessLogUrlDailyStatsTaskMapper accessLogUrlDailyStatsTaskMapper) {
        this.accessLogMapper = accessLogMapper;
        this.accessLogHourlyStatsMapper = accessLogHourlyStatsMapper;
        this.accessLogHourlyStatsTaskMapper = accessLogHourlyStatsTaskMapper;
        this.accessLogUrlDailyStatsMapper = accessLogUrlDailyStatsMapper;
        this.accessLogUrlDailyStatsTaskMapper = accessLogUrlDailyStatsTaskMapper;
        this.idGenerator = new IdGenerator(3L);
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

        // Referer 与 URL 一律裁剪掉查询参数
        // 前端曾把 token 拼在 query 上（如 /user/info?token=xxx），原样入库等于把登录凭证写进数据库；
        // 同时限制长度，避免超长值触发 "Data too long for column" 导致整条日志写入失败
        accessLog.setReferer(sanitizeUrlField(request.getHeader("Referer"), "Unknown"));
        // URL 额外做归一化（去结尾斜杠、绝对地址取 path），统计侧才能直接 GROUP BY url
        accessLog.setUrl(normalizeUrlForStore(accessLogDTO.getUrl()));

        // 访问时间
        accessLog.setAccessTime(new Date());

        // IP：来源可能是客户端可伪造的 X-Forwarded-For，长度不可信
        accessLog.setIp(truncate(IpUtil.getIpAddress(request), IP_MAX_LENGTH, "Unknown"));

        // User-Agent 解析
        accessLog.setBrowser(UserAgentUtil.getBrowser(request));
        accessLog.setOs(UserAgentUtil.getOs(request));
        accessLog.setDevice(UserAgentUtil.getDevice(request));

        // 若浏览器/OS/设备任一为Unknown，保存完整User-Agent备查
        if ("Unknown".equals(accessLog.getBrowser()) || "Unknown".equals(accessLog.getOs()) || "Unknown".equals(accessLog.getDevice())) {
            accessLog.setUserAgent(truncate(UserAgentUtil.getUserAgent(request), USER_AGENT_MAX_LENGTH, "Unknown"));
        }

        // 地域：客户端完全可控，超长会触发 "Data too long for column region"
        accessLog.setRegion(truncate(accessLogDTO.getRegion(), REGION_MAX_LENGTH, "Unknown"));

        accessLogMapper.insert(accessLog);
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
        if (diffMillis > MAX_RANGE_MILLIS) {
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
            String day = formatDay(stat.getStatDay());
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
     * <p>
     * 事务注解必须同时加在本方法上：类内自调用不经过 Spring 代理，
     * 只标 {@link #statisticsUrlDailyVisits(Date, Date)} 的话定时任务进来是没有事务的。
     *
     * @return 本次统计写入的URL数量
     */
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
    public long statisticsUrlDailyVisits(Date startDay, Date endDay) {
        // 强制将 startDay 对齐到当天零点，保证 stat_day 标识的唯一性与匹配一致性
        startDay = truncateToDay(startDay);

        // 按URL分组统计区间内的访问量，取访问量最高的前 N 个URL
        // url 在入库时已归一化，这里仍走一次 normalizeUrl 是为了兼容改造前的历史行（带结尾斜杠）
        List<UrlCountDTO> urlCounts = accessLogMapper.countByUrl(startDay, endDay, URL_DAILY_STATS_MAX_ROWS);
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
                Logger.info("URL每日访问量统计：{} 旧数据 {} 条已标记为过期", formatDay(startDay), expiredRows);
            }
        }

        // 3. 生成新 task_id，批量写入新的统计数据
        long newTaskId = idGenerator.nextId();
        if (!urlCountMap.isEmpty()) {
            List<AccessLogUrlDailyStats> batch = new ArrayList<>(urlCountMap.size());
            for (Map.Entry<String, Long> entry : urlCountMap.entrySet()) {
                AccessLogUrlDailyStats stats = new AccessLogUrlDailyStats();
                stats.setId(idGenerator.nextId());
                stats.setUrl(entry.getKey());
                stats.setStatDay(startDay);
                stats.setVisitCount(entry.getValue());
                stats.setTaskId(newTaskId);
                stats.setRecordCode(RecordType.DISPLAY.code());
                batch.add(stats);
            }
            accessLogUrlDailyStatsMapper.insertBatch(batch);
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

        Logger.info("URL每日访问量统计完成：{} 共 {} 个URL task_id {}", formatDay(startDay), urlCountMap.size(), newTaskId);
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
        if (diffMillis > MAX_RANGE_MILLIS) {
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
        if (diffMillis > MAX_RANGE_MILLIS) {
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
            hourCount.put(formatHour(stat.getStatHour()), stat.getVisitCount());
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
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
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
                Logger.info("小时访问量统计：{} 旧数据 {} 条已标记为过期", formatHour(startHour), expiredRows);
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

        Logger.info("每小时访问量统计完成：{} 访问量 {} task_id {}", formatHour(startHour), visitCount, newTaskId);
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
        if (diffMillis > MAX_RANGE_MILLIS) {
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
            String day = formatDay(stat.getStatDay());
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
            hours.add(formatHour(cal.getTime()));
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
            days.add(formatDay(cal.getTime()));
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

}
