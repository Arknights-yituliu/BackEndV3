package com.lhs.mapper.material;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.material.RecycleDrop;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecycleDropMapper extends BaseMapper<RecycleDrop> {

    /**
     * 批量插入回收站收获记录，单条 SQL 写入多条记录。
     *
     * @param list 待插入的记录，不能为空
     * @return 实际插入的行数
     */
    int insertBatch(@Param("list") List<RecycleDrop> list);
}
