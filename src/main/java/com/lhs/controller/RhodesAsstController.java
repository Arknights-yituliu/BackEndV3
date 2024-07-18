package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.item.StageParamDTO;
import com.lhs.entity.dto.survey.OperatorDataDTO;
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

    @Operation(summary = "获取蓝材料推荐关卡按效率倒序")
    @PostMapping("/operator/info/upload")
    public Result<Map<String, Object>> getStageResultT3(@RequestBody OperatorDataDTO operatorDataDTO) {

        operatorDataService.saveOperatorDataByRhodes(operatorDataDTO);

        return Result.success();
    }
}
