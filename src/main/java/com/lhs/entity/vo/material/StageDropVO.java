package com.lhs.entity.vo.material;

import lombok.Data;

import java.util.List;

@Data
public class StageDropVO {
     private Long id;
     private Long createTime;
     private String stageId;
     private String server;
     private String version;
     private String  uid;
     private Integer times;
     private List<StageDropDetailVO> dropList;
}
