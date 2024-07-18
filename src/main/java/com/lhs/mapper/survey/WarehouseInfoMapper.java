package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.WarehouseInfo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseInfoMapper extends BaseMapper<WarehouseInfo> {
    Integer insertBatch( @Param("list") List<WarehouseInfo> list);
}
