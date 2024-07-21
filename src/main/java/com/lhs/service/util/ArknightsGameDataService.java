package com.lhs.service.util;


import com.lhs.entity.po.survey.OperatorTable;

import java.util.List;
import java.util.Map;

public interface ArknightsGameDataService {



    Map<String, String> getEquipIdAndType();


    List<OperatorTable> getOperatorTable();

    void getOperatorInfoSimpleTable();

    void getBuildingTable();

    void getTermDescriptionTable();

    void getPortrait();

    void getAvatar();


}
