package com.lhs.controller;

import com.lhs.entity.dto.drop.RecycleDropDTO;
import com.lhs.service.maa.RecycleDropUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产出数据上报接口。
 * 该接口面向所有数据来源（MAA 及其他客户端），因此不挂在 /maa 路径下。
 */
@RestController
@Tag(name = "上报")
@RequestMapping(value = "/report")
public class ReportController {

    private final RecycleDropUploadService recycleDropUploadService;

    public ReportController(RecycleDropUploadService recycleDropUploadService) {
        this.recycleDropUploadService = recycleDropUploadService;
    }

    /**
     * 上报一次回收站产出记录。
     * 请求头 authorization 的第二个片段作为玩家 uid，按 uid 做 5 秒防重复提交。
     *
     * @param httpServletRequest 用于读取 authorization 请求头
     * @param recycleDropDTO     回收站产出明细及来源、版本等信息
     */
    @Operation(summary = "回收站产出上报")
    @PostMapping("/recycleDrop")
    public void recycleDropUpload(HttpServletRequest httpServletRequest,
                                  @RequestBody RecycleDropDTO recycleDropDTO) {
        recycleDropUploadService.saveRecycleDrop(httpServletRequest, recycleDropDTO);
    }
}
