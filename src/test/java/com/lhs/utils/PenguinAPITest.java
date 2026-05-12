package com.lhs.utils;

import com.lhs.service.material.StageCalService;
import com.lhs.service.material.StageResultService;
import com.lhs.service.material.StageService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PenguinAPITest {

    @Resource
    private StageService stageService;

    @Resource
    private StageCalService stageCalService;

    @Test
    void getPenguinData(){
        stageService.getPenguinStagesDropData();
    }

    @Test
    void getPenguinData2(){
        stageService.savePenguinData();
    }

    @Test
    void update(){
        stageCalService.updateStageResultByTaskConfig();
    }

}
