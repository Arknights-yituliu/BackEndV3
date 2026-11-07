package com.lhs.mapper.material;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.dto.drop.RecycleDropDailyCountDTO;
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
     * 查询全部未过期数据中每个物品的产出总数与上报批次数总和。
     * <p>
     * 同一天只保留一份 DISPLAY 数据（重跑会先把旧 task 置为 EXPIRE），可直接跨天求和。
     *
     * @return 按产出总数降序的物品统计，无数据时返回空列表
     */
    List<RecycleDropDailyCountDTO> selectAllItemTotal();
}
