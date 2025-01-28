package com.lhs.service.util;




import java.util.List;
import java.util.Map;

public interface ArknightsGameDataService {


    void saveOperatorDataTag(String tag);
    String getOperatorDataTag();

    void getOperatorInfoSimpleTable();

    /**
     * 生成基建技能一览json
     */
    void getBuildingTable();

    /**
     * 生成术语对应表json
     */
    void getTermDescriptionTable();

    /**
     * 生成生息演算食物/食材一览表json
     */
    void getSandboxFoodsTable();

    void getAvatar();


}
