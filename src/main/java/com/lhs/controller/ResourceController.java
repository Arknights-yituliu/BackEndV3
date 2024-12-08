package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.po.material.Stage;
import com.lhs.service.util.ArknightsGameDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "资源API")
public class ResourceController {

    private final ArknightsGameDataService arknightsGameDataService;

    public ResourceController(ArknightsGameDataService arknightsGameDataService) {
        this.arknightsGameDataService = arknightsGameDataService;
    }

    @Operation(summary = "检查干员数据tag，让前端判断是否需要更新")
    @GetMapping("/check/operator-data")
    public Result<String> checkOperatorData() {
        String operatorDataTag = arknightsGameDataService.getOperatorDataTag();
        return Result.success(operatorDataTag);
    }

    @Operation(summary = "保存干员数据的tag")
    @GetMapping("/upload/operator-data")
    public Result<Object> updateStageResult(@RequestParam("tag") String tag) {
        arknightsGameDataService.saveOperatorDataTag(tag);
        return Result.success();
    }
}
