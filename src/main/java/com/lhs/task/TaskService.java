package com.lhs.task;

import com.lhs.service.maa.RecruitTagUploadService;
import com.lhs.service.survey.OperatorCarryRateService;
import com.lhs.service.survey.OperatorDataService;
import com.lhs.service.survey.OperatorProgressionStatisticsService;
import com.lhs.service.material.*;

import com.lhs.service.survey.QuestionnaireService;
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

    private final StageDropHourStatisticsService stageDropHourStatisticsService;

    public TaskService(

            StageService stageService,
            OperatorProgressionStatisticsService operatorProgressionStatisticsService,
            RecruitTagUploadService recruitTagUploadService,
            OperatorCarryRateService operatorCarryRateService,
            QuestionnaireService questionnaireService,
            OperatorDataService operatorDataService,
            UserService userService,
            StageDropHourStatisticsService stageDropHourStatisticsService) {
        this.stageDropHourStatisticsService = stageDropHourStatisticsService;   

        this.stageService = stageService;
        this.operatorProgressionStatisticsService = operatorProgressionStatisticsService;
        this.recruitTagUploadService = recruitTagUploadService;
        this.operatorCarryRateService = operatorCarryRateService;
        this.questionnaireService = questionnaireService;
        this.operatorDataService = operatorDataService;
        this.userService = userService;
    }

    // 每天执行一次的任务

     /**
     * 备份用户数据
     */
    @Scheduled(cron = "0 31 4 * * ?")
    public void backupUserInfo() {
        userService.backupUserInfo();
        userService.backupUserExternalAccountBinding();
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
    @Scheduled(cron = "0 0/20 * * * ?")
    public void savePenguinData() {
        stageService.savePenguinData();
    }

    /**
     * 统计今天的干员携带率数据
     */
    @Scheduled(cron = "0 0/1 * * * ?")
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
