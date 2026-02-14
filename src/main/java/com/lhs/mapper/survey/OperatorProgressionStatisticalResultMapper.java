package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.OperatorProgressionStatisticalResult;
import org.apache.ibatis.annotations.Param;

public interface OperatorProgressionStatisticalResultMapper extends BaseMapper<OperatorProgressionStatisticalResult> {

    /**
     * 插入或更新record_type=1的数据
     * @param item 统计结果对象
     * @return 影响的行数
     */
    int insertOrUpdateByRecordType(@Param("item") OperatorProgressionStatisticalResult item);
}
