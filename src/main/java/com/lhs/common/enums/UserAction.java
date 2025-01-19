package com.lhs.common.enums;

public enum UserAction {

    COPY(1,"复制"),
    RATING(2,"点赞");

    private Integer actionCode;
    private String action;

    UserAction(Integer actionCode, String action) {

    }

    public Integer getActionCode() {
        return actionCode;
    }

    public String getAction() {
        return action;
    }
}
