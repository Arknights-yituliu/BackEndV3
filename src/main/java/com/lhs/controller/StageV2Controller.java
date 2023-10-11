package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.stage.StageParamDTO;
import com.lhs.entity.po.stage.StageResultSample;
import com.lhs.service.stage.StageResultV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name ="API—关卡效率、材料价值、商店性价比")
@CrossOrigin()
public class StageV2Controller {

    private final StageResultV2Service stageResultV2Service;

    public StageV2Controller(StageResultV2Service stageResultV2Service) {
        this.stageResultV2Service = stageResultV2Service;
    }

    @Operation(summary ="获取蓝材料推荐关卡按效率倒序")
    @GetMapping("/stage/t3/v2")
    public Result<List<List<StageResultSample>>> getStageResultT3(@RequestParam Double expCoefficient) {
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setDisplay("public");
        stageParamDTO.setVersion("v2");
        List<List<StageResultSample>> stageResultDataT3V3 = stageResultV2Service.getStageResultDataT3V3(stageParamDTO);

        return Result.success(stageResultDataT3V3);
    }
}
