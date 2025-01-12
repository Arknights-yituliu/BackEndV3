package com.lhs.task;

import com.lhs.service.rogue.RogueSeedService;
import com.lhs.service.survey.OperatorStatisticsService;
import com.lhs.service.material.*;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service

public class TaskService {


    private final StoreService storeService;
    private final StageCalService stageCalService;
    private final StageService stageService;
    private final OperatorStatisticsService operatorStatisticsService;

    private final RogueSeedService rogueSeedService;

    public TaskService(StoreService storeService,
                       StageCalService stageCalService,
                       StageService stageService,
                       OperatorStatisticsService operatorStatisticsService,
                       RogueSeedService rogueSeedService) {
        this.storeService = storeService;
        this.stageCalService = stageCalService;
        this.stageService = stageService;
        this.operatorStatisticsService = operatorStatisticsService;
        this.rogueSeedService = rogueSeedService;
    }


    /**
     * 保存企鹅物流数据到本地
     */
    @Scheduled(cron = "0 0/10 * * * ?")
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
     * 更新常驻商店性价比
     */
//    @Scheduled(cron = "0 0 0/1 * * ?")
    public void updateStorePerm() {
        storeService.updateStorePerm();
    }


    /**
     * 统计干员练度数据
     */
    @Scheduled(cron = "0 0 0/1 * * ? ")
    public void operatorStatistics(){
        operatorStatisticsService.statisticsOperatorData();
    }


    @Scheduled(cron = "0 0/10 * * * ?")
    public void rogueSeedRatingStatistics(){
        rogueSeedService.ratingStatistics();
    }


}
