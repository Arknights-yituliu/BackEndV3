package com.lhs.controller;

import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.Result;
import com.lhs.common.util.SignUtil;
import com.lhs.entity.vo.drop.RecycleDropBackfillResultVO;
import com.lhs.entity.vo.drop.RecycleDropStatisticsVO;
import com.lhs.service.material.RecycleDropDailyStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 回收站产出统计接口。
 * <p>
 * 调用时必须在请求参数 {@value #KEY_PARAM} 中携带明文密钥，
 * 服务端只保存密钥的 SHA-256 摘要，校验时对请求明文做同样摘要后比对。
 */
@RestController
@Tag(name = "回收站产出统计")
public class RecycleDropController {

    /** 密钥参数名 */
    private static final String KEY_PARAM = "key";

    /** 密钥明文的 SHA-256 摘要（小写十六进制），此处只保存摘要，不保存明文 */
    private static final String API_KEY_SHA256 = "91e49dd07365b175fecd09017bdbba19c978707f4d5d5ccd628dc799a72bef70";

    private final RecycleDropDailyStatisticsService recycleDropDailyStatisticsService;

    public RecycleDropController(RecycleDropDailyStatisticsService recycleDropDailyStatisticsService) {
        this.recycleDropDailyStatisticsService = recycleDropDailyStatisticsService;
    }

    /**
     * 查询全部未过期数据中每个物品的产出总数，以及全部物品的总记录条数。
     * <p>
     * 返回结果带 startDay / endDay，标识这批统计数据覆盖的日期范围；
     * times 是所有物品的记录条数总和（不分材料），每条记录取到相同的值，可直接用 quantity / times 算占比。
     *
     * @param key 访问密钥明文
     * @return 数据日期范围与每个物品的 itemId、quantity（产出总数）、times（全部记录条数）
     */
    @Operation(summary = "查询每个物品的产出总数与总记录条数")
    @GetMapping("/recycle-drop/statistics")
    public Result<RecycleDropStatisticsVO> getItemTotal(
            @RequestParam(value = KEY_PARAM, required = false) String key) {
        validateApiKey(key);
        return Result.success(recycleDropDailyStatisticsService.getItemTotal());
    }

    /**
     * 手动触发历史数据回填。
     * <p>
     * 回填 [今天-days, 今天) 共 days 个完整自然日的物品产出统计，重复调用会覆盖对应日期的旧结果。
     * 同一时间只允许一个回填任务执行；建议避开 22:00 与每小时 25 分的定时统计窗口。
     *
     * @param key  访问密钥明文
     * @param days 回填天数，默认 14，最大 90
     * @return 回填结果，包含成功/失败天数与失败日期
     */
    @Operation(summary = "回填回收站每日物品产出统计")
    @GetMapping("/recycle-drop/statistics/backfill")
    public Result<RecycleDropBackfillResultVO> backfill(
            @RequestParam(value = KEY_PARAM, required = false) String key,
            @RequestParam(defaultValue = "14") int days) {
        validateApiKey(key);
        return Result.success(recycleDropDailyStatisticsService.backfill(days));
    }

    /**
     * 校验请求参数中的密钥：对明文做 SHA-256 后与硬编码的摘要比对。
     * <p>
     * 用 {@link MessageDigest#isEqual} 做常量时间比较，避免按字节比较时的时序侧信道。
     *
     * @param key 请求携带的密钥明文
     * @throws ServiceException 密钥缺失或不匹配时抛出，返回无访问权限
     */
    private void validateApiKey(String key) {
        if (key == null || key.isBlank()) {
            throw new ServiceException(ResultCode.PERMISSION_NO_ACCESS);
        }
        byte[] actual = SignUtil.sha256(key.trim()).getBytes(StandardCharsets.UTF_8);
        byte[] expected = API_KEY_SHA256.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(actual, expected)) {
            throw new ServiceException(ResultCode.PERMISSION_NO_ACCESS);
        }
    }
}
