package com.lhs.mapper.material;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.material.StageDrop;
import com.lhs.entity.po.material.StageDropDetail;
import com.lhs.entity.po.material.StageDropV2;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;


@Repository
public interface StageDropMapper extends BaseMapper<StageDrop> {

    Integer insertBatch(@Param("list")List<StageDropV2> stageDropList);

    List<StageDrop> selectStageDropByDate(@Param("start")Long start,@Param("end")Long end);
    List<StageDropDetail> selectStageDropDetail(@Param("parentId")Long id);

    List<StageDropV2> selectStageByStageId(@Param("stageId") String stageId2, @Param("start") Date start, @Param("end")Date end);
}
