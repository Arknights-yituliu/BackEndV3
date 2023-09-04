package com.lhs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.survey.SurveyUser;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
public interface SurveyUserMapper extends BaseMapper<SurveyUser> {

    //用户表所有uid
    List<Long> selectSurveyUserIds();


}
