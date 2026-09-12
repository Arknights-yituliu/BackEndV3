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
    public void getOperatorInfoSimpleTableByGameResource() {
        GameDataFormatFilePath gameDataFormatFilePath = new GameDataFormatFilePath();
        gameDataFormatFilePath.setArknightsGameResourcePath("C:/dev_projects/ArknightsGameResource/gamedata/");
        gameDataFormatFilePath.setArknightsGameDataPath("C:/dev_projects/ArknightsGameData/zh_CN/gamedata/");
        gameDataFormatFilePath.setImageOutputPath("C:/dev_projects/ak-resources/image/avatar/");
        gameDataFormatFilePath.setJsonOutputPath("C:/dev_projects/frontend-v2-plus/");
        gameDataFormatFilePath.setArknightsGameResourcePath("C:/dev_projects/ArknightsGameResource/gamedata/");
        gameDataFormatFilePath.setArknightsGameDataPath("C:/dev_projects/ArknightsGameData/zh_CN/gamedata/");
        gameDataFormatFilePath.setImageOutputPath("C:/dev_projects/ak-resources/image/avatar/");
        gameDataFormatFilePath.setJsonOutputPath("C:/dev_projects/frontend-v2-plus/");
        arknightsGameDataV2Service.getOperatorInfoSimpleTableV2(gameDataFormatFilePath);

        arknightsGameDataService.getBuildingTableByGameResourceByTorappu(gameDataFormatFilePath);
    }


    @Test
    public void getAvatar() {
        GameDataFormatFilePath gameDataFormatFilePath = new GameDataFormatFilePath();
        gameDataFormatFilePath.setArknightsGameResourcePath("C:/dev_projects/ArknightsGameResource/gamedata/");
        gameDataFormatFilePath.setArknightsGameResourceAvatarPath("C:/dev_projects/ArknightsGameResource/avatar/");
        gameDataFormatFilePath.setArknightsGameDataPath("C:/dev_projects/ArknightsGameData/zh_CN/gamedata/");
        gameDataFormatFilePath.setImageOutputPath("C:/dev_projects/ak-resources/image/original/avatar/");
        arknightsGameDataService.getAvatar(gameDataFormatFilePath);
    }

}
