package com.lhs.entity.vo.item;


import com.lhs.entity.dto.item.ActTagAreaDTO;
import lombok.Data;

import java.util.List;

@Data
public class ActivityStoreDataVO {

    private Long endTime;

    private List<ActTagAreaDTO> actTagArea;

    private String imageLink;

    private Double actPPRBase;

    private Double actPPRStair;

    private String actName;

    private List<StoreItemVO> actStore;


}