package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.OperatorData;
import com.lhs.entity.po.survey.SurveyOperatorData;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperatorDataMapper extends BaseMapper<OperatorData> {
    Integer insertBatch(@Param("list") List<OperatorData> characterList);
}
