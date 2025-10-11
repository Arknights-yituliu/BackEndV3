package com.lhs.service.util;




import com.lhs.entity.dto.hypergryph.GameDataFormatFilePath;

public interface ArknightsGameDataService {


    void saveOperatorDataTag(String tag);
    String getOperatorDataTag();

    void getOperatorInfoSimpleTable(GameDataFormatFilePath gameDataFormatFilePath);

    void getOperatorInfoSimpleTableByGameResource(GameDataFormatFilePath gameDataFormatFilePath);

    /**
     * 生成基建技能一览json
     */
    void getBuildingTable(GameDataFormatFilePath gameDataFormatFilePath);

    void getBuildingTableByGameResource(GameDataFormatFilePath gameDataFormatFilePath);

    /**
     * 生成术语对应表json
     */
    void getTermDescriptionTable(GameDataFormatFilePath gameDataFormatFilePath);

    /**
     * 生成生息演算食物/食材一览表json
     */
    void getSandboxFoodsTable(GameDataFormatFilePath gameDataFormatFilePath);

    void getAvatar(GameDataFormatFilePath gameDataFormatFilePath);


}
