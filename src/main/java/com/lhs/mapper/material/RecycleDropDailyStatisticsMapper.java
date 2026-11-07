package com.lhs.mapper.material;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.dto.drop.RecycleDropDailyCountDTO;
import com.lhs.entity.dto.drop.RecycleDropStatDayRangeDTO;
import com.lhs.entity.po.material.RecycleDropDailyStatistics;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 回收站每日物品产出统计Mapper接口
 * 对应数据库表：recycle_drop_daily_statistics
 */
@Repository
public interface RecycleDropDailyStatisticsMapper extends BaseMapper<RecycleDropDailyStatistics> {

    /**
     * 批量插入某天的物品产出统计
     * <p>
     * 单日统计一次写入几十~几百条，逐条 insert 会让事务持有大量往返；
     * 配合 rewriteBatchedStatements 合并为一条多值 INSERT。
     *
     * @param list 待写入的统计记录，不可为空
     * @return 写入条数
     */
    int insertBatch(@Param("list") List<RecycleDropDailyStatistics> list);

    /**
     * 查询全部未过期数据中每个物品的产出总数量（按 quantity 降序）。
     * <p>
     * 同一天只保留一份 DISPLAY 数据（重跑会先把旧 task 置为 EXPIRE），可直接跨天求和。
     * 返回的 times 为空，掉率分母由 {@link #selectTotalTimes()} 单独查询。
     *
     * @return 按产出总数降序的物品统计，无数据时返回空列表
     */
    List<RecycleDropDailyCountDTO> selectItemTotal();

    /**
     * 查询全部未过期数据的总记录条数（不分材料）。
     * <p>
     * 同一天每一行的 times 相同，先按天取 MAX 再跨天求和，避免按物品重复累加。
     *
     * @return 总记录条数，无数据时返回 0
     */
    Long selectTotalTimes();

    /**
     * 查询全部未过期数据覆盖的日期范围（最早 / 最晚统计日）。
     *
     * @return 日期范围，无数据时 startDay / endDay 均为 null
     */
    RecycleDropStatDayRangeDTO selectStatDayRange();
}
