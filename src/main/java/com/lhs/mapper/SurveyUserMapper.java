package com.lhs.mapper;

import com.lhs.entity.survey.SurveyUser;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
public interface SurveyUserMapper {

    //新增用户
    Integer insertSurveyUser(@Param("user") SurveyUser user);

    //查询用户根据用户名
    SurveyUser selectSurveyUserByUserName(@Param("userName") String userName);

    //查询用户根据id
    SurveyUser selectSurveyUserById(@Param("id") Long id);

    //查询用户根据ip
    SurveyUser  selectLastSurveyUserIp(@Param("id") String id);

    //更新用户
    Integer updateSurveyUser(@Param("user") SurveyUser user);

    //用户表所有uid
    List<Long> selectSurveyUserIds();

    //查询最后一个注册的用户的id
    Long selectLastSurveyUserId();


    String selectConfigByKey(@Param("configKey")String configKey);

    Integer updateConfigByKey(@Param("configValue")String configValue,@Param("configKey")String configKey);
}
