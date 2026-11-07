package com.lhs.mapper.material;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.dto.drop.RecycleDropDailyCountDTO;
import com.lhs.entity.po.material.RecycleDrop;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface RecycleDropMapper extends BaseMapper<RecycleDrop> {

    /**
     * 批量插入回收站收获记录，单条 SQL 写入多条记录。
     *
     * @param list 待插入的记录，不能为空
     * @return 实际插入的行数
     */
    int insertBatch(@Param("list") List<RecycleDrop> list);

    /**
     * 按物品聚合时间区间 [startTime, endTime) 内的产出数量与上报批次数。
     * <p>
     * 口径：recycle_drop 一行代表一件产出，因此 quantity 为明细行数；
     * 一次上报写入的记录共用同一个 create_time，按 uid + create_time 去重即为上报批次数（times）。
     * 统计按 drop_time（物品产出时间）归属日期，补传的历史数据能落到实际产出当日。
     *
     * @param startTime 统计区间开始时间（含）
     * @param endTime   统计区间结束时间（不含）
     * @return 每个物品的统计结果，区间内无数据时返回空列表
     */
    List<RecycleDropDailyCountDTO> selectDailyItemCount(@Param("startTime") Date startTime,
                                                        @Param("endTime") Date endTime);
}
