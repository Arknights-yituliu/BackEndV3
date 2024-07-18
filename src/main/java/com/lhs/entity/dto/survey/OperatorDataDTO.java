package com.lhs.entity.dto.survey;

import com.lhs.entity.po.survey.OperatorData;
import lombok.Data;

import java.util.List;

@Data
public class OperatorDataDTO {
    private String userToken;
    private String uid;
    private String nickName;
    private String channelName;
    private Integer channelMasterId;
    private List<OperatorData> operatorDataList;

}
