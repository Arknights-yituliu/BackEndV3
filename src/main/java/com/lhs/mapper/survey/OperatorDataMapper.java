package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.OperatorData;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperatorDataMapper extends BaseMapper<OperatorData> {

    //批量插入干员练度信息表
    Integer insertBatch(@Param("tableName") String tableName,
                        @Param("list") List<OperatorData> characterList);

    void updateByUid(@Param("tableName") String tableName, @Param("item") OperatorData operatorData);


}
