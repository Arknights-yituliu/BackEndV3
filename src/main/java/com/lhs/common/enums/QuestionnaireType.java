package com.lhs.common.enums;

public enum QuestionnaireType {

    SELECTED_OPERATOR_FOR_NEW_GAME(1,"开荒队伍");


    QuestionnaireType(Integer code, String type) {
        this.code = code;
        this.type = type;
    }

    private Integer code;
    private String type;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
