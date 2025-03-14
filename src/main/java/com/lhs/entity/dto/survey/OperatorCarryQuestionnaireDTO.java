package com.lhs.entity.dto.survey;

import java.util.List;

public class OperatorCarryQuestionnaireDTO {
    private Integer questionnaireCode;
    private List<String> operatorList;



    public Integer getQuestionnaireCode() {
        return questionnaireCode;
    }

    public void setQuestionnaireCode(Integer code) {
        this.questionnaireCode = code;
    }

    public List<String> getOperatorList() {
        return operatorList;
    }

    public void setOperatorList(List<String> operatorList) {
        this.operatorList = operatorList;
    }
}
