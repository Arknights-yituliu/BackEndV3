package com.lhs.mapper.material;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.material.StageDrop;
import com.lhs.entity.po.material.StageDropStatistics;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;


@Repository
public interface StageDropMapper extends BaseMapper<StageDrop> {

    Integer countByDate(@Param("start") Date start, @Param("end") Date end);

    List<StageDrop> listStageDropByDate(@Param("start") Date start, @Param("end") Date end);

    Integer insertBatch(@Param("list") List<StageDrop> stageDropList);

    Integer insertBatchByTable(@Param("table") String table,@Param("list") List<StageDrop> stageDropList);

    List<StageDrop> listOldStageDropByDate(@Param("tableName") String tableName, @Param("start") Date start, @Param("end") Date end);

    //根据日期查询掉落记录数量，tableName参数拼接的表名，例如：stage_drop_2026_1
    Integer countFromTableByDate(@Param("tableName") String tableName,@Param("start") Date start, @Param("end") Date end );

    //根据日期查询掉落记录，tableName参数拼接的表名，例如：stage_drop_2026_1
    List<StageDrop> selectFromTableListByDate(@Param("tableName") String tableName,@Param("start") Date start, @Param("end") Date end);



}
