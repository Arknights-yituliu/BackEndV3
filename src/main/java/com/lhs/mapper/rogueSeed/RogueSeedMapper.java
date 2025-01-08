package com.lhs.mapper.rogueSeed;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.rogueSeed.RogueSeed;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RogueSeedMapper extends BaseMapper<RogueSeed> {

    List<RogueSeed> pageRogueSeedOrderByConditionAnd(@Param("sortCondition")String sortCondition,
                                                     @Param("pageNum")Integer pageNum,
                                                     @Param("pageSize")Integer pageSize);
}
