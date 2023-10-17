package com.lhs.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.*;
import com.lhs.entity.dto.item.StageParamDTO;
import com.lhs.entity.po.item.Item;
import com.lhs.entity.po.item.Stage;

import com.lhs.entity.vo.item.RecommendedStageVO;

import com.lhs.entity.vo.item.StageResultVOV2;
import com.lhs.mapper.item.StageMapper;
import com.lhs.service.dev.OSSService;
import com.lhs.service.item.*;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service

public class ItemTask {


    private final ItemService itemService;

    private final StoreService storeService;
    private final StageResultService stageResultService;
    private final OSSService ossService;
    private final StageService stageService;

    public ItemTask(ItemService itemService, StoreService storeService, StageResultService stageResultService, OSSService ossService, StageService stageService) {
        this.itemService = itemService;
        this.storeService = storeService;
        this.stageResultService = stageResultService;
        this.ossService = ossService;
        this.stageService = stageService;
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
    public void pullPenguinStagesApi(){
        stageService.pullPenguinStagesApi();
    }

    /**
     * 根据关卡配置更新关卡计算结果
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void updateStageResult() {
       stageResultService.updateStageResultByTaskConfig();
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
    public void updateStorePerm(){
        storeService.updateStorePerm();
    }

}
