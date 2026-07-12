package com.lhs.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.UserAgentUtil;
import com.lhs.entity.dto.AccessLogDTO;
import com.lhs.entity.po.admin.AccessLog;
import com.lhs.entity.po.admin.PageViewStatistics;
import com.lhs.entity.vo.dev.UrlPeriodDataVO;
import com.lhs.entity.vo.dev.UrlTotalVisitVO;
import com.lhs.entity.vo.dev.UrlVisitGroupVO;
import com.lhs.mapper.admin.AccessLogMapper;
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
    private final IdGenerator idGenerator;
    private final RedisTemplate<String, Object> redisTemplate;

    /** Redis中记录上次迁移日期的键 */
    private static final String MIGRATE_LAST_DATE_KEY = "migrate:lastSyncedDate";

    /** 迁移起始日期（前一天），首次执行时迁移 2026-07-13 */
    private static final String MIGRATE_START_DATE = "2026-07-12";

    public AccessService(AccessLogMapper accessLogMapper, PageVisitsMapper pageVisitsMapper,
            RedisTemplate<String, Object> redisTemplate) {
        this.accessLogMapper = accessLogMapper;
        this.pageVisitsMapper = pageVisitsMapper;
        this.idGenerator = new IdGenerator(1L);
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

        // URL
        String url = accessLogDTO.getUrl();
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
     * 分批查询（每批10万条），Java代码聚合，零值补齐，返回Top 30 URL
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

        // url → (day → count)
        Map<String, Map<String, Long>> urlDayCount = new HashMap<>();

        long offset = 0;
        while (true) {
            List<AccessLog> batch = accessLogMapper.selectList(
                    new LambdaQueryWrapper<AccessLog>()
                            .select(AccessLog::getUrl, AccessLog::getAccessTime)
                            .ge(AccessLog::getAccessTime, startTime)
                            .lt(AccessLog::getAccessTime, endTime)
                            .last("ORDER BY id LIMIT " + offset + "," + BATCH_SIZE)
            );

            if (batch.isEmpty()) {
                break;
            }

            for (AccessLog log : batch) {
                String url = normalizeUrl(log.getUrl());
                String day = DAY_FORMAT.format(log.getAccessTime());
                urlDayCount
                        .computeIfAbsent(url, k -> new HashMap<>())
                        .merge(day, 1L, Long::sum);
            }

            offset += BATCH_SIZE;
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
     * 统计指定时间范围内每个URL的总访问次数
     * 分批查询（每批10万条），Java代码聚合，返回全部URL
     * Top 15 单独列出，其余合并为"其他"，第一项固定为"访问总和"
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

        Map<String, Long> urlCount = new HashMap<>();

        long offset = 0;
        while (true) {
            List<AccessLog> batch = accessLogMapper.selectList(
                    new LambdaQueryWrapper<AccessLog>()
                            .select(AccessLog::getUrl)
                            .ge(AccessLog::getAccessTime, startTime)
                            .lt(AccessLog::getAccessTime, endTime)
                            .last("ORDER BY id LIMIT " + offset + "," + BATCH_SIZE)
            );

            if (batch.isEmpty()) {
                break;
            }

            for (AccessLog log : batch) {
                String url = normalizeUrl(log.getUrl());
                urlCount.merge(url, 1L, Long::sum);
            }

            offset += BATCH_SIZE;
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
     * 分批查询（每批10万条），Java代码聚合，零值补齐
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

        Map<String, Long> hourCount = new HashMap<>();

        long offset = 0;
        while (true) {
            List<AccessLog> batch = accessLogMapper.selectList(
                    new LambdaQueryWrapper<AccessLog>()
                            .select(AccessLog::getAccessTime)
                            .ge(AccessLog::getAccessTime, startTime)
                            .lt(AccessLog::getAccessTime, endTime)
                            .last("ORDER BY id LIMIT " + offset + "," + BATCH_SIZE)
            );

            if (batch.isEmpty()) {
                break;
            }

            for (AccessLog log : batch) {
                String hour = HOUR_FORMAT.format(log.getAccessTime());
                hourCount.merge(hour, 1L, Long::sum);
            }

            offset += BATCH_SIZE;
        }

        List<String> allHours = generateAllHours(startTime, endTime);
        List<UrlPeriodDataVO> result = new ArrayList<>(allHours.size());
        for (String hour : allHours) {
            result.add(new UrlPeriodDataVO(hour, hourCount.getOrDefault(hour, 0L)));
        }

        return result;
    }

    /**
     * 统计指定时间范围内每日的总访问量（所有URL聚合）
     * 分批查询（每批10万条），Java代码聚合，零值补齐
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

        Map<String, Long> dayCount = new HashMap<>();

        long offset = 0;
        while (true) {
            List<AccessLog> batch = accessLogMapper.selectList(
                    new LambdaQueryWrapper<AccessLog>()
                            .select(AccessLog::getAccessTime)
                            .ge(AccessLog::getAccessTime, startTime)
                            .lt(AccessLog::getAccessTime, endTime)
                            .last("ORDER BY id LIMIT " + offset + "," + BATCH_SIZE)
            );

            if (batch.isEmpty()) {
                break;
            }

            for (AccessLog log : batch) {
                String day = DAY_FORMAT.format(log.getAccessTime());
                dayCount.merge(day, 1L, Long::sum);
            }

            offset += BATCH_SIZE;
        }

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
     * 每次执行迁移一天的旧数据，基于 Redis 记录进度，定时任务每分钟调用一次
     *
     * @return 本次迁移的记录数，0 表示没有待迁移的数据或已全部完成
     */
    public long migrateOldVisits() {
        // 从Redis获取上次迁移到的日期，首次执行默认为MIGRATE_START_DATE
        Object lastDateObj = redisTemplate.opsForValue().get(MIGRATE_LAST_DATE_KEY);
        String lastDateStr = lastDateObj != null ? lastDateObj.toString() : MIGRATE_START_DATE;

        // 计算下一个要迁移的日期
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(DAY_FORMAT.parse(lastDateStr));
        } catch (Exception e) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }
        cal.add(Calendar.DAY_OF_YEAR, 1);

        // 不超过今天（今天数据还未完整，不迁移）
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        if (!cal.before(today)) {
            return 0; // 没有更多待迁移的日期
        }

        Date startOfDay = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Date endOfDay = cal.getTime();
        String syncingDate = DAY_FORMAT.format(startOfDay);

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
            batch.clear();
        }

        // 更新Redis中的进度到本次迁移的日期
        redisTemplate.opsForValue().set(MIGRATE_LAST_DATE_KEY, syncingDate);

        return totalMigrated;
    }

}
