package com.lhs.entity.vo.stage;


import com.lhs.entity.dto.stage.ActTagAreaDTO;
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