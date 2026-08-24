package com.lhs.common.enums;

public enum QuestionnaireType {

    SELECTED_OPERATOR_FOR_NEW_GAME(1,"开荒队伍"),

    MAIN_AND_SIDE_STORY_FOR_NEW_GAME(101,"主线orSS编队"),
    INTEGRATED_STRATEGIES_FOR_NEW_GAME(102,"集成战略编队"),
    CONTINGENCY_CONTRACT_Mode_FOR_NEW_GAME(103,"危机合约编队"),
    RECLAMATION_ALGORITHM_FOR_NEW_GAME(104,"生息盐酸编队");


    QuestionnaireType(Integer code, String type) {
        this.code = code;
        this.type = type;
    }

    private Integer code;
    private String type;

    public Integer code() {
        return code;
    }



    public String type() {
        return type;
    }


}
