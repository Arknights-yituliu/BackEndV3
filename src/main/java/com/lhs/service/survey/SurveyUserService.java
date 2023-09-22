package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.*;
import com.lhs.entity.survey.SurveyOperator;
import com.lhs.entity.survey.SurveyUser;
import com.lhs.entity.survey.SurveyStatisticsUser;

import com.lhs.mapper.survey.SurveyOperatorMapper;
import com.lhs.mapper.survey.SurveyUserMapper;
import com.lhs.mapper.survey.SurveyStatisticsUserMapper;
import com.lhs.service.dev.EmailService;
import com.lhs.service.dev.OSSService;
import com.lhs.vo.survey.SurveyRequestVo;
import com.lhs.vo.survey.UserDataResponse;
import com.lhs.vo.user.EmailRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SurveyUserService {


    private final SurveyUserMapper surveyUserMapper;

    private final RedisTemplate<String, String> redisTemplate;

    private final SurveyOperatorMapper surveyOperatorMapper;

    private final SurveyStatisticsUserMapper surveyStatisticsUserMapper;

    private final EmailService emailService;

    private final OSSService ossService;

    public SurveyUserService(SurveyUserMapper surveyUserMapper, RedisTemplate<String, String> redisTemplate, SurveyOperatorMapper surveyOperatorMapper, SurveyStatisticsUserMapper surveyStatisticsUserMapper, EmailService emailService, OSSService ossService) {
        this.surveyUserMapper = surveyUserMapper;
        this.redisTemplate = redisTemplate;
        this.surveyOperatorMapper = surveyOperatorMapper;
        this.surveyStatisticsUserMapper = surveyStatisticsUserMapper;
        this.emailService = emailService;
        this.ossService = ossService;
    }

    /**
     * 调查站注册
     * @param ipAddress ip
     * @param userName  用户id
     * @return 用户状态信息
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

        int row = surveyUserMapper.save(surveyUser);
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
     * 通过森空岛cred登录
     * @param ipAddress  ip地址
     * @param surveyRequestVo  自定义的请求实体类（具体内容看实体类的注释）
     * @return 用户状态信息
     */
    public Result<UserDataResponse> loginByCRED(String ipAddress, SurveyRequestVo surveyRequestVo) {
        UserDataResponse response = new UserDataResponse();
        Date date = new Date();
        long time = date.getTime();

        String uid = surveyRequestVo.getUid();
        String nickName = surveyRequestVo.getNickName();

        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
         queryWrapper.eq("uid", uid);
        SurveyUser surveyUserByUid = surveyUserMapper.selectOne(queryWrapper);
        if(surveyUserByUid!=null){
            response.setUserName(surveyUserByUid.getUserName());
            String token = AES.encrypt(surveyUserByUid.getUserName()+"."+surveyUserByUid.getId()+"."+time, ApplicationConfig.Secret);
            response.setToken(token);
            response.setStatus(surveyUserByUid.getStatus());
            surveyUserByUid.setNickName(nickName);
            updateSurveyUser(surveyUserByUid);
            return Result.success(response);

        }


        String idStr = date.getTime()+randomEnd4_id();
        long id = Long.parseLong(idStr);


        String[] split = nickName.split("#");
        String userName = split[0]+ randomEnd4(4);

        for (int i = 0; i < 6; i++) {
            QueryWrapper<SurveyUser> queryWrapperNoExist = new QueryWrapper<>();
            queryWrapperNoExist.eq("user_name", userName);
            SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapperNoExist);
            if(surveyUser==null){
                break;
            }else {
                userName = split[0]+ randomEnd4(4);
            }
        }

        SurveyUser surveyUser = new SurveyUser();
        surveyUser.setId(id);
        surveyUser.setNickName(nickName);
        surveyUser.setIp(ipAddress);
        surveyUser.setUserName(userName);
        surveyUser.setUid(uid);
        surveyUser.setStatus(1);
        surveyUser.setCreateTime(date);
        surveyUser.setUpdateTime(new Date(time+3600));

        String token = AES.encrypt(userName+"."+id+"."+time, ApplicationConfig.Secret);

        surveyUserMapper.save(surveyUser);


        response.setUserName(surveyUser.getUserName());
        response.setToken(token);
        response.setStatus(surveyUser.getStatus());



        return Result.success(response);
    }

    /**
     * 调查表登录
     * @param ipAddress  ip
     * @param surveyRequestVo  用户信息
     * @return 用户状态信息
     */
    public UserDataResponse login(String ipAddress, SurveyRequestVo surveyRequestVo) {
        String userName = surveyRequestVo.getUserName();
        String passWord = surveyRequestVo.getPassWord();
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

    /**
     * 通过森空岛CRED找回账号
     * @param CRED 森空岛CRED
     * @return 用户状态信息
     */
    public Result<UserDataResponse> retrievalAccountByCRED(String CRED) {

        Map<String, String> skLandPlayerBinding = getSKLandPlayerBinding(CRED);
        String uid = skLandPlayerBinding.get("uid");

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

    /**
     * 发送邮件验证码
     * @param surveyRequestVo  自定义的请求实体类（具体内容看实体类的注释）
     * @return
     */
    public Result<Object> sendEmailCode(SurveyRequestVo surveyRequestVo) {
        String email = surveyRequestVo.getEmail();
        String token = surveyRequestVo.getToken();
        getSurveyUserByToken(token);


        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setFrom("1820702789@qq.com");
        emailRequest.setTo(email);
        emailRequest.setSubject("一图流账号绑定邮箱");
        int random = new Random().nextInt(999999);
        String code = String.format("%6s", random).replace(" ", "0");
        redisTemplate.opsForValue().set("CODE:CODE." + email , code, 300, TimeUnit.SECONDS);
        emailRequest.setText("本次绑定验证码：" + code);
        emailService.sendMail(emailRequest);
        return Result.success();
    }

    /**
     * 更新或绑定邮箱
     * @param surveyRequestVo 自定义的请求实体类（具体内容看实体类的注释）
     * @return
     */
    public Result<Object> updateOrBindEmail(SurveyRequestVo surveyRequestVo) {
        String email = surveyRequestVo.getEmail();
        String token = surveyRequestVo.getToken();
        String verificationCode = surveyRequestVo.getEmailCode();
        SurveyUser surveyUserByToken = getSurveyUserByToken(token);

        String code = redisTemplate.opsForValue().get("CODE:CODE." + email);
        if(code == null) {
            throw new ServiceException(ResultCode.CODE_NOT_EXIST);
        }
        if(!verificationCode.equals(code)){
            throw new ServiceException(ResultCode.CODE_ERROR);
        }

        surveyUserByToken.setEmail(email);
        updateSurveyUser(surveyUserByToken);
        return Result.success();
    }

    /**
     * 更新密码
     * @param surveyRequestVo  自定义的请求实体类（具体内容看实体类的注释）
     * @return
     */
    public Result<Object> updatePassWord(SurveyRequestVo surveyRequestVo){
        String token = surveyRequestVo.getToken();
        String passWord = surveyRequestVo.getPassWord();
        checkPassWord(passWord);

        SurveyUser surveyUserByToken = getSurveyUserByToken(token);

        surveyUserByToken.setPassWord(passWord);
        surveyUserByToken.setStatus(2);
        updateSurveyUser(surveyUserByToken);
        return Result.success();
    }


    /**
     * 通过cred进行身份验证
     * @param surveyRequestVo 自定义的请求实体类（具体内容看实体类的注释）
     * @return
     */
    public Result<UserDataResponse> authentication(SurveyRequestVo surveyRequestVo) {
        String token = surveyRequestVo.getToken();
        SurveyUser surveyUser = getSurveyUserByToken(token);
        String cred = surveyRequestVo.getCred();
        Map<String, String> skLandPlayerBinding = getSKLandPlayerBinding(cred);
        String uid = skLandPlayerBinding.get("uid");
        String nickName = skLandPlayerBinding.get("nickName");
        if(!surveyUser.getUid().equals(uid)){
            throw new ServiceException(ResultCode.USER_ACCOUNT_NOT_BIND_UID);
        }

        surveyUser.setNickName(nickName);

        UserDataResponse response = new UserDataResponse();
        response.setStatus(surveyUser.getStatus());
        response.setUserName(surveyUser.getUserName());
        response.setUid(uid);
        response.setNickName(nickName);
        return Result.success(response);
    }


    /**
     * 获得森空岛绑定信息
     * @param CRED 森空岛CRED
     * @return
     */
    public Map<String, String> getSKLandPlayerBinding(String CRED){
        HashMap<String, String> header = new HashMap<>();
        CRED = CRED.trim();
        header.put("cred", CRED);

        String SKLandPlayerBindingAPI = ApplicationConfig.SKLandPlayerBindingAPI;
        String SKLandPlayerBinding = HttpRequestUtil.get(SKLandPlayerBindingAPI, header);
        JsonNode SKLandPlayerBindingNode = JsonMapper.parseJSONObject(SKLandPlayerBinding);
        int code = SKLandPlayerBindingNode.get("code").intValue();
        if (code != 0) throw new ServiceException(ResultCode.SKLAND_CRED_ERROR);
        JsonNode data = SKLandPlayerBindingNode.get("data");

        JsonNode list = data.get("list");
        JsonNode bindingList = list.get(0).get("bindingList");
        String uid = "";
        String nickName = "";
        for (int i = 0; i < bindingList.size(); i++) {
            if(bindingList.get(i).get("isDefault").asBoolean()){
                uid = bindingList.get(i).get("uid").asText();
                nickName = bindingList.get(i).get("nickName").asText();
            }
        }

        if("".equals(uid)){
            for (int i = 0; i < bindingList.size(); i++) {
                if(bindingList.get(i).get("isOfficial").asBoolean()){
                    uid = bindingList.get(i).get("uid").asText();
                    nickName = bindingList.get(i).get("nickName").asText();
                }
            }
        }

        if("".equals(uid)){
            uid = bindingList.get(0).get("uid").asText();
            nickName = bindingList.get(0).get("nickName").asText();
        }

        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("uid",uid);
        hashMap.put("nickName",nickName);

        return hashMap;
    }

    /**
     * 通过用户凭证查找用户信息
     * @param token 用户凭证
     * @return 用户信息
     */
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

    /**
     * 通过游戏内的玩家uid查找用户信息
     * @param uid 游戏内的玩家uid
     * @return 用户信息
     */
    public SurveyUser getSurveyUserByUid(String uid){
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid",uid);
        return surveyUserMapper.selectOne(queryWrapper);
    }

    /**
     * 更新用户信息
     * @param surveyUser 用户信息
     */
    public void updateSurveyUser(SurveyUser surveyUser){
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",surveyUser.getId());
        surveyUser.setUpdateTime(new Date());
        surveyUserMapper.update(surveyUser,queryWrapper);   //更新用户表

        Long id = surveyUser.getId();
        ossService.upload(JsonMapper.toJSONString(surveyUser),"survey/user/info/"+id+".json");
    }



    public List<Long> selectSurveyUserIds() {
        QueryWrapper<SurveyStatisticsUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("operator_count",1);

        List<SurveyStatisticsUser> surveyUserStatisticList = surveyStatisticsUserMapper.selectList(queryWrapper);
        List<Long> ids = new ArrayList<>();
        for (SurveyStatisticsUser statistics : surveyUserStatisticList) {
            ids.add(statistics.getId());
        }
        return  ids;
    }


    public Integer getTableIndex(Long id){
        return 1;
    }


//    @Scheduled(cron = "0 0 0/2 * * ?")
    public void userStatistics(){
        List<SurveyUser> surveyUserList = surveyUserMapper.selectList(null);


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
            QueryWrapper<SurveyStatisticsUser> userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper.eq("id",yituliuId);
            SurveyStatisticsUser savedData = surveyStatisticsUserMapper.selectOne(userQueryWrapper);


            if(savedData==null){
                SurveyStatisticsUser statistics = new SurveyStatisticsUser();
                statistics.setId(yituliuId);
                statistics.setUid(uid);
                statistics.setOperatorCount(count.intValue());

            }else {
                savedData.setUid(uid);
                savedData.setOperatorCount(count.intValue());
                surveyStatisticsUserMapper.update(savedData,userQueryWrapper);
            }

        }


    }

    /**
     * 解密用户凭证
     * @param token 用户凭证
     * @return
     */
    private Long decryptToken(String token){
        String decrypt = AES.decrypt(token.replaceAll(" +", "+"), ApplicationConfig.Secret);

        String idText = decrypt.split("\\.")[1];
        return Long.valueOf(idText);
    }

    /**
     * 检查用户名是否为中文，英文，数字
     * @param userName
     */
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

    /**
     * 检查密码是否为英文，数字
     * @param passWord
     */
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
