package com.lhs.controller;

import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.Result;
import com.lhs.entity.dto.drop.RecycleDropDTO;
import com.lhs.service.maa.RecycleDropUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产出数据上报接口。
 * 该接口面向所有数据来源。
 */
@RestController
@Tag(name = "上报")
@RequestMapping(value = "/report")
public class ReportController {

    private final RecycleDropUploadService recycleDropUploadService;

    /** uid 生成器，workerId 固定为 1，与项目内其他服务保持一致 */
    private final IdGenerator idGenerator;

    public ReportController(RecycleDropUploadService recycleDropUploadService) {
        this.recycleDropUploadService = recycleDropUploadService;
        this.idGenerator = new IdGenerator(1L);
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

    /**
     * 生成一个随机的 uid。
     * 仅用 IdGenerator 生成并直接返回，不落库。
     *
     * @return 生成的 uid
     */
    @Operation(summary = "生成一个随机的uid（不落库）")
    @GetMapping("/uid/generate")
    public Result<Long> generateUid() {
        return Result.success(idGenerator.nextId());
    }
}
