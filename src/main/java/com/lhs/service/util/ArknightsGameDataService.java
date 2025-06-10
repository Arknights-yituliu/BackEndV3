package com.lhs.service.util;




import com.lhs.entity.dto.hypergryph.FilePath;

import java.util.List;
import java.util.Map;

public interface ArknightsGameDataService {


    void saveOperatorDataTag(String tag);
    String getOperatorDataTag();

    void getOperatorInfoSimpleTable(FilePath filePath);

    void getOperatorInfoSimpleTableByGameResource(FilePath filePath);

    /**
     * 生成基建技能一览json
     */
    void getBuildingTable(FilePath filePath);

    void getBuildingTableByGameResource(FilePath filePath);

    /**
     * 生成术语对应表json
     */
    void getTermDescriptionTable(FilePath filePath);

    /**
     * 生成生息演算食物/食材一览表json
     */
    void getSandboxFoodsTable(FilePath filePath);

    void getAvatar(FilePath filePath);


}
