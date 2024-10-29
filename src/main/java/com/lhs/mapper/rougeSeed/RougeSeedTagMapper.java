package com.lhs.mapper.rougeSeed;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.rougeSeed.RougeSeedTag;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RougeSeedTagMapper extends BaseMapper<RougeSeedTag> {

    Integer insertBatch(@Param("list") List<RougeSeedTag> list);
}
