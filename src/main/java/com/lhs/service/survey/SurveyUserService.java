package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RateLimiter;
import com.lhs.common.entity.Result;
import com.lhs.common.entity.ResultCode;
import com.lhs.common.util.UserStatus;
import com.lhs.common.util.UserStatusCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.*;
import com.lhs.entity.dto.survey.EmailDto;
import com.lhs.entity.dto.survey.UpdateUserDataDto;
import com.lhs.entity.dto.survey.UserDataDto;
import com.lhs.entity.dto.survey.SklandDto;
import com.lhs.entity.po.survey.SurveyOperator;
import com.lhs.entity.po.survey.SurveyUser;
import com.lhs.entity.po.survey.SurveyStatisticsUser;

import com.lhs.mapper.survey.SurveyOperatorMapper;
import com.lhs.mapper.survey.SurveyUserMapper;
import com.lhs.mapper.survey.SurveyStatisticsUserMapper;
import com.lhs.service.dev.EmailService;
import com.lhs.service.dev.OSSService;

import com.lhs.entity.vo.survey.UserDataResponse;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

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


    public UserDataResponse registerV2(String ipAddress, UserDataDto surveyRequestVo) {
        String accountType = surveyRequestVo.getAccountType();
        String userName = surveyRequestVo.getUserName().trim();
        String passWord = surveyRequestVo.getPassWord().trim();
        String email = surveyRequestVo.getEmail().trim();

        if (accountType == null || "undefined".equals(accountType) || "null".equals(accountType)) {
            throw new ServiceException(ResultCode.PARAM_TYPE_BIND_ERROR);
        }


        checkPassWord(passWord);

        Date date = new Date();  //当前时间
        String idStr = date.getTime() + randomEnd4_id();
        long yituliuId = Long.parseLong(idStr);   //一图流id 当前时间戳加随机4位数字

        int status = UserStatus.addPermission(1, UserStatusCode.HAS_PASSWORD);


        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();

        passWord = AES.encrypt(passWord, ApplicationConfig.Secret);

        SurveyUser surveyUserNew = SurveyUser.builder()  //新用户信息
                .id(yituliuId)
                .passWord(passWord)
                .ip(ipAddress)
                .createTime(date)
                .updateTime(date)
                .build();

        if ("passWord".equals(accountType)) {
            Log.info("密码登录");
            checkUserName(userName);
            queryWrapper.eq("user_name", userName);
            SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
            if (surveyUser != null) throw new ServiceException(ResultCode.USER_EXISTED);
            surveyUserNew.setUserName(userName);
        }

        if ("emailCode".equals(accountType)) {
            Log.info("邮箱登录");
            queryWrapper.eq("email", email);
            SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
            if (surveyUser != null) throw new ServiceException(ResultCode.USER_EXISTED);

            String redisCode = redisTemplate.opsForValue().get("CODE:CODE." + email);
            String emailCode = surveyRequestVo.getEmailCode();
            //检查验证码
            if (redisCode == null) {
                throw new ServiceException(ResultCode.CODE_NOT_EXIST);
            }
            if (!emailCode.equals(redisCode)) {
                throw new ServiceException(ResultCode.CODE_ERROR);
            }

            status = UserStatus.addPermission(status, UserStatusCode.HAS_EMAIL);

            userName = "博士" + getSuffix(7);
            surveyUserNew.setUserName(userName);
            surveyUserNew.setEmail(email);
        }

        surveyUserNew.setStatus(status);

        System.out.println(surveyUserNew);

        int row = surveyUserMapper.save(surveyUserNew);
        if (row < 1) throw new ServiceException(ResultCode.SYSTEM_INNER_ERROR);


        long timeStamp = date.getTime();

        //用户凭证  由用户部分信息+一图流id+时间戳 加密得到
        Map<Object, Object> hashMap = new HashMap<>();
        hashMap.put("userName", surveyUserNew.getUserName());
        String header = JsonMapper.toJSONString(hashMap);
        String token = AES.encrypt(header + "." + yituliuId + "." + timeStamp, ApplicationConfig.Secret);

        UserDataResponse response = new UserDataResponse();  //返回的用户信息实体类  包括凭证，用户名，用户状态等
        response.setUserName(surveyUserNew.getUserName());
        response.setToken(token);
        response.setStatus(surveyUserNew.getStatus());
        response.setEmail(surveyUserNew.getEmail());

        return response;
    }

    

    /**
     * 调查站登录
     *
     * @param ipAddress       ip
     * @param userDataDto 自定义的请求实体类（具体内容看实体类的注释）
     * @return 用户状态信息
     */
    public UserDataResponse loginV2(String ipAddress, UserDataDto userDataDto) {
        String accountType = userDataDto.getAccountType();
        String userName = userDataDto.getUserName().trim();
        String passWord = userDataDto.getPassWord().trim();
        String email = userDataDto.getEmail().trim();
        String emailCode = userDataDto.getEmailCode().trim();

        if (accountType == null || "undefined".equals(accountType) || "null".equals(accountType)) {
            throw new ServiceException(ResultCode.PARAM_TYPE_BIND_ERROR);
        }

        passWord = AES.encrypt(passWord, ApplicationConfig.Secret);

        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();

        boolean allowLogin = false;

        if ("passWord".equals(accountType)) {
            if (userName.contains("@")) {
                queryWrapper.eq("email", userName);
            } else {
                queryWrapper.eq("user_name", userName);
            }
        }

        if ("emailCode".equals(accountType)) {
            queryWrapper.eq("email", email);
        }

        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);

        if ("passWord".equals(accountType)) {
            Log.info("密码登录");
            if (UserStatus.hasPermission(surveyUser.getStatus(), UserStatusCode.HAS_PASSWORD)) {
                if (!surveyUser.getPassWord().equals(passWord)) {
                    throw new ServiceException(ResultCode.USER_LOGIN_ERROR);
                }
            }
            allowLogin = true;
        }

        if ("emailCode".equals(accountType)) {
            Log.info("邮箱登录");
            String code = redisTemplate.opsForValue().get("CODE:CODE." + email);
            //检查验证码
            if (code == null) {
                throw new ServiceException(ResultCode.CODE_NOT_SEND);
            }
            if (emailCode.equals(code)) {
                allowLogin = true;
            }else {
                throw new ServiceException(ResultCode.CODE_ERROR);
            }
        }

        if (!allowLogin) throw new ServiceException(ResultCode.USER_LOGIN_ERROR);

        long timeStamp = System.currentTimeMillis();
        Long yituliuId = surveyUser.getId();  //一图流id

        //用户凭证  由用户部分信息+一图流id+时间戳 加密得到
        Map<Object, Object> hashMap = new HashMap<>();
        hashMap.put("userName", surveyUser.getUserName());
        String header = JsonMapper.toJSONString(hashMap);
        String token = AES.encrypt(header + "." + yituliuId + "." + timeStamp, ApplicationConfig.Secret);

        UserDataResponse response = new UserDataResponse();  //返回的用户信息实体类  包括凭证，用户名，用户状态等
        response.setUserName(surveyUser.getUserName());
        response.setToken(token);
        response.setStatus(surveyUser.getStatus());
        response.setEmail(surveyUser.getEmail());

        return response;

    }

    /**
     * 设置注册验证码的邮件信息
     * @param emailDto 邮件信息
     */
    public void sendEmailForRegister(EmailDto emailDto){
        String emailAddress = emailDto.getEmail().trim();
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", emailAddress);
        SurveyUser surveyUserByEmail = surveyUserMapper.selectOne(queryWrapper);

        //判断是否注册过
        if (surveyUserByEmail != null) throw new ServiceException(ResultCode.USER_EXISTED);
        String code = emailService.CreateVerificationCode(emailAddress, 9999);
        String subject = "一图流注册验证码";
        String text = "您本次注册验证码： " + code +",验证码有效时间5分钟";
        sendEmailCode(emailAddress,subject,text);
    }

    /**
     * 设置登录验证码的邮件信息
     * @param emailDto 邮件信息
     */
    public void sendEmailForLogin(EmailDto emailDto){
        String emailAddress = emailDto.getEmail().trim();
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", emailAddress);
        SurveyUser surveyUserByEmail = surveyUserMapper.selectOne(queryWrapper);

        //判断用户是否绑定过邮箱
        if(surveyUserByEmail == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        String code = emailService.CreateVerificationCode(emailAddress, 9999);
        String subject = "一图流登录验证码";
        String text = "您本次登录验证码：" + code+",验证码有效时间5分钟";
        sendEmailCode(emailAddress,subject,text);
    }

    /**
     * 设置修改邮箱验证码的邮件信息
     * @param emailDto 邮件信息
     */
    public void sendEmailForChangeEmail(EmailDto emailDto){
        String emailAddress = emailDto.getEmail().trim();
        String token = emailDto.getToken();  //用户凭证
        //变更邮箱
        SurveyUser surveyUserByToken = getSurveyUserByToken(token);
        //判断是否绑定过邮箱
        if (UserStatus.hasPermission(surveyUserByToken.getStatus(), UserStatusCode.HAS_EMAIL)) {
            emailAddress = surveyUserByToken.getEmail();
        }

        String code = emailService.CreateVerificationCode(emailAddress, 9999);
        String subject = "一图流邮箱变更验证码";
        String text = "您本次变更邮箱验证码：" + code+",验证码有效时间5分钟";
        sendEmailCode(emailAddress,subject,text);
    }

    /**
     * 发送验证码
     * @param emailAddress 要发送的邮箱地址
     * @param subject  标题
     * @param text  内容
     */
    @RateLimiter(key = "SurveyEmailRateLimit")
    public void sendEmailCode(String emailAddress,String subject,String text) {

        Long daySendingLimit = redisTemplate.opsForValue().increment("OuterServiceMaximumLimit");
        daySendingLimit = daySendingLimit==null?1L:daySendingLimit;
        if(daySendingLimit>50000){
            throw new ServiceException(ResultCode.INTERFACE_DAILY_SENDING_LIMIT);
        }

        emailService.singleSendMail(emailAddress,text,subject);

//        //注册
//
//
//        //登录
//        if ("login".equals(type)) {
//            //判断用户是否绑定过邮箱
//            if(surveyUserByEmail ==null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
//            subject = "一图流登录验证码";
//            text = "您本次登录验证码：" + code+",验证码有效时间5分钟";
//        }
//
//        //变更邮箱
//        if ("changeEmail".equals(type)) {
//            subject = "一图流邮箱变更验证码";
//            text = "您本次变更邮箱验证码：" + code+",验证码有效时间5分钟";
//            SurveyUser surveyUserByToken = getSurveyUserByToken(token);
//            //判断是否绑定过邮箱
//            if (UserStatus.hasPermission(surveyUserByToken.getStatus(), UserStatusCode.HAS_EMAIL)) {
//                toAddress = surveyUserByToken.getEmail();
//            }
//        }



    }


    /**
     * 更新或绑定邮箱
     *
     * @param updateUserDataDto 自定义的请求实体类（具体内容看实体类的注释）
     * @return 成功信息
     */
    public UserDataResponse updateOrBindEmail(UpdateUserDataDto updateUserDataDto) {
        String email = updateUserDataDto.getEmail().trim();
        String token = updateUserDataDto.getToken();
        String emailCode = updateUserDataDto.getEmailCode().trim();

        SurveyUser surveyUserByToken = getSurveyUserByToken(token);
        String code = redisTemplate.opsForValue().get("CODE:CODE." + email);
        //检查验证码
        if (code == null) {
            throw new ServiceException(ResultCode.CODE_NOT_EXIST);
        }
        if (emailCode.equals(code)) {
            surveyUserByToken.setEmail(email);
            if (!UserStatus.hasPermission(surveyUserByToken.getStatus(), UserStatusCode.HAS_EMAIL)) {
                surveyUserByToken.setStatus(UserStatus.addPermission(surveyUserByToken.getStatus(), UserStatusCode.HAS_EMAIL));
            }
        }else {
            throw new ServiceException(ResultCode.CODE_ERROR);
        }



        updateSurveyUser(surveyUserByToken);
        UserDataResponse response = new UserDataResponse();
        response.setStatus(surveyUserByToken.getStatus());

        return response;
    }

    /**
     * 更新密码
     *
     * @param updateUserDataDto 自定义的请求实体类（具体内容看实体类的注释）
     * @return
     */
    public UserDataResponse updatePassWord(UpdateUserDataDto updateUserDataDto) {

        String token = updateUserDataDto.getToken();
        String newPassWord = updateUserDataDto.getNewPassWord().trim(); //新密码


        SurveyUser surveyUserByToken = getSurveyUserByToken(token); //用户信息



        checkPassWord(newPassWord);  //检查新密码格式

        newPassWord = AES.encrypt(newPassWord, ApplicationConfig.Secret);  //加密新密码

        Integer status = surveyUserByToken.getStatus();

        if (UserStatus.hasPermission(status, UserStatusCode.HAS_PASSWORD)) {
            //替换旧密码
            String oldPassWord = updateUserDataDto.getOldPassWord().trim(); //旧密码
            checkPassWord(oldPassWord);  //检查旧密码
            oldPassWord = AES.encrypt(oldPassWord, ApplicationConfig.Secret);
            if (surveyUserByToken.getPassWord().equals(oldPassWord)) {  //检查旧密码是否正确
                surveyUserByToken.setPassWord(newPassWord);  //更新旧密码为新密码
            } else {
                throw new ServiceException(ResultCode.USER_PASSWORD_ERROR);
            }
        } else {
            //设置新密码
            status = UserStatus.addPermission(status, UserStatusCode.HAS_PASSWORD);
            surveyUserByToken.setPassWord(newPassWord);
            surveyUserByToken.setStatus(status);
        }

        updateSurveyUser(surveyUserByToken);

        UserDataResponse response = new UserDataResponse();
        response.setStatus(surveyUserByToken.getStatus());

        return response;
    }


    public UserDataResponse updateUserName(UpdateUserDataDto updateUserDataDto) {
        String token = updateUserDataDto.getToken();
        String userName = updateUserDataDto.getUserName().trim();

        SurveyUser surveyUserByToken = getSurveyUserByToken(token);
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name",userName);
        if(surveyUserMapper.selectOne(queryWrapper)!= null) throw new ServiceException(ResultCode.USER_EXISTED);

        if(UserStatus.hasPermission(surveyUserByToken.getStatus(),UserStatusCode.HAS_PASSWORD)||
                UserStatus.hasPermission(surveyUserByToken.getStatus(),UserStatusCode.HAS_EMAIL)){
            surveyUserByToken.setUserName("山桜");
            updateSurveyUser(surveyUserByToken);
        }else {
            throw new ServiceException(ResultCode.NOT_SET_PASSWORD_OR_BIND_EMAIL);
        }

        UserDataResponse response = new UserDataResponse();
        response.setUserName(surveyUserByToken.getUserName());
        return response;

    }

    /**
     * 通过cred进行身份验证
     *
     * @param sklandDto 自定义的请求实体类（具体内容看实体类的注释）
     * @return
     */
    public UserDataResponse authentication(SklandDto sklandDto) {
        String token = sklandDto.getToken();
        String cred = sklandDto.getCred();
        SurveyUser surveyUser = getSurveyUserByToken(token);

        Map<String, String> skLandPlayerBinding = getSKLandPlayerBinding(cred);
        String uid = skLandPlayerBinding.get("uid");
        String nickName = skLandPlayerBinding.get("nickName");
        if (!surveyUser.getUid().equals(uid)) {
            throw new ServiceException(ResultCode.USER_NOT_BIND_UID);
        }


        UserDataResponse response = new UserDataResponse();
        response.setStatus(surveyUser.getStatus());
        response.setUserName(surveyUser.getUserName());
        response.setUid(uid);
        response.setNickName(nickName);
        return response;
    }


    /**
     * 获得森空岛绑定信息
     *
     * @param CRED 森空岛CRED
     * @return
     */
    public Map<String, String> getSKLandPlayerBinding(String CRED) {
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
            if (bindingList.get(i).get("isDefault").asBoolean()) {
                uid = bindingList.get(i).get("uid").asText();
                nickName = bindingList.get(i).get("nickName").asText();
            }
        }

        if ("".equals(uid)) {
            for (int i = 0; i < bindingList.size(); i++) {
                if (bindingList.get(i).get("isOfficial").asBoolean()) {
                    uid = bindingList.get(i).get("uid").asText();
                    nickName = bindingList.get(i).get("nickName").asText();
                }
            }
        }

        if ("".equals(uid)) {
            uid = bindingList.get(0).get("uid").asText();
            nickName = bindingList.get(0).get("nickName").asText();
        }

        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("uid", uid);
        hashMap.put("nickName", nickName);

        return hashMap;
    }

    /**
     * 通过用户凭证查找用户信息
     *
     * @param token 用户凭证
     * @return 用户信息
     */
    public SurveyUser getSurveyUserByToken(String token) {
        if (token == null || "undefined".equals(token)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
        Long yituliuId = decryptToken(token);
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", yituliuId);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper); //查询用户
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        if (surveyUser.getStatus() < 0) throw new ServiceException(ResultCode.USER_FORBIDDEN);
        return surveyUser;
    }

    /**
     * 通过邮箱找用户信息
     *
     * @param email 用户绑定邮箱
     * @return 用户信息
     */
    public SurveyUser getSurveyUserByEmail(String email) {
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper); //查询用户
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        if (surveyUser.getStatus() < 0) throw new ServiceException(ResultCode.USER_FORBIDDEN);
        return surveyUser;
    }

    /**
     * 通过游戏内的玩家uid查找用户信息
     *
     * @param uid 游戏内的玩家uid
     * @return 用户信息
     */
    public SurveyUser getSurveyUserByUid(String uid) {
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", uid);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        if (surveyUser.getStatus() < 0) throw new ServiceException(ResultCode.USER_FORBIDDEN);
        return surveyUser;
    }

    /**
     * 更新用户信息
     *
     * @param surveyUser 用户信息
     */
    public void updateSurveyUser(SurveyUser surveyUser) {
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", surveyUser.getId());
        surveyUser.setUpdateTime(new Date());
        surveyUserMapper.update(surveyUser, queryWrapper);   //更新用户表

        Long id = surveyUser.getId();
        ossService.upload(JsonMapper.toJSONString(surveyUser), "survey/user/info/" + id + ".json");
    }

    /**
     * 获取过滤后的有效调查者的一图流id
     *
     * @return
     */
    public List<Long> selectSurveyUserIds() {
        QueryWrapper<SurveyStatisticsUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("operator_count", 1);

        List<SurveyStatisticsUser> surveyUserStatisticList = surveyStatisticsUserMapper.selectList(queryWrapper);
        List<Long> ids = new ArrayList<>();
        for (SurveyStatisticsUser statistics : surveyUserStatisticList) {
            ids.add(statistics.getId());
        }
        return ids;
    }


    public Integer getTableIndex(Long id) {
        return 1;
    }


    //    @Scheduled(cron = "0 0 0/2 * * ?")
    public void userStatistics() {
        List<SurveyUser> surveyUserList = surveyUserMapper.selectList(null);

        for (SurveyUser surveyUser : surveyUserList) {
            long yituliuId = surveyUser.getId(); //一图流id

            QueryWrapper<SurveyOperator> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("uid", yituliuId);
            Long count = surveyOperatorMapper.selectCount(queryWrapper);
            if (count < 10) continue;

            //查询是否统计过这个用户
            QueryWrapper<SurveyStatisticsUser> userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper.eq("id", yituliuId);
            SurveyStatisticsUser savedData = surveyStatisticsUserMapper.selectOne(userQueryWrapper);

            SurveyStatisticsUser statistics = SurveyStatisticsUser
                    .builder()
                    .id(surveyUser.getId())
                    .uid(surveyUser.getUid())
                    .email(surveyUser.getEmail())
                    .userName(surveyUser.getUserName())
                    .passWord(surveyUser.getPassWord())
                    .createTime(surveyUser.getCreateTime())
                    .operatorCount(count.intValue())
                    .status(surveyUser.getStatus())
                    .build();

            if (savedData == null) {
                surveyStatisticsUserMapper.insert(statistics);
            } else {
                surveyStatisticsUserMapper.update(statistics, userQueryWrapper);
            }

        }


    }

    /**
     * 解密用户凭证
     *
     * @param token 用户凭证
     * @return
     */
    private Long decryptToken(String token) {
        String decrypt = AES.decrypt(token.replaceAll(" +", "+"), ApplicationConfig.Secret);

        String idText = decrypt.split("\\.")[1];
        return Long.valueOf(idText);
    }

    /**
     * 检查用户名是否为中文，英文，数字
     *
     * @param userName 用户名
     */
    private static void checkUserName(String userName) {
        if (userName == null || userName.length() < 2) {
            throw new ServiceException(ResultCode.USER_NAME_LENGTH_TOO_SHORT);
        }

        if (userName.length() > 20) {
            throw new ServiceException(ResultCode.USER_NAME_LENGTH_TOO_LONG);
        }

        for (int i = 0; i < userName.length(); i++) {
            String substring = userName.substring(i, i + 1);
            if (substring.matches("[\u4e00-\u9fa5]+")) {
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
     *
     * @param passWord 密码
     */
    private static void checkPassWord(String passWord) {
        if (passWord == null || passWord.length() < 6) {
            throw new ServiceException(ResultCode.PASS_WORD_LENGTH_TOO_SHORT);
        }

        if (passWord.length() > 20) {
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


    private String getSuffix(Integer digit) {
        int random = new Random().nextInt(9999);
        String end4 = String.format("%0" + digit + "d", random);
        return "#" + end4;
    }

    private String randomEnd4_id() {
        int random = new Random().nextInt(99);
        return String.format("%02d", random);
    }


    /**
     * 调查站注册
     *
     * @param ipAddress       ip
     * @param surveyRequestVo 自定义的请求实体类（具体内容看实体类的注释）
     * @return 用户状态信息
     */

//    public UserDataResponse register(String ipAddress, SurveyRequestVo surveyRequestVo) {
//        String userName = surveyRequestVo.getUserName();
//        String passWord = surveyRequestVo.getPassWord();
//        checkUserName(userName);
//        Date date = new Date();  //当前时间
//        String userNameAndSuffix = userName + getSuffix(4); //游客注册的用户名可重复，但是有后缀，格式：用户名#xxxx
//        SurveyUser surveyUserExist = null;  //是否存在的旧用户信息
//
//        String idStr = date.getTime() + randomEnd4_id();
//        long yituliuId = Long.parseLong(idStr);   //一图流id 当前时间戳加随机4位数字
//
//        int status = 1;
//
//        SurveyUser surveyUserNew = SurveyUser.builder()  //新用户信息
//                .id(yituliuId)
//                .createTime(date)
//                .updateTime(date)
//                .status(status) //无密码的账号状态值为2
//                .ip(ipAddress)
//                .build();
//
//
//        if (passWord != null && !"undefined".equals(passWord) && !"null".equals(passWord) && passWord.length() > 5) {  //注册正式账号，密码不为空或未知值且长度大于5
//            Log.info("注册有密码的账号");
//            checkPassWord(passWord); //检查密码
//            QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
//            queryWrapper.eq("user_name", userName);  //查询用户名是否存在
//            surveyUserExist = surveyUserMapper.selectOne(queryWrapper);
//
//            passWord = AES.encrypt(passWord, ApplicationConfig.Secret);
//            surveyUserNew.setUserName(userName);  //非游客用户设置的用户名不带后缀
//            surveyUserNew.setPassWord(passWord);
//            status = UserStatus.addPermission(surveyUserNew.getStatus(), UserStatusCode.HAS_PASSWORD); //有密码的账号增加权限
//            surveyUserNew.setStatus(status);
//        } else { //注册游客账号
//            Log.info("注册无密码的账号");
//            for (int i = 0; i < 6; i++) {
//                QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
//                queryWrapper.eq("user_name", userNameAndSuffix);  //查询用户名#后缀是否存在
//                surveyUserExist = surveyUserMapper.selectOne(queryWrapper);
//                if (surveyUserExist == null) break;  //不存在就跳出开始注册
//                if (i < 4) {
//                    userNameAndSuffix = userName + getSuffix(4); //用户id后四位后缀  #xxxx
//                    Log.error("id后缀重复");
//                } else {
//                    userNameAndSuffix = userName + getSuffix(5);
//                    Log.error("id后缀重复次数过多扩充位数");
//                }
//            }
//            surveyUserNew.setUserName(userNameAndSuffix);  //设置游客用户名
//        }
//
//        if (surveyUserExist != null) throw new ServiceException(ResultCode.USER_EXISTED);
//
//        int row = surveyUserMapper.save(surveyUserNew);
//        if (row < 1) throw new ServiceException(ResultCode.SYSTEM_INNER_ERROR);
//
//        long timeStamp = date.getTime();
//
//        //用户凭证  由用户名+一图流id+时间戳 加密得到
//        String token = AES.encrypt(surveyUserNew.getUserName() + "." + yituliuId + "." + timeStamp, ApplicationConfig.Secret);
//
//        UserDataResponse response = new UserDataResponse();  //返回的用户信息实体类  包括凭证，用户名，用户状态等
//        response.setUserName(userNameAndSuffix);
//        response.setToken(token);
//        response.setStatus(surveyUserNew.getStatus());
//
//        return response;
//    }
//
//
//    /**
//     * 调查站登录
//     *
//     * @param ipAddress       ip
//     * @param surveyRequestVo 用户信息
//     * @return 用户状态信息
//     */
//    public UserDataResponse login(String ipAddress, SurveyRequestVo surveyRequestVo) {
//        String userName = surveyRequestVo.getUserName();
//        String passWord = surveyRequestVo.getPassWord();
//        String email = surveyRequestVo.getEmail();
//
//        if (userName == null) {
//            userName = "";
//        }
//
//        //根据用户名查询用户信息
//        QueryWrapper<SurveyUser> queryWrapperByUserName = new QueryWrapper<>();
//        queryWrapperByUserName.eq("user_name", userName);
//        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapperByUserName);
//
//        //根据邮箱查询用户信息
//        if (surveyUser == null) {
//            if (email == null) {
//                email = "";
//            }
//            Log.info("通过邮箱查询");
//            QueryWrapper<SurveyUser> queryWrapperByEmail = new QueryWrapper<>();
//            queryWrapperByEmail.eq("email", email);
//            surveyUser = surveyUserMapper.selectOne(queryWrapperByEmail);
//        }
//
//        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
//
//        //用户状态为2则为正式账号 需匹配密码
//        if (UserStatus.hasPermission(surveyUser.getStatus(), UserStatusCode.HAS_PASSWORD)) {
//            String encrypt = AES.encrypt(passWord, ApplicationConfig.Secret);
//            if (surveyUser.getPassWord() == null || (!surveyUser.getPassWord().equals(encrypt))) {
//                throw new ServiceException(ResultCode.USER_LOGIN_ERROR);
//            }
//        }
//
//        long timeStamp = System.currentTimeMillis();
//        Long yituliuId = surveyUser.getId();  //一图流id
//
//        //用户凭证  由用户名+一图流id+时间戳 加密得到
//        String token = AES.encrypt(surveyUser.getUserName() + "." + yituliuId + "." + timeStamp, ApplicationConfig.Secret);
//
//        UserDataResponse response = new UserDataResponse();  //返回的用户信息实体类  包括凭证，用户名，用户状态等
//        response.setUserName(userName);
//        response.setToken(token);
//        response.setStatus(surveyUser.getStatus());
//
//        return response;
//    }


    /**
     * 通过森空岛cred登录
     *
     * @param ipAddress       ip地址
     * @param sklandDto 自定义的请求实体类（具体内容看实体类的注释）
     * @return 用户状态信息
     */
    public Result<UserDataResponse> loginByCRED(String ipAddress, SklandDto sklandDto) {
        UserDataResponse response = new UserDataResponse();
        Date date = new Date();  //当前时间
        long timeStamp = date.getTime();

        String uid = sklandDto.getUid();
        String nickName = sklandDto.getNickName();

        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", uid);
        SurveyUser surveyUserByUid = surveyUserMapper.selectOne(queryWrapper);

        if (surveyUserByUid != null) {
            response.setUserName(surveyUserByUid.getUserName());
            String token = AES.encrypt(surveyUserByUid.getUserName() + "." + surveyUserByUid.getId() + "." + timeStamp, ApplicationConfig.Secret);
            response.setToken(token);
            response.setStatus(surveyUserByUid.getStatus());

            updateSurveyUser(surveyUserByUid);
            return Result.success(response);
        }


        String idStr = date.getTime() + randomEnd4_id();
        long id = Long.parseLong(idStr);


        String[] split = nickName.split("#");
        String userName = split[0] + getSuffix(4);

        for (int i = 0; i < 6; i++) {
            QueryWrapper<SurveyUser> queryWrapperNoExist = new QueryWrapper<>();
            queryWrapperNoExist.eq("user_name", userName);
            SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapperNoExist);
            if (surveyUser == null) {
                break;
            } else {
                userName = split[0] + getSuffix(4);
            }
        }

        SurveyUser surveyUser = new SurveyUser();
        surveyUser.setId(id);
        surveyUser.setIp(ipAddress);
        surveyUser.setUserName(userName);
        surveyUser.setUid(uid);
        surveyUser.setStatus(1);
        surveyUser.setCreateTime(date);
        surveyUser.setUpdateTime(new Date(timeStamp + 3600));

        String token = AES.encrypt(userName + "." + id + "." + timeStamp, ApplicationConfig.Secret);

        surveyUserMapper.save(surveyUser);


        response.setUserName(surveyUser.getUserName());
        response.setToken(token);
        response.setStatus(surveyUser.getStatus());


        return Result.success(response);
    }

    /**
     * 通过森空岛CRED找回账号
     *
     * @param CRED 森空岛CRED
     * @return 用户状态信息
     */
    public Result<UserDataResponse> retrievalAccountByCRED(String CRED) {

        Map<String, String> skLandPlayerBinding = getSKLandPlayerBinding(CRED);
        String uid = skLandPlayerBinding.get("uid");

        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", uid);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
        if (surveyUser == null) {
            throw new ServiceException(ResultCode.USER_NOT_BIND_UID);
        }
        UserDataResponse response = new UserDataResponse();
        response.setUserName(surveyUser.getUserName());
        return Result.success(response);
    }

}
