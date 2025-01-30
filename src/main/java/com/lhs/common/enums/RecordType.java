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

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }
}
