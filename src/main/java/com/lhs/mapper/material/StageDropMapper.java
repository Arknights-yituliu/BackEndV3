package com.lhs.mapper.material;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.dto.maa.StageDropDTO;
import com.lhs.entity.po.material.StageDrop;
import com.lhs.entity.po.material.StageDropDetail;
import com.lhs.entity.po.material.StageDropStatistics;
import com.lhs.entity.po.material.StageDropV2;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;


@Repository
public interface StageDropMapper extends BaseMapper<StageDrop> {

    Integer insertBatch(@Param("tableName")String tableName,@Param("list")List<StageDropV2> stageDropList);

    List<StageDropDTO> selectStageDropByDate(@Param("start")Long start, @Param("end")Long end);
    List<StageDropDetail> selectStageDropDetail(@Param("start")Long start,@Param("end")Long end);

    List<StageDropV2> selectStageDropV2ByStageId(@Param("stageId") String stageId2, @Param("start") Date start, @Param("end")Date end);

    List<StageDropV2> selectStageDropV2ByDate(@Param("tableName")String tableName,@Param("start") Date start, @Param("end")Date end);

    Integer insertBatchStageDropStatistics(@Param("list")List<StageDropStatistics> stageDropList);

    List<StageDropStatistics> listStageDropStatisticsByDate(@Param("timeGranularity") Integer timeGranularity,
                                                                              @Param("start") Date start, @Param("end")Date end);

    List<StageDropStatistics> listStageDropStatisticsByStageId(@Param("stageId") String stageId,@Param("timeGranularity") Integer timeGranularity,
                                                            @Param("start") Date start, @Param("end")Date end);


}
