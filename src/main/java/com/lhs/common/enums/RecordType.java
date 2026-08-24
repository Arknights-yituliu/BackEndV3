package com.lhs.common.enums;

public enum RecordType {
    DISPLAY(1,"展示数据"),
    EXPIRE(-1,"过期数据"),

    ARCHIVED(2,"归档数据");
    private Integer code;
    private String recordType;



    RecordType(Integer code, String recordType) {
        this.code = code;
        this.recordType = recordType;
    }

    public Integer code() {
        return code;
    }

    public String recordType() {
        return recordType;
    }


}
