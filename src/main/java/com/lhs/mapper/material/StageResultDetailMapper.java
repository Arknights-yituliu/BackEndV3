package com.lhs.mapper.material;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.material.StageResultDetail;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageResultDetailMapper extends BaseMapper<StageResultDetail> {

    void insertBatch(@Param("list") List<StageResultDetail> list);


}
