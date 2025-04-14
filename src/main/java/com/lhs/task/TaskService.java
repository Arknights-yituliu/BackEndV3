package com.lhs.task;

import com.lhs.common.enums.QuestionnaireType;
import com.lhs.common.enums.RecordType;
import com.lhs.common.enums.TimeGranularity;
import com.lhs.service.maa.RecruitTagUploadService;
import com.lhs.service.rogue.RogueSeedService;
import com.lhs.service.survey.OperatorProgressionStatisticsService;
import com.lhs.service.material.*;

import com.lhs.service.survey.QuestionnaireService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service

public class TaskService {


    private final StoreService storeService;
    private final StageCalService stageCalService;
    private final StageService stageService;
    private final OperatorProgressionStatisticsService operatorProgressionStatisticsService;

    private final RogueSeedService rogueSeedService;
    private final RecruitTagUploadService recruitTagUploadService;

    private final QuestionnaireService questionnaireService;

    public TaskService(StoreService storeService,
                       StageCalService stageCalService,
                       StageService stageService,
                       OperatorProgressionStatisticsService operatorProgressionStatisticsService,
                       RogueSeedService rogueSeedService,
                       RecruitTagUploadService recruitTagUploadService,
                       QuestionnaireService questionnaireService) {
        this.storeService = storeService;
        this.stageCalService = stageCalService;
        this.stageService = stageService;
        this.operatorProgressionStatisticsService = operatorProgressionStatisticsService;
        this.rogueSeedService = rogueSeedService;
        this.recruitTagUploadService = recruitTagUploadService;
        this.questionnaireService = questionnaireService;
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

    @Scheduled(cron = "0 0 1 * * ?")
    public void deleteOperatorProgressionResultExpireData() {
        operatorProgressionStatisticsService.deleteExpireData();
    }


    @Scheduled(cron = "0 0/1 * * * ?")
    public void statisticsQuestionnaireResult() {
        long currentTimeStamp = System.currentTimeMillis();
        questionnaireService.statisticsQuestionnaireResult(null, QuestionnaireType.MAIN_AND_SIDE_STORY_FOR_NEW_GAME, TimeGranularity.FROM_INCEPTION_TO_PRESENT);
        questionnaireService.statisticsQuestionnaireResult(null, QuestionnaireType.CONTINGENCY_CONTRACT_Mode_FOR_NEW_GAME, TimeGranularity.FROM_INCEPTION_TO_PRESENT);
        questionnaireService.statisticsQuestionnaireResult(null, QuestionnaireType.INTEGRATED_STRATEGIES_FOR_NEW_GAME, TimeGranularity.FROM_INCEPTION_TO_PRESENT);

        Date pastWeek = new Date(currentTimeStamp - 60 * 60 * 24 * 7 * 1000L);
        questionnaireService.statisticsQuestionnaireResult(pastWeek, QuestionnaireType.MAIN_AND_SIDE_STORY_FOR_NEW_GAME, TimeGranularity.THE_PAST_WEEK);
        questionnaireService.statisticsQuestionnaireResult(pastWeek, QuestionnaireType.CONTINGENCY_CONTRACT_Mode_FOR_NEW_GAME, TimeGranularity.THE_PAST_WEEK);
        questionnaireService.statisticsQuestionnaireResult(pastWeek, QuestionnaireType.INTEGRATED_STRATEGIES_FOR_NEW_GAME, TimeGranularity.THE_PAST_WEEK);

        Date pastTwoWeek = new Date(currentTimeStamp - 60 * 60 * 24 * 14 * 1000L);
        questionnaireService.statisticsQuestionnaireResult(pastTwoWeek, QuestionnaireType.MAIN_AND_SIDE_STORY_FOR_NEW_GAME, TimeGranularity.THE_PAST_TWO_WEEK);
        questionnaireService.statisticsQuestionnaireResult(pastTwoWeek, QuestionnaireType.CONTINGENCY_CONTRACT_Mode_FOR_NEW_GAME, TimeGranularity.THE_PAST_TWO_WEEK);
        questionnaireService.statisticsQuestionnaireResult(pastTwoWeek, QuestionnaireType.INTEGRATED_STRATEGIES_FOR_NEW_GAME, TimeGranularity.THE_PAST_TWO_WEEK);
    }

    @Scheduled(cron = "0 23 * * * ?")
    public void archivedOperatorCarryRateResult() {
        questionnaireService.archivedQuestionnaireStatisticsResult(QuestionnaireType.MAIN_AND_SIDE_STORY_FOR_NEW_GAME);
        questionnaireService.archivedQuestionnaireStatisticsResult(QuestionnaireType.CONTINGENCY_CONTRACT_Mode_FOR_NEW_GAME);
        questionnaireService.archivedQuestionnaireStatisticsResult(QuestionnaireType.INTEGRATED_STRATEGIES_FOR_NEW_GAME);
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void deleteExpireData() {
        questionnaireService.deleteExpireData();
    }

}
