package com.lhs.mapper.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.item.StageResultCommon;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageResultCommonMapper extends BaseMapper<StageResultCommon> {
    void insertBatch(@Param("list") List<StageResultCommon> list);
}
