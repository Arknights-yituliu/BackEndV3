package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.OperatorProgressionManualData;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OperatorProgressionManualDataMapper extends BaseMapper<OperatorProgressionManualData> {

    OperatorProgressionManualData getTimeInfoByAkUid(@Param("akUid") String akUid);
}
