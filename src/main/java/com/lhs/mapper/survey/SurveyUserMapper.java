package com.lhs.mapper.survey;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.survey.UserInfo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyUserMapper extends BaseMapper<UserInfo> {

    Integer save(@Param("item") UserInfo userInfo);

    Integer updateUserById(@Param("item") UserInfo userInfo);

}
