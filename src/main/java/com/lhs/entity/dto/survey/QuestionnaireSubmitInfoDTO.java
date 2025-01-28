package com.lhs.entity.dto.survey;

import lombok.Data;

import java.io.Serializable;
import java.util.List;


public class QuestionnaireSubmitInfoDTO{

    private static  final Long SerializableUid = 1L;

    private Integer type;
    private List<String> operatorList;



    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public List<String> getOperatorList() {
        return operatorList;
    }

    public void setOperatorList(List<String> operatorList) {
        this.operatorList = operatorList;
    }
}
