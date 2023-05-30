package com.lhs.service;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.survey.SurveyUser;
import com.lhs.mapper.SurveyUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class SurveyUserService {

    @Resource
    private SurveyUserMapper surveyUserMapper;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 调查表注册
     *
     * @param ipAddress ip
     * @param userName  用户id
     * @return 成功消息
     */
    public HashMap<Object, Object> register(String ipAddress, String userName) {
        Long incrId = redisTemplate.opsForValue().increment("incrUId");   //从redis拿到自增id
        Date date = new Date();  //存入的时间
        String userNameAndEnd = null;
        SurveyUser surveyUser = null;

        for (int i = 0; i < 5; i++) {
            if (i < 3) {
                userNameAndEnd = userName + randomEnd4(4); //用户id后四位后缀  #xxxx
                surveyUser = surveyUserMapper.selectSurveyUserByUserName(userNameAndEnd);
                if (surveyUser == null) break;  //未注册就跳出开始注册
                log.warn("发生用户id碰撞");
            } else {
                userNameAndEnd = userName + randomEnd4(5);
                surveyUser = surveyUserMapper.selectSurveyUserByUserName(userNameAndEnd);
                if (surveyUser == null) break;  //未注册就跳出开始注册
                log.warn("用户id碰撞过多扩充位数");
            }
        }

        if (surveyUser != null) throw new ServiceException(ResultCode.USER_ID_ERROR);

        surveyUser = SurveyUser.builder()
                .id(incrId)
                .userName(userNameAndEnd)
                .createTime(date)
                .updateTime(date)
                .status(1)
                .ip(ipAddress)
                .build();

        Integer row = surveyUserMapper.insertSurveyUser(surveyUser);
        if (row < 1) throw new ServiceException(ResultCode.SYSTEM_INNER_ERROR);

        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("userName", userNameAndEnd);
        hashMap.put("uid", incrId);
        hashMap.put("status", 1);
        return hashMap;
    }

    private String randomEnd4(Integer digit) {
        int random = new Random().nextInt(9999);
        String end4 = String.format("%0" + digit + "d", random);
        return "#" + end4;
    }


    /**
     * 调查表登录
     * @param ipAddress  ip
     * @param userName  用户id
     * @return 成功消息
     */
    public HashMap<Object, Object> login(String ipAddress, String userName) {
        SurveyUser surveyUser = surveyUserMapper.selectSurveyUserByUserName(userName);  //查询用户
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("userName", userName);
        hashMap.put("uid", surveyUser.getId());
        hashMap.put("status", surveyUser.getStatus());
        return hashMap;
    }


    public SurveyUser selectSurveyUser(String userName){
        SurveyUser surveyUser = surveyUserMapper.selectSurveyUserByUserName(userName); //查询用户
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        surveyUser.setUpdateTime(new Date());   //更新用户最后一次上传时间
        return surveyUser;
    }


    public void updateSurveyUser(SurveyUser surveyUser){
        surveyUserMapper.updateSurveyUser(surveyUser);   //更新用户表
    }

    public List<Long> selectSurveyUserIds() {
        return  surveyUserMapper.selectSurveyUserIds();
    }


    public void updateConfigByKey(String value, String key) {
        surveyUserMapper.updateConfigByKey(value,key);
    }

    public String selectConfigByKey(String key) {
       return    surveyUserMapper.selectConfigByKey(key);
    }

    public Integer getTableIndex(Long id){
         if(id<20000) return 1;
         if(id<40000) return 2;
        return 1;
    }



    public SurveyUser registerByMaa(String ipAddress) {
        Long incrId = redisTemplate.opsForValue().increment("incrUId");
        SurveyUser surveyUser = surveyUserMapper.selectLastSurveyUserIp(ipAddress);

        if(surveyUser!=null) return surveyUser;

        Date date = new Date();
        long id = incrId;
        String userNameAndEnd = "MAA#" + incrId;
        surveyUser = SurveyUser.builder()
                .id(id)
                .userName(userNameAndEnd)
                .createTime(date)
                .updateTime(date)
                .status(1)
                .ip(ipAddress)
                .build();
        Integer row = surveyUserMapper.insertSurveyUser(surveyUser);
        if (row < 1) throw new ServiceException(ResultCode.SYSTEM_INNER_ERROR);

        return surveyUser;
    }

}
