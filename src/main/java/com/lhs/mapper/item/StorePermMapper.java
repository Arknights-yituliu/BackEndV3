package com.lhs.mapper.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.item.StorePerm;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StorePermMapper extends BaseMapper<StorePerm> {

    Integer updateBatch(@Param("list") List<StorePerm> storePermList);
}
