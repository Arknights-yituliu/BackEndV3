package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.OperatorProgressionDataDTO;
import com.lhs.entity.dto.survey.OperatorProgressionDataV2DTO;
import com.lhs.entity.dto.survey.ManualOperatorDataDTO;
import com.lhs.entity.dto.survey.PlayerInfoDTO;
import com.lhs.service.survey.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "干员练度调查")

public class GameDataController {
    private final OperatorDataService operatorDataService;

    public GameDataController(OperatorDataService operatorDataService) {
        this.operatorDataService = operatorDataService;
    }

    @Operation(summary = "获取干员数据")
    @GetMapping("/auth/survey/operator/info")
    public Result<Object> getOperatorTable() {
        List<OperatorProgressionDataDTO> operatorProgressionDataDTOList = operatorDataService
                .listOperatorProgressionData();
        return Result.success(operatorProgressionDataDTOList);
    }

    @Operation(summary = "通过森空岛导入干员练度V3")
    @PostMapping("/auth/survey/operator/import/skland/v3")
    public Result<Object> importSurveyCharacterFormBySKLandV3(HttpServletRequest httpServletRequest,
            @RequestBody PlayerInfoDTO playerInfoDTO) {

        return Result.success(operatorDataService.importSKLandPlayerInfoV3(httpServletRequest, playerInfoDTO));
    }

    @Operation(summary = "手动导入干员练度")
    @PostMapping("/auth/survey/operator/import/manual")
    public Result<Map<String, Object>> importManualOperatorData(
            @RequestBody ManualOperatorDataDTO manualOperatorDataDTO) {
        return Result.success(operatorDataService.importManualOperatorData(manualOperatorDataDTO));
    }

    @Operation(summary = "第三方API获取干员练度数据（需要read或write权限token）")
    @GetMapping("/open-api/operator/info")
    public Result<List<OperatorProgressionDataV2DTO>> openApiGetOperatorData(HttpServletRequest httpServletRequest) {
        return Result.success(operatorDataService.openApiGetOperatorData(httpServletRequest));
    }

}
