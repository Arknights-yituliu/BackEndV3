package com.lhs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.other.PageVisits;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageVisitsMapper extends BaseMapper<PageVisits> {
    Integer insertBatch(@Param("list") List<PageVisits> pageVisitsList);

}
