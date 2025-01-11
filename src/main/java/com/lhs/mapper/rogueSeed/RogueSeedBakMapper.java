package com.lhs.mapper.rogueSeed;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.rogueSeed.RogueSeed;
import com.lhs.entity.po.rogueSeed.RogueSeedBak;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RogueSeedBakMapper extends BaseMapper<RogueSeedBak> {

}
