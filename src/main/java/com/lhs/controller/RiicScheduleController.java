package com.lhs.controller;

import com.lhs.common.context.UserContext;
import com.lhs.common.util.Result;
import com.lhs.entity.po.user.RiicSchedule;
import com.lhs.service.user.RiicScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基建排班表控制器
 * <p>
 * 路径挂在 /auth/** 下，由 UserInterceptor 统一校验登录态，
 * 当前登录用户从 UserContext 获取，不传 token 参数
 */
@RestController
@Tag(name = "基建排班表")
public class RiicScheduleController {

    private final RiicScheduleService riicScheduleService;

    public RiicScheduleController(RiicScheduleService riicScheduleService) {
        this.riicScheduleService = riicScheduleService;
    }

    /**
     * 保存或覆盖排班表
     * <p>
     * 请求体：{"id": 123, "schedule": "{...}"}，id 可选；
     * 不传 id 时新增（每用户最多 10 条），传 id 时覆盖（仅限本人）
     *
     * @param body 请求体（id、schedule）
     * @return 排班表 id
     */
    @Operation(summary = "保存或覆盖用户的排班表（每用户最多10条，传id则覆盖且仅限本人）")
    @PostMapping("/auth/schedule/save")
    public Result<Map<String, Long>> saveSchedule(@RequestBody Map<String, Object> body) {
        Long uid = UserContext.getUid();
        Object idObj = body.get("id");
        Long id = idObj != null ? Long.valueOf(idObj.toString()) : null;
        Object scheduleObj = body.get("schedule");
        String scheduleJson = scheduleObj != null ? scheduleObj.toString() : null;

        Long scheduleId = riicScheduleService.saveSchedule(uid, id, scheduleJson);
        Map<String, Long> result = new HashMap<>();
        result.put("id", scheduleId);
        return Result.success(result);
    }

    /**
     * 查询当前用户名下全部排班表
     *
     * @return 排班表列表（按更新时间倒序）
     */
    @Operation(summary = "查询当前用户全部排班表")
    @GetMapping("/auth/schedule/list")
    public Result<List<RiicSchedule>> listSchedules() {
        return Result.success(riicScheduleService.listByUid(UserContext.getUid()));
    }

    /**
     * 查询指定 id 的排班表（登录用户均可查看，不限本人）
     *
     * @param id 排班表 id
     * @return 排班表
     */
    @Operation(summary = "查询指定 id 的排班表（登录用户均可查看）")
    @GetMapping("/auth/schedule/detail")
    public Result<RiicSchedule> getSchedule(@RequestParam Long id) {
        return Result.success(riicScheduleService.getSchedule(id));
    }
}
