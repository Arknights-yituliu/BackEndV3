package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.survey.SurveyUser;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyUserMapper extends BaseMapper<SurveyUser> {

    Integer save(@Param("item") SurveyUser surveyUser);

    Integer updateUserById(@Param("item") SurveyUser surveyUser);

}
