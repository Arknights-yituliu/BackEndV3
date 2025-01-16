package com.lhs.mapper.rogueSeed;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.rogue.RogueSeedRatingStatistics;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RogueSeedRatingStatisticsMapper extends BaseMapper<RogueSeedRatingStatistics> {

       List<RogueSeedRatingStatistics> pageRogueSeedRatingStatistics(@Param("pageNum")Integer pageNum,@Param("pageSize")Integer pageSize);
}
