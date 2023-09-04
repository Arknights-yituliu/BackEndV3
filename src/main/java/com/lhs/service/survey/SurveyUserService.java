package com.lhs.service.survey;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.survey.CharacterTable;
import com.lhs.entity.survey.SurveyOperator;
import com.lhs.entity.survey.SurveyUser;
import com.lhs.entity.survey.SurveyUserStatistics;
import com.lhs.mapper.CharacterTableMapper;
import com.lhs.mapper.SurveyOperatorMapper;
import com.lhs.mapper.SurveyUserMapper;
import com.lhs.mapper.SurveyUserStatisticsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.web.config.QuerydslWebConfiguration;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SurveyUserService {


    private final SurveyUserMapper surveyUserMapper;

    private final RedisTemplate<String, String> redisTemplate;

    private final CharacterTableMapper characterTableMapper;

    private final SurveyOperatorMapper surveyOperatorMapper;

    private final SurveyUserStatisticsMapper surveyUserStatisticsMapper;

    public SurveyUserService(SurveyUserMapper surveyUserMapper, RedisTemplate<String, String> redisTemplate, CharacterTableMapper characterTableMapper, SurveyOperatorMapper surveyOperatorMapper, SurveyUserStatisticsMapper surveyUserStatisticsMapper) {
        this.surveyUserMapper = surveyUserMapper;
        this.redisTemplate = redisTemplate;
        this.characterTableMapper = characterTableMapper;
        this.surveyOperatorMapper = surveyOperatorMapper;
        this.surveyUserStatisticsMapper = surveyUserStatisticsMapper;
    }

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

        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name",userName);

        for (int i = 0; i < 5; i++) {
            if (i < 3) {
                userNameAndEnd = userName + randomEnd4(4); //用户id后四位后缀  #xxxx
                surveyUser = surveyUserMapper.selectOne(queryWrapper);
                if (surveyUser == null) break;  //未注册就跳出开始注册
                log.warn("id后缀重复");
            } else {
                userNameAndEnd = userName + randomEnd4(5);
                surveyUser = surveyUserMapper.selectOne(queryWrapper);
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

        Integer row = surveyUserMapper.insert(surveyUser);
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
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name",userName);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);  //查询用户
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



    public SurveyUser getSurveyUserById(String token){
        if(token==null||"undefined".equals(token)){
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
        Long id = decryptToken(token);
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",id);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper); //查询用户
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        return surveyUser;
    }

    public SurveyUser getSurveyUserByUid(Long uid){

        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid",uid);
        return surveyUserMapper.selectOne(queryWrapper);
    }



    public void updateSurveyUser(SurveyUser surveyUser){
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",surveyUser.getId());
        surveyUserMapper.update(surveyUser,queryWrapper);   //更新用户表
    }

    public List<Long> selectSurveyUserIds() {
        QueryWrapper<SurveyUserStatistics> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("operator_count",1);

        List<SurveyUserStatistics> surveyUserStatisticList = surveyUserStatisticsMapper.selectList(queryWrapper);
        List<Long> ids = new ArrayList<>();
        for (SurveyUserStatistics statistics : surveyUserStatisticList) {
            ids.add(statistics.getId());
        }
        return  ids;
    }


    public Integer getTableIndex(Long id){
        return 1;
    }

    public Map<String,Object> userStatistics(){
        List<Long> list = surveyUserMapper.selectSurveyUserIds();
        int insert = 0;
        for (Long id : list) {
            QueryWrapper<SurveyOperator> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("uid",id);
            Long count = surveyOperatorMapper.selectCount(queryWrapper);


            QueryWrapper<SurveyUserStatistics> userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper.eq("id",id);
            SurveyUserStatistics savedData = surveyUserStatisticsMapper.selectOne(userQueryWrapper);
            if(savedData==null){
                SurveyUserStatistics statistics = new SurveyUserStatistics();
                statistics.setId(id);
                statistics.setOperatorCount(count.intValue());
                insert+= surveyUserStatisticsMapper.insert(statistics);
            }else {
                savedData.setOperatorCount(count.intValue());
                surveyUserStatisticsMapper.update(savedData,userQueryWrapper);
            }

        }

        Map<String, Object> hashMap = new HashMap<>();
        return hashMap;
    }





    @RedisCacheable(key = "CharacterTableSimple",timeout = 3000)
    public Map<String, CharacterTable> getCharacterTable(){

        List<CharacterTable> characterTables = characterTableMapper.selectList(null);
        Map<String, CharacterTable> collect = characterTables.stream()
                .collect(Collectors.toMap(CharacterTable::getCharId, Function.identity()));


        return collect;
    }






}
