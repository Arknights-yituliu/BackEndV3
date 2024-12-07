package com.lhs.service.util;


import com.lhs.entity.po.survey.OperatorTable;

import java.util.List;
import java.util.Map;

public interface ArknightsGameDataService {



    Map<String, String> getEquipIdAndType();

    /**
     * 获取干员信息集合，里面主要用到干员的获取方式和实装时间
     * @return
     */
    List<OperatorTable> getOperatorTable();

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
