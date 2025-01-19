package com.lhs.mapper.rogue;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.rogue.RogueSeedRating;
import com.lhs.entity.vo.rogue.RogueSeedRatingVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RogueSeedRatingMapper extends BaseMapper<RogueSeedRating> {
    List<RogueSeedRatingVO> listRogueSeedRating(@Param("pageNum")Integer pageNum,@Param("pageSize")Integer pageSize);
}
