package com.lhs.mapper.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.stage.StageResultV2;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageResultV2Mapper extends BaseMapper<StageResultV2> {

    void insertBatch(@Param("list") List<StageResultV2> list);


}
