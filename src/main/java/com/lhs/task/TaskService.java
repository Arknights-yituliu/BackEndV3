package com.lhs.task;

import com.lhs.service.maa.RecruitTagUploadService;
import com.lhs.service.survey.OperatorCarryRateService;
import com.lhs.service.survey.OperatorProgressionStatisticsService;
import com.lhs.service.material.*;

import com.lhs.service.user.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service

public class TaskService {


    private final StageCalService stageCalService;
    private final StageService stageService;
    private final OperatorProgressionStatisticsService operatorProgressionStatisticsService;
    private final RecruitTagUploadService recruitTagUploadService;
    private final OperatorCarryRateService operatorCarryRateService;

    private final StageDropService stageDropService;

    private final UserService userService;

    public TaskService(
            StageCalService stageCalService,
            StageService stageService,
            OperatorProgressionStatisticsService operatorProgressionStatisticsService,
            RecruitTagUploadService recruitTagUploadService,
            OperatorCarryRateService operatorCarryRateService, StageDropService stageDropService, UserService userService) {
        this.stageCalService = stageCalService;
        this.stageService = stageService;
        this.operatorProgressionStatisticsService = operatorProgressionStatisticsService;
        this.recruitTagUploadService = recruitTagUploadService;
        this.operatorCarryRateService = operatorCarryRateService;
        this.stageDropService = stageDropService;
        this.userService = userService;
    }


    /**
     * 公招统计
     */
    @Scheduled(cron = "0 0 0/3 * * ?")
    public void recruitStatistics() {
        recruitTagUploadService.recruitStatistics();
    }


    /**
     * 保存企鹅物流数据到本地
     */
    @Scheduled(cron = "0 0/20 * * * ?")
    public void savePenguinData() {
        stageService.savePenguinData();
    }

    /**
     * 拉取企鹅的关卡表
     */
    @Scheduled(cron = "0 0 0/3 * * ?")
    public void pullPenguinStagesApi() {
        stageService.getPenguinStagesDropData();
    }

    /**
     * 根据关卡配置更新关卡计算结果
     */
    @Scheduled(cron = "0 0/19 * * * ?")
    public void updateStageResult() {
        stageCalService.updateStageResultByTaskConfig();
    }

    /**
     * 统计干员练度数据
     */
    @Scheduled(cron = "0 0 0/1 * * ?")
    public void statisticsProgressionOperatorData() {
        operatorProgressionStatisticsService.statisticsOperatorProgressionDataV2();
    }

    @Scheduled(cron = "0 23 * * * ?")
    public void archivedOperatorProgressionResult() {
        operatorProgressionStatisticsService.archivedOperatorProgressionResult();
    }

    @Scheduled(cron = "0 0 0/1 * * ?")
    public void deleteOperatorProgressionResultExpireData() {
        operatorProgressionStatisticsService.deleteExpireData();
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void statisticsTodayOperatorCarryRateTask() {
        operatorCarryRateService.statisticsTodayOperatorCarryRate();
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    public void deleteOperatorCarryRateExpireData() {
        operatorCarryRateService.deleteExpireData();
    }

    @Scheduled(cron = "0 0 0/6 * * ?")
    public void statisticsYesterdayOperatorCarryRateTask() {
        operatorCarryRateService.statisticsYesterdayOperatorCarryRate();
    }


    @Scheduled(cron = "0 0 0/19 * * ?")
    public void stageDropHourlyStatistics() {
        stageDropService.stageDropHourlyStatistics();
    }


    @Scheduled(cron = "0 11 * * * ?")
    public void backupUserInfo() {
        userService.backupUserInfo();
        userService.backupUserExternalAccountBinding();
    }





}
