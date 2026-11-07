package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.vo.drop.RecycleDropBackfillResultVO;
import com.lhs.entity.vo.drop.RecycleDropItemTotalVO;
import com.lhs.service.material.RecycleDropDailyStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 回收站产出统计接口（公开，不鉴权）。
 * 查询数据来自 recycle_drop_daily_statistics 预聚合表，不直接扫 recycle_drop。
 */
@RestController
@Tag(name = "回收站产出统计")
public class RecycleDropController {

    private final RecycleDropDailyStatisticsService recycleDropDailyStatisticsService;

    public RecycleDropController(RecycleDropDailyStatisticsService recycleDropDailyStatisticsService) {
        this.recycleDropDailyStatisticsService = recycleDropDailyStatisticsService;
    }

    /**
     * 查询全部未过期数据中每个物品的产出总数与上报批次数总和，按产出总数降序。
     *
     * @return 每个物品的 itemId、quantity（产出总数）、times（上报批次数总和）
     */
    @Operation(summary = "查询每个物品的产出总数与掉落次数")
    @GetMapping("/recycle-drop/statistics")
    public Result<List<RecycleDropItemTotalVO>> getItemTotal() {
        return Result.success(recycleDropDailyStatisticsService.getAllItemTotal());
    }

    /**
     * 手动触发历史数据回填。
     * <p>
     * 回填 [今天-days, 今天) 共 days 个完整自然日的物品产出统计，重复调用会覆盖对应日期的旧结果。
     * 同一时间只允许一个回填任务执行；建议避开 22:00 与每小时 25 分的定时统计窗口。
     *
     * @param days 回填天数，默认 14，最大 90
     * @return 回填结果，包含成功/失败天数与失败日期
     */
    @Operation(summary = "回填回收站每日物品产出统计")
    @PostMapping("/recycle-drop/statistics/backfill")
    public Result<RecycleDropBackfillResultVO> backfill(
            @RequestParam(defaultValue = "14") int days) {
        return Result.success(recycleDropDailyStatisticsService.backfill(days));
    }
}
