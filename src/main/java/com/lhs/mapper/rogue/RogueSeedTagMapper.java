package com.lhs.mapper.rogue;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.rogue.RogueSeedTag;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RogueSeedTagMapper extends BaseMapper<RogueSeedTag> {

    Integer insertBatch(@Param("list") List<RogueSeedTag> list);
}
