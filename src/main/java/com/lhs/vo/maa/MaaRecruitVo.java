package com.lhs.vo.maa;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

import java.util.List;

//用于接收maa的公招数据
@Data
public class MaaRecruitVo {

    private String uuid;
    private List<String> tags;
    private Integer level;
    private JSONArray result;
    private String server;
    private String source;
    private String version;

    public MaaRecruitVo() {
    }
}
