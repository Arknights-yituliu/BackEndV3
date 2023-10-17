package com.lhs.entity.vo.item;


import com.lhs.entity.dto.item.ActTagAreaDTO;
import lombok.Data;

import java.util.List;

@Data
public class StoreActVO {
    private String actEndDate;

    private List<ActTagAreaDTO> actTagArea;

    private String actImgUrl;

    private Double actPPRBase;

    private Double actPPRStair;

    private String actName;

    private List<StoreItemVO> actStore;
}