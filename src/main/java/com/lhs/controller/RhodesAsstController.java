package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.PlayerInfoDTO;
import com.lhs.service.survey.OperatorDataService;
import com.lhs.service.survey.WarehouseInfoService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/rhodes")
public class RhodesAsstController {

    private final OperatorDataService operatorDataService;
    private final WarehouseInfoService warehouseInfoService;


    public RhodesAsstController(OperatorDataService operatorDataService,
                                WarehouseInfoService warehouseInfoService) {
        this.operatorDataService = operatorDataService;
        this.warehouseInfoService = warehouseInfoService;
    }


}
