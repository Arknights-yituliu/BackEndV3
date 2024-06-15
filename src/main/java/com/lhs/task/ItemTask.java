package com.lhs.task;

import com.lhs.service.maa.StageDropUploadService;
import com.lhs.service.util.OSSService;
import com.lhs.service.item.*;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service

public class ItemTask {


    private final ItemService itemService;

    private final StoreService storeService;
    private final StageResultService stageResultService;
    private final OSSService ossService;
    private final StageService stageService;

    private final StageDropUploadService stageDropUploadService;

    public ItemTask(ItemService itemService,
                    StoreService storeService,
                    StageResultService stageResultService,
                    OSSService ossService,
                    StageService stageService,
                    StageDropUploadService stageDropUploadService) {
        this.itemService = itemService;
        this.storeService = storeService;
        this.stageResultService = stageResultService;
        this.ossService = ossService;
        this.stageService = stageService;
        this.stageDropUploadService = stageDropUploadService;
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
    @Scheduled(cron = "0 0 0/3 * * ? ")
    public void pullPenguinStagesApi() {
        stageService.getPenguinStagesDropData();
    }

    /**
     * 根据关卡配置更新关卡计算结果
     */
    @Scheduled(cron = "0 0/19 * * * ?")
    public void updateStageResult() {
        stageResultService.updateStageResultByTaskConfig();
    }

//    @Scheduled(cron = "0 0/17 * * * ?")
//    public void updateStageResultApi() {
//        StageParamDTO stageParamDTO = new StageParamDTO();
//        stageParamDTO.setExpCoefficient(0.625);
//        stageParamDTO.setSampleSize(300);
//        stageResultService.getT3RecommendedStageV3(stageParamDTO.getVersion());
//    }


//    @Scheduled(cron = "0/10 * * * * ?")
    @Scheduled(cron = "0 0/5 * * * ?")
    public void exportMAAStageDropData() {
       stageDropUploadService.exportData();
    }


    /**
     * 备份关卡计算结果
     */
    @Scheduled(cron = "0 5/10 * * * ?")
    public void backupStageResult() {
        stageResultService.backupStageResult();
    }

    /**
     * 更新常驻商店性价比
     */
    @Scheduled(cron = "0 0 0/1 * * ?")
    public void updateStorePerm() {
        storeService.updateStorePerm();
    }

}
