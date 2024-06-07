package com.lhs.service.util;


import com.lhs.entity.po.survey.OperatorTable;

import java.util.List;
import java.util.Map;

public interface ArknightsGameDataService {



    Map<String, String> getEquipIdAndType();


    Map<String, String> getHasEquipTable();


    List<OperatorTable> getOperatorTable();

    void getCharacterData();

    void getOperatorApCost();

    void getBuildingTable();

    void getBuildingTableByBot();

    void getPortrait();

    void getAvatar();

    void getAvatarByBot();
}
