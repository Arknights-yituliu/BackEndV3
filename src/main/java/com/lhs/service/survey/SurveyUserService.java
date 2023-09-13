package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.*;
import com.lhs.entity.survey.SurveyOperator;
import com.lhs.entity.survey.SurveyUser;
import com.lhs.entity.survey.SurveyUserStatistics;

import com.lhs.mapper.survey.SurveyOperatorMapper;
import com.lhs.mapper.survey.SurveyUserMapper;
import com.lhs.mapper.survey.SurveyStatisticsUserMapper;
import com.lhs.vo.survey.SurveyUserVo;
import com.lhs.vo.survey.UserDataResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class SurveyUserService {


    private final SurveyUserMapper surveyUserMapper;

    private final RedisTemplate<String, String> redisTemplate;



    private final SurveyOperatorMapper surveyOperatorMapper;

    private final SurveyStatisticsUserMapper surveyStatisticsUserMapper;

    public SurveyUserService(SurveyUserMapper surveyUserMapper, RedisTemplate<String, String> redisTemplate,  SurveyOperatorMapper surveyOperatorMapper, SurveyStatisticsUserMapper surveyStatisticsUserMapper) {
        this.surveyUserMapper = surveyUserMapper;
        this.redisTemplate = redisTemplate;
        this.surveyOperatorMapper = surveyOperatorMapper;
        this.surveyStatisticsUserMapper = surveyStatisticsUserMapper;
    }

    /**
     * 调查表注册
     *
     * @param ipAddress ip
     * @param userName  用户id
     * @return 成功消息
     */
    public UserDataResponse register(String ipAddress, String userName) {
        checkUserName(userName);
        Date date = new Date();  //存入的时间
        String userNameAndEnd = null;
        SurveyUser surveyUser = null;

        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name",userName);

        for (int i = 0; i < 6; i++) {
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

        if (surveyUser != null) throw new ServiceException(ResultCode.USER_ID_TOO_MANY_TIMES);

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

        int row = surveyUserMapper.insert(surveyUser);
        if (row < 1) throw new ServiceException(ResultCode.SYSTEM_INNER_ERROR);

        long time = System.currentTimeMillis();

        String token = AES.encrypt(surveyUser.getUserName()+"."+id+"."+time, ApplicationConfig.Secret);

        UserDataResponse response = new UserDataResponse();
        response.setUserName(userNameAndEnd);
        response.setToken(token);
        response.setStatus(1);

        return response;
    }

    /**
     * 调查表登录
     * @param ipAddress  ip
     * @param surveyUserVo  用户信息
     * @return 成功消息
     */
    public UserDataResponse login(String ipAddress, SurveyUserVo surveyUserVo) {
        String userName = surveyUserVo.getUserName();
        String passWord = surveyUserVo.getPassWord();
        Map<String, Object> hashMap = new HashMap<>();

        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name",userName);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);  //查询用户

        if (surveyUser == null) throw new ServiceException(ResultCode.USER_ACCOUNT_NOT_EXIST);

        UserDataResponse response = new UserDataResponse();

//        if(surveyUser.getStatus()==2){
//            String encrypt = AES.encrypt(passWord, ApplicationConfig.Secret);
//            if(surveyUser.getPassWord()==null||(!surveyUser.getPassWord().equals(encrypt))){
//                throw new ServiceException(ResultCode.USER_NEED_PASSWORD);
//            }
//        }

        if (surveyUser.getStatus()<0) throw new ServiceException(ResultCode.USER_ACCOUNT_FORBIDDEN);


        long time = System.currentTimeMillis();
        Long id = surveyUser.getId();

        String token = AES.encrypt(surveyUser.getUserName()+"."+id+"."+time, ApplicationConfig.Secret);

        response.setUserName(userName);
        response.setToken(token);
        response.setStatus(surveyUser.getStatus());

        return response;
    }

    public Result<Object> retrievalAccountByCRED(String CRED) {

        JsonNode skLandPlayerBinding = getSKLandPlayerBinding(CRED);
        JsonNode list = skLandPlayerBinding.get("list");
        String uid = list.get(0).get("defaultUid").asText();
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid",uid);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
        if(surveyUser==null){
            throw new ServiceException(ResultCode.USER_ACCOUNT_NOT_BIND_UID);
        }
        UserDataResponse response = new UserDataResponse();
        response.setUserName(surveyUser.getUserName());
        return Result.success(response);
    }

    public Result<Object> retrievalAccount(String uid) {
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid",uid);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
        if(surveyUser==null){
            throw new ServiceException(ResultCode.USER_ACCOUNT_NOT_BIND_UID);
        }
        UserDataResponse response = new UserDataResponse();
        response.setUserName(surveyUser.getUserName());
        return Result.success(response);
    }


    public Result<Object> updateAccountStatus(SurveyUserVo surveyUserVo){
        Map<String,Object> hashMap = new HashMap<>();
        String token = surveyUserVo.getToken();
        String cred = surveyUserVo.getCred();
        String passWord = surveyUserVo.getPassWord();
        String userName = surveyUserVo.getUserName();

        SurveyUser bindAccount = getSurveyUserByToken(token);

        UserDataResponse response = new UserDataResponse();

        //获取玩家uid
        JsonNode PlayerBindingData = getSKLandPlayerBinding(cred);
        JsonNode list = PlayerBindingData.get("list");
        JsonNode bindingList = list.get(0).get("bindingList");
        String uid = bindingList.get(0).get("uid").asText();
        //查询这个uid是否有绑定一图流账号
        SurveyUser surveyUserByUid = getSurveyUserByUid(uid);

        //uid有绑定一图流账号，返回之前绑定过的账号
        if(surveyUserByUid!=null){
            if(!Objects.equals(surveyUserByUid.getId(), bindAccount.getId())){
                hashMap.put("userName",surveyUserByUid.getUserName());
                response.setUserName(userName);
                return Result.failure(ResultCode.USER_ACCOUNT_BIND_UID,response);
            }
        }


        //如果是改密码则先验证旧密码
        if(bindAccount.getStatus()==2){
            String oldPassWord = surveyUserVo.getOldPassWord();
           if(!Objects.equals(bindAccount.getPassWord(), AES.encrypt(oldPassWord, ApplicationConfig.Secret))){
               throw new ServiceException(ResultCode.USER_LOGIN_ERROR);
           }
        }


        //更新用户名和密码和账号状态
        checkPassWord(passWord);
        passWord = AES.encrypt(passWord,ApplicationConfig.Secret);
        bindAccount.setUserName(userName);
        bindAccount.setUid(uid);
        bindAccount.setPassWord(passWord);
        bindAccount.setStatus(2);

        //更新账号信息到数据库
        Long id = bindAccount.getId();
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",id);
        int update = surveyUserMapper.update(bindAccount, queryWrapper);
        response.setUserName(userName);
        response.setStatus(2);

        return Result.success(response);
    }




    public  Long decryptToken(String token){
        String decrypt = AES.decrypt(token.replaceAll(" +", "+"), ApplicationConfig.Secret);
        String idText = decrypt.split("\\.")[1];
        return Long.valueOf(idText);
    }


    public JsonNode getSKLandPlayerBinding(String cred){
        HashMap<String, String> header = new HashMap<>();
        cred = cred.trim();
        header.put("cred", cred);

        String SKLandPlayerBindingAPI = ApplicationConfig.SKLandPlayerBindingAPI;
        String SKLandPlayerBinding = HttpRequestUtil.get(SKLandPlayerBindingAPI, header);
        JsonNode SKLandPlayerBindingNode = JsonMapper.parseJSONObject(SKLandPlayerBinding);
        JsonNode data = SKLandPlayerBindingNode.get("data");
        int code = SKLandPlayerBindingNode.get("code").intValue();
        if (code != 0) throw new ServiceException(ResultCode.SKLAND_CRED_ERROR);
        return data;
    }


    public SurveyUser getSurveyUserByToken(String token){
        if(token==null||"undefined".equals(token)){
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
        Long id = decryptToken(token);
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",id);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper); //查询用户
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_ACCOUNT_NOT_EXIST);
        return surveyUser;
    }

    public SurveyUser getSurveyUserByUid(String uid){
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid",uid);
        return surveyUserMapper.selectOne(queryWrapper);
    }



    public void updateSurveyUser(SurveyUser surveyUser){
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",surveyUser.getId());
        surveyUser.setUpdateTime(new Date());
        surveyUserMapper.update(surveyUser,queryWrapper);   //更新用户表
    }

    public List<Long> selectSurveyUserIds() {
        QueryWrapper<SurveyUserStatistics> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("operator_count",1);

        List<SurveyUserStatistics> surveyUserStatisticList = surveyStatisticsUserMapper.selectList(queryWrapper);
        List<Long> ids = new ArrayList<>();
        for (SurveyUserStatistics statistics : surveyUserStatisticList) {
            ids.add(statistics.getId());
        }
        return  ids;
    }


    public Integer getTableIndex(Long id){
        return 1;
    }


    @Scheduled(cron = "0 0 0/1 * * ?")
    public void userStatistics(){
        List<SurveyUser> surveyUserList = surveyUserMapper.selectList(null);
        int insert = 0;
        for (SurveyUser surveyUser : surveyUserList) {
            long yituliuId = surveyUser.getId(); //一图流id

            String uid = surveyUser.getUid();
            if(uid==null) continue;
            if(uid.length()<10)
            if(uid.contains("delete")) continue;


            QueryWrapper<SurveyOperator> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("uid",yituliuId);
            Long count = surveyOperatorMapper.selectCount(queryWrapper);


            //查询是否统计过这个用户
            QueryWrapper<SurveyUserStatistics> userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper.eq("id",yituliuId);
            SurveyUserStatistics savedData = surveyStatisticsUserMapper.selectOne(userQueryWrapper);


            if(savedData==null){
                SurveyUserStatistics statistics = new SurveyUserStatistics();
                statistics.setId(yituliuId);
                statistics.setUid(uid);
                statistics.setOperatorCount(count.intValue());
                insert+= surveyStatisticsUserMapper.insert(statistics);
            }else {
                savedData.setUid(uid);
                savedData.setOperatorCount(count.intValue());
                surveyStatisticsUserMapper.update(savedData,userQueryWrapper);
            }

        }


    }





    private static void checkUserName(String userName){
        if(userName==null||userName.length()<2){
            throw new ServiceException(ResultCode.USER_NAME_LENGTH_TOO_SHORT);
        }

        if(userName.length()>20){
            throw new ServiceException(ResultCode.USER_NAME_LENGTH_TOO_LONG);
        }

        for (int i = 0; i < userName.length(); i++) {
            String substring = userName.substring(i, i + 1);
            if (substring.matches("[\u4e00-\u9fa5]+")){
//                System.out.println("中文: "+substring);
            } else if (substring.matches("[a-zA-Z\0-9]+")) {
//                System.out.println("英文 ：" + substring);
            } else {
//                System.out.println("其他字符："+substring);
                throw new ServiceException(ResultCode.USER_NAME_MUST_BE_IN_CHINESE_OR_ENGLISH);
            }
        }
    }

    private static void checkPassWord(String passWord){
        if(passWord==null||passWord.length()<6){
            throw new ServiceException(ResultCode.PASS_WORD_LENGTH_TOO_SHORT);
        }

        if(passWord.length()>20){
            throw new ServiceException(ResultCode.PASS_WORD_LENGTH_TOO_LONG);
        }

        for (int i = 0; i < passWord.length(); i++) {
            String substring = passWord.substring(i, i + 1);
            if (substring.matches("[a-zA-Z\0-9]+")) {
//                System.out.println("英文 ：" + substring);
            } else {
//                System.out.println("其他字符："+substring);
                throw new ServiceException(ResultCode.PASS_WORD_MUST_BE_IN_CHINESE_OR_ENGLISH);
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


}
