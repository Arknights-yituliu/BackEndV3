package com.lhs.mapper.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.item.StageResultDetail;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageResultDetailMapper extends BaseMapper<StageResultDetail> {

    void insertBatch(@Param("list") List<StageResultDetail> list);


}
