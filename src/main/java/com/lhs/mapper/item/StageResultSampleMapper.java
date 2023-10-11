package com.lhs.mapper.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.stage.StageResultSample;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageResultSampleMapper extends BaseMapper<StageResultSample> {
    void insertBatch(@Param("list") List<StageResultSample> list);
}
