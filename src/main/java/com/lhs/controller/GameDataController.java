package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.OperatorProgressionDataV2DTO;
import com.lhs.service.survey.OperatorDataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 干员练度相关接口
 * <p>
 * 本地导入与查询链路（/auth/survey/operator/info、/auth/survey/operator/import/skland/v3、
 * /auth/survey/operator/import/manual）已下线，本地不再承接练度数据的写入与回读；
 * 本控制器只保留第三方读取接口（/open-api/**，令牌由用户中心签发并校验）
 */
@RestController
@Tag(name = "干员练度调查")
public class GameDataController {

    private final OperatorDataService operatorDataService;

    public GameDataController(OperatorDataService operatorDataService) {
        this.operatorDataService = operatorDataService;
    }

    @Operation(summary = "第三方API获取干员练度数据（需要read或write权限token）")
    @GetMapping("/open-api/operator/info")
    public Result<List<OperatorProgressionDataV2DTO>> openApiGetOperatorData(HttpServletRequest httpServletRequest) {
        return Result.success(operatorDataService.openApiGetOperatorData(httpServletRequest));
    }

}
