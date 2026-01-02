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


    private final StageCalService stageCalService;
    private final StageService stageService;
    private final OperatorProgressionStatisticsService operatorProgressionStatisticsService;
    private final RecruitTagUploadService recruitTagUploadService;
    private final OperatorCarryRateService operatorCarryRateService;

    private final QuestionnaireService questionnaireService;

    private final StageDropService stageDropService;

    private final OperatorDataService operatorDataService;

    private final UserService userService;

    public TaskService(
            StageCalService stageCalService,
            StageService stageService,
            OperatorProgressionStatisticsService operatorProgressionStatisticsService,
            RecruitTagUploadService recruitTagUploadService,
            OperatorCarryRateService operatorCarryRateService,
            QuestionnaireService questionnaireService, StageDropService stageDropService,
            OperatorDataService operatorDataService,
            UserService userService) {
        this.stageCalService = stageCalService;
        this.stageService = stageService;
        this.operatorProgressionStatisticsService = operatorProgressionStatisticsService;
        this.recruitTagUploadService = recruitTagUploadService;
        this.operatorCarryRateService = operatorCarryRateService;
        this.questionnaireService = questionnaireService;
        this.stageDropService = stageDropService;
        this.operatorDataService = operatorDataService;
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
    @Scheduled(cron = "0 0 */3 * * ?")
    public void statisticsProgressionOperatorData() {
        operatorProgressionStatisticsService.statisticsOperatorProgressionDataV2(false);
    }

     @Scheduled(cron = "0 0 */3 * * ?")
    public void archivedOperatorProgressionResult() {
        operatorProgressionStatisticsService.archivedOperatorProgressionResult();
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


    /**
     * 每小时的19、39、59分执行一次
     */
    @Scheduled(cron = "0 19,39,59 * * * ?")
    public void stageDropHourlyStatistics() {
        stageDropService.stageDropHourlyStatistics();
    }

    /**
     * 备份用户数据
     */
    @Scheduled(cron = "0 21 * * * ?")
    public void backupUserInfo() {
        userService.backupUserInfo();
        userService.backupUserExternalAccountBinding();
    }

    @Scheduled(cron = "0 26 * * * ?")
    public void backupQuestionnaireResult() {
        questionnaireService.backup();
    }

    @Scheduled(cron = "0 54 * * * ?")
    public void backupOperatorProgressionData() {
        operatorDataService.backupOperatorProgressionData();
    }


}
