package com.lhs.entity.vo.maa;


import lombok.Data;

import java.util.List;

//用于接收maa的公招数据
@Data
public class MaaRecruitVo {

    private String uuid;
    private List<String> tags;
    private Integer level;
    private String server;
    private String source;
    private String version;

    public MaaRecruitVo() {
    }
}
