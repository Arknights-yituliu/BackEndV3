package com.lhs.mapper.material;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.dto.drop.StageDropQuantityCountRawDTO;
import com.lhs.entity.dto.drop.StageDropTimesCountRawDTO;
import com.lhs.entity.po.material.StageDrop;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;


@Repository
public interface StageDropMapper extends BaseMapper<StageDrop> {


    Integer insertBatch(@Param("list") List<StageDrop> stageDropList);

    void insertBatchByTable(@Param("table") String table, @Param("list") List<StageDrop> stageDropList);

    List<StageDrop> listOldStageDropByDate(@Param("tableName") String tableName, @Param("start") Date start, @Param("end") Date end);

    //根据日期查询掉落记录数量，tableName参数拼接的表名，例如：stage_drop_2026_1
    Integer countByDate(@Param("tableName") String tableName,@Param("start") Date start, @Param("end") Date end );

    //根据日期查询掉落记录，tableName参数拼接的表名，例如：stage_drop_2026_1
    List<StageDrop> selectListByDate(@Param("tableName") String tableName,@Param("start") Date start, @Param("end") Date end);

    // SQL-1：按 stageId 聚合时段内的总 times
    List<StageDropTimesCountRawDTO> selectStageTimesByDate(@Param("tableName") String tableName, @Param("start") Date start, @Param("end") Date end);

    // SQL-2：按 stageId + itemId 聚合掉落数量（JSON_TABLE）
    List<StageDropQuantityCountRawDTO> selectDropStatsByDate(@Param("tableName") String tableName, @Param("start") Date start, @Param("end") Date end);

}
