package com.lhs.task;

import com.lhs.common.enums.QuestionnaireType;
import com.lhs.common.enums.TimeGranularity;
import com.lhs.common.enums.UnitTime;
import com.lhs.common.util.TimeUtil;
import com.lhs.service.maa.RecruitTagUploadService;
import com.lhs.service.rogue.RogueSeedService;
import com.lhs.service.survey.OperatorCarryRateService;
import com.lhs.service.survey.OperatorProgressionStatisticsService;
import com.lhs.service.material.*;

import com.lhs.service.survey.QuestionnaireService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service

public class TaskService {


    private final StoreService storeService;
    private final StageCalService stageCalService;
    private final StageService stageService;
    private final OperatorProgressionStatisticsService operatorProgressionStatisticsService;

    private final RogueSeedService rogueSeedService;
    private final RecruitTagUploadService recruitTagUploadService;

    private final QuestionnaireService questionnaireService;

    private final OperatorCarryRateService operatorCarryRateService;

    public TaskService(StoreService storeService,
                       StageCalService stageCalService,
                       StageService stageService,
                       OperatorProgressionStatisticsService operatorProgressionStatisticsService,
                       RogueSeedService rogueSeedService,
                       RecruitTagUploadService recruitTagUploadService,
                       QuestionnaireService questionnaireService, OperatorCarryRateService operatorCarryRateService) {
        this.storeService = storeService;
        this.stageCalService = stageCalService;
        this.stageService = stageService;
        this.operatorProgressionStatisticsService = operatorProgressionStatisticsService;
        this.rogueSeedService = rogueSeedService;
        this.recruitTagUploadService = recruitTagUploadService;
        this.questionnaireService = questionnaireService;
        this.operatorCarryRateService = operatorCarryRateService;
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
    @Scheduled(cron = "0 5 0/1 * * ?")
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



}
