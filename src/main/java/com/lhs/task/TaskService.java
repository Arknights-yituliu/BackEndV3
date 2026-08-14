package com.lhs.task;

import com.lhs.service.admin.AccessService;
import com.lhs.service.admin.BackfillHourlyAccessStatsService;
import com.lhs.service.admin.BackfillUrlDailyStatsService;
import com.lhs.service.maa.RecruitTagUploadService;
import com.lhs.service.survey.OperatorCarryRateService;
import com.lhs.service.survey.OperatorDataService;
import com.lhs.service.survey.OperatorProgressionStatisticsService;
import com.lhs.service.material.*;

import com.lhs.service.survey.QuestionnaireService;
import com.lhs.service.user.BindService;
import com.lhs.service.user.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service

public class TaskService {

    private final StageService stageService;
    private final OperatorProgressionStatisticsService operatorProgressionStatisticsService;
    private final RecruitTagUploadService recruitTagUploadService;
    private final OperatorCarryRateService operatorCarryRateService;

    private final QuestionnaireService questionnaireService;

    private final OperatorDataService operatorDataService;

    private final UserService userService;
    private final BindService bindService;

    private final AccessService accessService;

    private final BackfillHourlyAccessStatsService backfillHourlyAccessStatsService;

    private final BackfillUrlDailyStatsService backfillUrlDailyStatsService;

    private final StageDropHourStatisticsService stageDropHourStatisticsService;

    public TaskService(

            StageService stageService,
            OperatorProgressionStatisticsService operatorProgressionStatisticsService,
            RecruitTagUploadService recruitTagUploadService,
            OperatorCarryRateService operatorCarryRateService,
            QuestionnaireService questionnaireService,
            OperatorDataService operatorDataService,
            UserService userService,
            BindService bindService,
            AccessService accessService,
            BackfillHourlyAccessStatsService backfillHourlyAccessStatsService,
            BackfillUrlDailyStatsService backfillUrlDailyStatsService,
            StageDropHourStatisticsService stageDropHourStatisticsService) {
        this.stageDropHourStatisticsService = stageDropHourStatisticsService;   

        this.stageService = stageService;
        this.operatorProgressionStatisticsService = operatorProgressionStatisticsService;
        this.recruitTagUploadService = recruitTagUploadService;
        this.operatorCarryRateService = operatorCarryRateService;
        this.questionnaireService = questionnaireService;
        this.operatorDataService = operatorDataService;
        this.userService = userService;
        this.bindService = bindService;
        this.accessService = accessService;
        this.backfillHourlyAccessStatsService = backfillHourlyAccessStatsService;
        this.backfillUrlDailyStatsService = backfillUrlDailyStatsService;
    }

    // 每天执行一次的任务

     /**
     * 备份用户数据
     */
    @Scheduled(cron = "0 31 4 * * ?")
    public void backupUserInfo() {
        userService.backupUserInfo();
        bindService.backupUserExternalAccountBinding();
    }

    /**
     * 备份问卷数据
     */
    @Scheduled(cron = "0 32 4 * * ?")
    public void backupQuestionnaireResult() {
        questionnaireService.backup();
    }

    /**
     * 备份干员练度数据
     */
    @Scheduled(cron = "0 33 4 * * ?")
    public void backupOperatorProgressionData() {
        operatorDataService.backupOperatorProgressionData();
    }

    // 按小时执行的任务


    /**
     * 统计上一个小时的关卡掉率数据
     */
    @Scheduled(cron = "0 15 * * * ?")
    public void statisticsLastHour() {
        stageDropHourStatisticsService.statisticsLastHour();
    }

    /**
     * 统计上一个完整小时的访问量并写入预聚合表
     * 每小时执行 3 次（每 20 分钟一次），重复执行幂等，供 /access-log/hourly-total 接口直接读取
     */
    @Scheduled(cron = "0 0/20 * * * ?")
    public void statisticsLastHourAccessVisits() {
        accessService.statisticsLastHour();
    }

    /**
     * 删除 access_log_hourly_stats 表中已过期的统计数据
     * 每天凌晨执行一次，清理重跑后遗留的 EXPIRE 记录
     */
    @Scheduled(cron = "0 20 4 * * ?")
    public void deleteExpireAccessHourlyStats() {
        accessService.deleteExpireHourlyStats();
    }

    /**
     * 统计昨天的每个URL访问量并写入预聚合表
     * 一天执行 3 次（0:30、8:30、16:30），重复执行幂等，供 /access-log/daily 接口直接读取
     */
    @Scheduled(cron = "0 0 0/8 * * ?")
    public void statisticsYesterdayUrlDailyVisits() {
        accessService.statisticsYesterdayUrlDailyVisits();
    }

    /**
     * 统计今天的每个URL访问量并写入预聚合表
     * 每 30 分钟执行一次，反复重跑刷新今天的最新数据，供 /access-log/daily 接口直接读取
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void statisticsTodayUrlDailyVisits() {
        accessService.statisticsTodayUrlDailyVisits();
    }

    /**
     * 删除 access_log_url_daily_stats 表中已过期的统计数据
     * 每天凌晨执行一次，清理重跑后遗留的 EXPIRE 记录
     */
    @Scheduled(cron = "0 25 4 * * ?")
    public void deleteExpireAccessUrlDailyStats() {
        accessService.deleteExpireUrlDailyStats();
    }

    /**
     * 公招统计
     */
    @Scheduled(cron = "0 1 * * * ?")
    public void recruitStatistics() {
        recruitTagUploadService.recruitStatistics();
    }

    /**
     * 拉取企鹅的关卡表
     */
    @Scheduled(cron = "0 4 * * * ?")
    public void pullPenguinStagesApi() {
        stageService.getPenguinStagesDropData();
    }

    /**
     * 统计干员练度数据
     */
    @Scheduled(cron = "0 10 0/6 * * ?")
    public void statisticsProgressionOperatorData() {
        operatorProgressionStatisticsService.statisticsOperatorProgressionDataV2(false);
    }

    /**
     * 归档干员练度数据
     */
    @Scheduled(cron = "0 12 0/6 * * ?")
    public void archivedOperatorProgressionResult() {
        operatorProgressionStatisticsService.archivedOperatorProgressionResult();
    }

    /**
     * 统计昨天的干员携带率数据
     */
    @Scheduled(cron = "0 0 0/6 * * ?")
    public void statisticsYesterdayOperatorCarryRateTask() {
        operatorCarryRateService.statisticsYesterdayOperatorCarryRate();
    }



    // 按分钟执行的任务


    /**
     * 保存企鹅物流数据到本地
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void savePenguinData() {
        stageService.savePenguinData();
    }

    /**
     * 统计今天的干员携带率数据
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void statisticsTodayOperatorCarryRateTask() {
        operatorCarryRateService.statisticsTodayOperatorCarryRate();
    }

    /**
     * 删除过期的干员携带率数据
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void deleteOperatorCarryRateExpireData() {
        operatorCarryRateService.deleteExpireData();
    }

    

   

}
