package com.lhs.utils;

import com.lhs.entity.dto.hypergryph.GameDataFormatFilePath;
import com.lhs.service.util.ArknightsGameDataService;
import com.lhs.service.util.ArknightsGameDataV2Service;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ArknightsBaseDataTest {

    @Resource
    private ArknightsGameDataService arknightsGameDataService;

    @Resource
    private ArknightsGameDataV2Service arknightsGameDataV2Service;

    @Test
    public void getArkGameDataSimple() {
        GameDataFormatFilePath gameDataFormatFilePath = new GameDataFormatFilePath();
        gameDataFormatFilePath.setArknightsGameResourcePath("C:/VCProject/ArknightsGameResource/gamedata/");
        gameDataFormatFilePath.setArknightsGameDataPath("C:/VCProject/ArknightsGameData/zh_CN/gamedata/");
        gameDataFormatFilePath.setImageOutputPath("C:/VCProject/ak-resources/image/avatar/");
        gameDataFormatFilePath.setJsonOutputPath("C:/VCProject/frontend-v2-plus/");
        arknightsGameDataV2Service.getOperatorInfoSimpleTableV2(gameDataFormatFilePath);
//


//        arknightsGameDataService.getBuildingTable();
    }



    @Test
    public void getOperatorInfoSimpleTable() {
        GameDataFormatFilePath gameDataFormatFilePath = new GameDataFormatFilePath();
        gameDataFormatFilePath.setArknightsGameResourcePath("C:/WebStormProject/ArknightsGameResource/");
        gameDataFormatFilePath.setArknightsGameDataPath("C:/WebStormProject/ArknightsGameData/zh_CN/gamedata/");
        gameDataFormatFilePath.setImageOutputPath("C:/WebStormProject/ak-resources/image/avatar/");
        gameDataFormatFilePath.setJsonOutputPath("C:/WebStormProject/frontend-v2-plus/");
        arknightsGameDataService.getOperatorInfoSimpleTable(gameDataFormatFilePath);
        arknightsGameDataService.getBuildingTable(gameDataFormatFilePath);
    }


    @Test
    public void getOperatorInfoSimpleTableByGameResource() {
        GameDataFormatFilePath gameDataFormatFilePath = new GameDataFormatFilePath();
        gameDataFormatFilePath.setArknightsGameResourcePath("C:/VCProject/ArknightsGameResource/gamedata/");
        gameDataFormatFilePath.setArknightsGameDataPath("C:/VCProject/ArknightsGameData/zh_CN/gamedata/");
        gameDataFormatFilePath.setImageOutputPath("C:/VCProject/ak-resources/image/avatar/");
        gameDataFormatFilePath.setJsonOutputPath("C:/VCProject/frontend-v2-plus/");
        arknightsGameDataService.getOperatorInfoSimpleTableByGameResource(gameDataFormatFilePath);
        arknightsGameDataService.getBuildingTableByGameResource(gameDataFormatFilePath);
    }


    @Test
    public void getAvatar() {
        GameDataFormatFilePath gameDataFormatFilePath = new GameDataFormatFilePath();
        gameDataFormatFilePath.setArknightsGameResourcePath("C:/VCProject/ArknightsGameResource/gamedata/");
        gameDataFormatFilePath.setArknightsGameResourceAvatarPath("C:/VCProject/ArknightsGameResource/avatar/");
        gameDataFormatFilePath.setArknightsGameDataPath("C:/VCProject/ArknightsGameData/zh_CN/gamedata/");
        gameDataFormatFilePath.setImageOutputPath("C:/VCProject/ak-resources/image/avatar/");
        arknightsGameDataService.getAvatar(gameDataFormatFilePath);
    }

}
