package com.lhs.utils;

import com.lhs.entity.dto.hypergryph.FilePath;
import com.lhs.service.util.ArknightsGameDataService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ArknightsBaseDataTest {

    @Resource
    private ArknightsGameDataService arknightsGameDataService;

    @Test
    public void getArkGameDataSimple() {


//


//        arknightsGameDataService.getBuildingTable();
    }



    @Test
    public void getOperatorInfoSimpleTable() {
        FilePath filePath = new FilePath();
        filePath.setArknightsGameResourcePath("C:/WebStormProject/ArknightsGameResource/");
        filePath.setArknightsGameDataPath("C:/WebStormProject/ArknightsGameData/zh_CN/gamedata/");
        filePath.setImageOutputPath("C:/WebStormProject/ak-resources/image/avatar/");
        filePath.setJsonOutputPath("C:/WebStormProject/frontend-v2-plus/");
        arknightsGameDataService.getOperatorInfoSimpleTable(filePath);
        arknightsGameDataService.getBuildingTable(filePath);
    }


    @Test
    public void getOperatorInfoSimpleTableByGameResource() {
        FilePath filePath = new FilePath();
        filePath.setArknightsGameResourcePath("C:/WebStormProject/ArknightsGameResource/gamedata/");
        filePath.setArknightsGameDataPath("C:/WebStormProject/ArknightsGameData/zh_CN/gamedata/");
        filePath.setImageOutputPath("C:/WebStormProject/ak-resources/image/avatar/");
        filePath.setJsonOutputPath("C:/WebStormProject/frontend-v2-plus/");
        arknightsGameDataService.getOperatorInfoSimpleTableByGameResource(filePath);
        arknightsGameDataService.getBuildingTableByGameResource(filePath);
    }


    @Test
    public void getAvatar() {
        FilePath filePath = new FilePath();
        filePath.setArknightsGameResourcePath("C:/WebStormProject/ArknightsGameResource/gamedata/");
        filePath.setArknightsGameResourceAvatarPath("C:/WebStormProject/ArknightsGameResource/avatar/");
        filePath.setArknightsGameDataPath("C:/WebStormProject/ArknightsGameData/zh_CN/gamedata/");
        filePath.setImageOutputPath("C:/WebStormProject/ak-resources/image/avatar/");
        arknightsGameDataService.getAvatar(filePath);
    }

}
