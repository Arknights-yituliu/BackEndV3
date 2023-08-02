package com.lhs.service.survey;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.survey.SurveyUser;
import com.lhs.mapper.SurveyUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

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
        Date date = new Date();  //存入的时间
        String userNameAndEnd = null;
        SurveyUser surveyUser = null;

        for (int i = 0; i < 5; i++) {
            if (i < 3) {
                userNameAndEnd = userName + randomEnd4(4); //用户id后四位后缀  #xxxx
                surveyUser = surveyUserMapper.selectSurveyUserByUserName(userNameAndEnd);
                if (surveyUser == null) break;  //未注册就跳出开始注册
                log.warn("id后缀重复");
            } else {
                userNameAndEnd = userName + randomEnd4(5);
                surveyUser = surveyUserMapper.selectSurveyUserByUserName(userNameAndEnd);
                if (surveyUser == null) break;  //未注册就跳出开始注册
                log.warn("id后缀重复次数过多扩充位数");
            }
        }

        if (surveyUser != null) throw new ServiceException(ResultCode.USER_ID_ERROR);

        String idStr = date.getTime()+randomEnd4_id();
        long id = Long.parseLong(idStr);

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

        String token = AES.encrypt(surveyUser.getUserName()+"."+id, ApplicationConfig.Secret);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("userName", userNameAndEnd);
        hashMap.put("token", token);
        hashMap.put("status",1);

        return hashMap;
    }


    private void checkUserName(String userName){
        if(userName.length()>30){
            throw new ServiceException(ResultCode.USER_NAME_LENGTH_TOO_LONG);
        }

        for (int i = 0; i < userName.length(); i++) {
            String substring = userName.substring(i, i + 1);
            if (substring.matches("[\u4e00-\u9fa5 ]+")){
                System.out.println("中文: "+substring);
            } else if (substring.matches("[a-zA-Z ]+")) {
                System.out.println("英文 ：" + substring);
            } else {
                System.out.println("其他字符："+substring);
                throw new ServiceException(ResultCode.USER_NAME_MUST_BE_IN_CHINESE_OR_ENGLISH);
            }
        }



    }

    private String randomEnd4(Integer digit) {
        int random = new Random().nextInt(9999);
        String end4 = String.format("%0" + digit + "d", random);
        return "#" + end4;
    }

    private String randomEnd4_id(){
        int random = new Random().nextInt(99);
        return String.format("%02d", random);
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
        if (surveyUser.getStatus()==0) throw new ServiceException(ResultCode.USER_ACCOUNT_FORBIDDEN);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("userName", userName);
        hashMap.put("token", AES.encrypt(userName+"."+surveyUser.getId(), ApplicationConfig.Secret));
        hashMap.put("status",surveyUser.getStatus());
        return hashMap;
    }

    public  Long decryptToken(String token){

        String decrypt = AES.decrypt(token.replaceAll(" +", "+"), ApplicationConfig.Secret);
        String idStr = decrypt.split("\\.")[1];
        return Long.valueOf(idStr);
    }

    public SurveyUser getSurveyUserByUserName(String userName){
        SurveyUser surveyUser = surveyUserMapper.selectSurveyUserByUserName(userName); //查询用户
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        return surveyUser;
    }

    public SurveyUser getSurveyUserById(String token){
        Long id = decryptToken(token);
        SurveyUser surveyUser = surveyUserMapper.selectSurveyUserById(id); //查询用户
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
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



    @RedisCacheable(key = "CharacterTableSimple",timeout = -1)
    public JSONObject getCharacterTable(){
        String read = FileUtil.read(ApplicationConfig.Item + "character_table_simple.json");
        return JSONObject.parseObject(read);
    }




}
