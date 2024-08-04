package com.lhs.entity.dto.survey;

import com.lhs.entity.po.survey.OperatorData;
import com.lhs.entity.po.survey.WarehouseInfo;
import lombok.Data;

import java.util.List;

@Data
public class PlayerInfoDTO {
    private String token;
    private String uid;
    private String nickName;
    private String channelName;
    private Integer channelMasterId;
    private List<OperatorData> operatorDataList;
    private List<WarehouseInfo> itemList;

}
