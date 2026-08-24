package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.OperatorProgressionData;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface OperatorProgressionDataMapper extends BaseMapper<OperatorProgressionData> {

    List<OperatorProgressionData> getOperatorProgressionDataByDate(@Param("createTime") Date createTime, @Param("offset") Integer offset);

    List<OperatorProgressionData> getOperatorProgressionData(@Param("offset") Integer offset,@Param("rowNum")Integer rowNum);
}
