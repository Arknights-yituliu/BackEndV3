package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.util.Result;
import com.lhs.common.util.ResultCode;
import com.lhs.common.util.UserStatus;
import com.lhs.common.util.UserStatusCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.*;
import com.lhs.entity.dto.survey.EmailRequestDTO;
import com.lhs.entity.dto.survey.UpdateUserDataDTO;
import com.lhs.entity.dto.survey.LoginDataDTO;
import com.lhs.entity.dto.survey.SklandDTO;
import com.lhs.entity.dto.util.EmailFormDTO;
import com.lhs.entity.po.survey.AkPlayerBindInfo;
import com.lhs.entity.po.survey.SurveyUser;

import com.lhs.mapper.survey.AkPlayerBindInfoMapper;
import com.lhs.mapper.survey.SurveyUserMapper;
import com.lhs.service.util.Email163Service;
import com.lhs.service.util.OSSService;

import com.lhs.entity.vo.survey.UserDataVO;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SurveyUserService {


    private final SurveyUserMapper surveyUserMapper;

    private final RedisTemplate<String, String> redisTemplate;

    private final Email163Service email163Service;

    private final AkPlayerBindInfoMapper akPlayerBindInfoMapper;


    private final OSSService ossService;

    public SurveyUserService(SurveyUserMapper surveyUserMapper, RedisTemplate<String, String> redisTemplate, Email163Service email163Service, AkPlayerBindInfoMapper akPlayerBindInfoMapper, OSSService ossService) {
        this.surveyUserMapper = surveyUserMapper;
        this.redisTemplate = redisTemplate;
        this.email163Service = email163Service;
        this.akPlayerBindInfoMapper = akPlayerBindInfoMapper;
        this.ossService = ossService;
    }

    /**
     * 调查站用户注册
     * @param ipAddress 用户ip
     * @param loginDataDTO 注册信息
     * @return
     */
    public UserDataVO registerV2(String ipAddress, LoginDataDTO loginDataDTO) {
        //注册类型
        String accountType = loginDataDTO.getAccountType();

        //注册类型不能为空或未知
        if (accountType == null || "undefined".equals(accountType) || "null".equals(accountType)) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }

        //获取用户名，密码，邮箱
        String userName = loginDataDTO.getUserName().trim();
        String passWord = loginDataDTO.getPassWord().trim();
        String email = loginDataDTO.getEmail().trim();

        //当前时间
        Date date = new Date();
        //一图流id 当前时间戳加随机4位数字
        long userId = Long.parseLong(date.getTime() + randomEnd4_id());
        //用户初始状态
        int status = 1;

        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        //组装新用户信息
        SurveyUser surveyUserNew = SurveyUser.builder()
                .id(userId)
                .ip(ipAddress)
                .createTime(date)
                .updateTime(date)
                .build();

        //账号密码方式注册
        if ("passWord".equals(accountType)) {
            //检查用户名格式
            checkUserName(userName);
            //查询是否存在同名用户
            queryWrapper.eq("user_name", userName);
            SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
            if (surveyUser != null) throw new ServiceException(ResultCode.USER_EXISTED);
            //给用户信息写入用户名
            surveyUserNew.setUserName(userName);
            //检查密码格式
            checkPassWord(passWord);
            //密码加密
            passWord = AES.encrypt(passWord, ApplicationConfig.Secret);
            // 更新用户状态为有密码
            status =  UserStatus.addPermission(status, UserStatusCode.HAS_PASSWORD);
            //给用户信息写入密码
            surveyUserNew.setPassWord(passWord);
//            Log.info("账号密码注册——用户名："+userName+"密码："+passWord);

        }

        if ("emailCode".equals(accountType)) {
            //查看邮箱是否注册过
            queryWrapper.eq("email", email);
            SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
            if (surveyUser != null) throw new ServiceException(ResultCode.USER_EXISTED);
            //用户输入的验证码
            String inputCode = loginDataDTO.getEmailCode();
            //检查验证码
            email163Service.compareVerificationCode(inputCode,"CODE:CODE." + email);
            //更新用户状态为有密码
            status = UserStatus.addPermission(status, UserStatusCode.HAS_EMAIL);
            //给用户设置初始昵称
            userName = "博士" + date.getTime() + randomEnd4_id();
            surveyUserNew.setUserName(userName);
            surveyUserNew.setEmail(email);
//            Log.info("账号密码注册——邮箱："+email+"验证码："+inputCode);
        }

        //给用户写入状态
        surveyUserNew.setStatus(status);
        surveyUserNew.setDeleteFlag(false);

        System.out.println(surveyUserNew);

        surveyUserMapper.save(surveyUserNew);

        System.out.println(surveyUserNew);

        long timeStamp = date.getTime();

        //用户凭证  由用户部分信息+一图流id+时间戳 加密得到
        Map<Object, Object> hashMap = new HashMap<>();
        hashMap.put("userName", surveyUserNew.getUserName());
        String header = JsonMapper.toJSONString(hashMap);
        String token = getToken(header,userId,timeStamp);

        UserDataVO response = new UserDataVO();  //返回的用户信息实体类  包括凭证，用户名，用户状态等
        response.setUserName(surveyUserNew.getUserName());
        response.setToken(token);
        response.setStatus(surveyUserNew.getStatus());
        response.setEmail(surveyUserNew.getEmail());

        return response;
    }



    

    /**
     * 调查站登录
     * @param ipAddress       ip
     * @param loginDataDto 用户修改的信息
     * @return 用户状态信息
     */
    public UserDataVO loginV2(String ipAddress, LoginDataDTO loginDataDto) {
        String accountType = loginDataDto.getAccountType();

        if (accountType == null || "undefined".equals(accountType) || "null".equals(accountType)) {
            throw new ServiceException(ResultCode.PARAM_TYPE_BIND_ERROR);
        }

        SurveyUser surveyUser = null;

        if ("passWord".equals(accountType)) {
            surveyUser = loginByPassWord(loginDataDto);
        }

        if ("emailCode".equals(accountType)) {
            surveyUser = loginByEmailCode(loginDataDto);
        }

        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);

        long timeStamp = System.currentTimeMillis();
        Long userId = surveyUser.getId();  //一图流id

        //用户凭证  由用户部分信息+一图流id+时间戳 加密得到
        Map<Object, Object> hashMap = new HashMap<>();
        hashMap.put("userName", surveyUser.getUserName());
        String header = JsonMapper.toJSONString(hashMap);
        String token = getToken(header,userId,timeStamp);

        UserDataVO response = new UserDataVO();  //返回的用户信息实体类  包括凭证，用户名，用户状态等
        response.setUserName(surveyUser.getUserName());
        response.setToken(token);
        response.setStatus(surveyUser.getStatus());
        response.setEmail(surveyUser.getEmail());
        response.setAvatar(surveyUser.getAvatar());

        return response;
    }

    private String getToken(String header,Long userId,Long timeStamp){
        return AES.encrypt(header + "." + userId + "." + timeStamp, ApplicationConfig.Secret);
    }

    /**
     * 通过密码登录
     * @param loginDataDto 前端传来的用户名密码
     * @return 用户数据
     */
    public SurveyUser loginByPassWord(LoginDataDTO loginDataDto){
        String userName = loginDataDto.getUserName().trim();
        String passWord = loginDataDto.getPassWord().trim();

        passWord = AES.encrypt(passWord, ApplicationConfig.Secret); //密码加密

        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();

        //判断用户名/邮箱登录
        if (userName.contains("@")) {
            queryWrapper.eq("email", userName);  //邮箱
        } else {
            queryWrapper.eq("user_name", userName); //用户名
        }

        //查询用户信息是否存在
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);

        //用户是否设置了密码
        if (UserStatus.hasPermission(surveyUser.getStatus(), UserStatusCode.HAS_PASSWORD)) {
            //对比密码是否正确
            if (!surveyUser.getPassWord().equals(passWord)) {
                throw new ServiceException(ResultCode.USER_LOGIN_ERROR);
            }
        }


        return surveyUser;
    }

    /**
     * 通过邮箱验证码登录
     * @param loginDataDto 前端传来的邮箱和验证码
     * @return 用户数据
     */
    public SurveyUser loginByEmailCode(LoginDataDTO loginDataDto){
        String email = loginDataDto.getEmail().trim();
        String emailCode = loginDataDto.getEmailCode().trim();

        //设置查询构造器条件
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);

        //查询用户是否存在
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);

        //对比输入的验证码和redis中的验证码
        email163Service.compareVerificationCode(emailCode,"CODE:CODE." + email);

        return surveyUser;
    }

    public void sendEmail(EmailRequestDTO emailRequestDto){
        String mailUsage = emailRequestDto.getMailUsage();
        //注册
        if("register".equals(mailUsage)){
           sendEmailForRegister(emailRequestDto);
        }
        //登录
        if("login".equals(mailUsage)){
            sendEmailForLogin(emailRequestDto);
        }
        //修改邮箱
        if("changeEmail".equals(mailUsage)){
           sendEmailForChangeEmail(emailRequestDto);
        }
    }

    /**
     * 设置注册验证码的邮件信息
     * @param emailRequestDto 邮件信息
     */
    public void sendEmailForRegister(EmailRequestDTO emailRequestDto){
        String emailAddress = emailRequestDto.getEmail().trim();
        //设置查询构造器条件
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", emailAddress);
        //查询是否有绑定这个邮箱的用户
        SurveyUser surveyUserByEmail = surveyUserMapper.selectOne(queryWrapper);
        if (surveyUserByEmail != null) throw new ServiceException(ResultCode.USER_EXISTED);

        Integer code = email163Service.CreateVerificationCode(emailAddress, 9999);
        String subject = "一图流注册验证码";
        String text = "您本次注册验证码： " + code +",验证码有效时间5分钟";

        EmailFormDTO emailFormDTO = new EmailFormDTO();
        emailFormDTO.setFrom("ark_yituliu@163.com");
        emailFormDTO.setTo(emailAddress);
        emailFormDTO.setSubject(subject);
        emailFormDTO.setText(text);
        email163Service.sendSimpleEmail(emailFormDTO);
    }

    /**
     * 设置登录验证码的邮件信息
     * @param emailRequestDto 前端输入的收件人地址
     */
    public void sendEmailForLogin(EmailRequestDTO emailRequestDto){
        String emailAddress = emailRequestDto.getEmail().trim();  //收件人地址
        //设置查询构造器条件
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", emailAddress);
        //查询是否有绑定这个邮箱的用户
        SurveyUser surveyUserByEmail = surveyUserMapper.selectOne(queryWrapper);
        if(surveyUserByEmail == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        Integer code = email163Service.CreateVerificationCode(emailAddress, 9999);
        String subject = "一图流登录验证码";
        String text = "您本次登录验证码：" + code+",验证码有效时间5分钟";

        EmailFormDTO emailFormDTO = new EmailFormDTO();
        emailFormDTO.setFrom("ark_yituliu@163.com");
        emailFormDTO.setTo(emailAddress);
        emailFormDTO.setSubject(subject);
        emailFormDTO.setText(text);
        email163Service.sendSimpleEmail(emailFormDTO);
    }

    /**
     * 设置修改邮箱验证码的邮件信息
     * @param emailRequestDto 邮件信息
     */
    public void sendEmailForChangeEmail(EmailRequestDTO emailRequestDto){
        String emailAddress = emailRequestDto.getEmail().trim();
        String token = emailRequestDto.getToken();  //用户凭证
        //变更邮箱
        SurveyUser surveyUserByToken = getSurveyUserByToken(token);
        //判断是否绑定过邮箱
        if (UserStatus.hasPermission(surveyUserByToken.getStatus(), UserStatusCode.HAS_EMAIL)) {
            emailAddress = surveyUserByToken.getEmail();
        }

        Integer code = email163Service.CreateVerificationCode(emailAddress, 9999);
        String subject = "一图流邮箱变更验证码";
        String text = "您本次变更邮箱验证码：" + code +",验证码有效时间5分钟";

        EmailFormDTO emailFormDTO = new EmailFormDTO();
        emailFormDTO.setFrom("ark_yituliu@163.com");
        emailFormDTO.setTo(emailAddress);
        emailFormDTO.setSubject(subject);
        emailFormDTO.setText(text);
        email163Service.sendSimpleEmail(emailFormDTO);
    }

    public UserDataVO updateUserData(UpdateUserDataDTO updateUserDataDto){

        String property = updateUserDataDto.getProperty();
        String token = updateUserDataDto.getToken();
        SurveyUser surveyUserByToken = getSurveyUserByToken(token);

        if("email".equals(property)){
            return updateOrBindEmail(surveyUserByToken,updateUserDataDto);
        }

        if("passWord".equals(property)){
            return  updatePassWord(surveyUserByToken,updateUserDataDto);
        }

        if("userName".equals(property)){
            return updateUserName(surveyUserByToken,updateUserDataDto);
        }

        if("avatar".equals(property)){
            return updateUserAvatar(surveyUserByToken,updateUserDataDto);
        }

        throw new ServiceException(ResultCode.PARAM_IS_INVALID);

    }

    /**
     * 更新或绑定邮箱
     * @param surveyUserByToken 用户信息
     * @param updateUserDataDto 用户修改的信息
     * @return 成功信息
     */
    private UserDataVO updateOrBindEmail(SurveyUser surveyUserByToken,UpdateUserDataDTO updateUserDataDto) {
        String email = updateUserDataDto.getEmail().trim();
        String emailCode = updateUserDataDto.getEmailCode().trim();

        //对比用户输入的验证码和后台的验证码
        email163Service.compareVerificationCode(emailCode,"CODE:CODE." + email);
        //设置用户邮箱
        surveyUserByToken.setEmail(email);
        if (!UserStatus.hasPermission(surveyUserByToken.getStatus(), UserStatusCode.HAS_EMAIL)) {
            surveyUserByToken.setStatus(UserStatus.addPermission(surveyUserByToken.getStatus(), UserStatusCode.HAS_EMAIL));
        }

        backupSurveyUser(surveyUserByToken);
        UserDataVO response = new UserDataVO();
        response.setStatus(surveyUserByToken.getStatus());
        return response;
    }

    /**
     * 更新密码
     * @param surveyUserByToken 用户信息
     * @param updateUserDataDto 用户修改的信息
     * @return 用户新信息
     */
    private UserDataVO updatePassWord(SurveyUser surveyUserByToken, UpdateUserDataDTO updateUserDataDto) {


        String newPassWord = updateUserDataDto.getNewPassWord().trim(); //新密码
        //检查新密码格式
        checkPassWord(newPassWord);
        //加密新密码
        newPassWord = AES.encrypt(newPassWord, ApplicationConfig.Secret);
        //用户状态
        Integer status = surveyUserByToken.getStatus();

        if (UserStatus.hasPermission(status, UserStatusCode.HAS_PASSWORD)) {//替换旧密码
            //旧密码
            String oldPassWord = updateUserDataDto.getOldPassWord().trim();
            //加密旧密码
            oldPassWord = AES.encrypt(oldPassWord, ApplicationConfig.Secret);
            //检查旧密码是否正确
            if (surveyUserByToken.getPassWord().equals(oldPassWord)) {
                //更新旧密码为新密码
                surveyUserByToken.setPassWord(newPassWord);
            } else {
                throw new ServiceException(ResultCode.USER_PASSWORD_ERROR);
            }
        } else { //设置新密码
            //更新用户状态
            status = UserStatus.addPermission(status, UserStatusCode.HAS_PASSWORD);
            //想用户信息写入密码和状态
            surveyUserByToken.setPassWord(newPassWord);
            surveyUserByToken.setStatus(status);
        }

        backupSurveyUser(surveyUserByToken);

        UserDataVO response = new UserDataVO();
        response.setStatus(surveyUserByToken.getStatus());

        return response;
    }

    /**
     * 更新用户名
     * @param surveyUserByToken 用户信息
     * @param updateUserDataDto 用户修改的信息
     * @return 用户新信息
     */
    private UserDataVO updateUserName(SurveyUser surveyUserByToken, UpdateUserDataDTO updateUserDataDto) {
        String userName = updateUserDataDto.getUserName().trim();
        //检查用户名格式
        checkUserName(userName);
        //查询更新的用户名是否有同名的
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name",userName);
        if(surveyUserMapper.selectOne(queryWrapper)!= null) throw new ServiceException(ResultCode.USER_EXISTED);

        //通过判断用户是否绑定了邮箱或设置了密码，用来区分v2版本注册的用户
        if(UserStatus.hasPermission(surveyUserByToken.getStatus(),UserStatusCode.HAS_PASSWORD)||
                UserStatus.hasPermission(surveyUserByToken.getStatus(),UserStatusCode.HAS_EMAIL)){
            //用户信息写入新用户名
            surveyUserByToken.setUserName(userName);
            //备份用户信息
            backupSurveyUser(surveyUserByToken);
        }else {
            throw new ServiceException(ResultCode.NOT_SET_PASSWORD_OR_BIND_EMAIL);
        }

        UserDataVO response = new UserDataVO();
        response.setUserName(surveyUserByToken.getUserName());
        return response;
    }

    private UserDataVO updateUserAvatar(SurveyUser surveyUserByToken, UpdateUserDataDTO updateUserDataDto){
              surveyUserByToken.setAvatar(updateUserDataDto.getAvatar());
              UserDataVO response = new UserDataVO();
              response.setAvatar(surveyUserByToken.getAvatar());
              backupSurveyUser(surveyUserByToken);
              return response;
    }

   

    /**
     * 通过用户凭证查找用户信息
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
     * 通过游戏内的玩家uid查找用户信息
     *
     * @param uid 游戏内的玩家uid
     * @return 用户信息
     */
    public SurveyUser getSurveyUserByUid(String uid) {
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ak_uid", uid);
        return surveyUserMapper.selectOne(queryWrapper);
    }

    /**
     * 备份更新用户信息
     *
     * @param surveyUser 用户信息
     */
    public void backupSurveyUser(SurveyUser surveyUser) {
        surveyUser.setUpdateTime(new Date());
        surveyUserMapper.updateUserById(surveyUser);   //更新用户表

        Long id = surveyUser.getId();
        ossService.upload(JsonMapper.toJSONString(surveyUser), "survey/user/info/" + id + ".json");
    }



    /**
     * 获得森空岛绑定信息
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
     * 拿到表名序号
     * @param id  一图流id
     * @return 表名序号
     */
    public Integer getTableIndex(Long id) {
        return 1;
    }

    /**
     * 解密用户凭证
     * @param token 用户凭证
     * @return 一图流id
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
     * 通过森空岛cred登录
     *
     * @param ipAddress       ip地址
     * @param sklandDto 用户修改的信息
     * @return 用户状态信息
     */
    public Result<UserDataVO> loginByCRED(String ipAddress, SklandDTO sklandDto) {
        UserDataVO response = new UserDataVO();
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

            backupSurveyUser(surveyUserByUid);
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
        surveyUser.setAkUid(uid);
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
    public Result<UserDataVO> retrievalAccountByCRED(String CRED) {

        Map<String, String> skLandPlayerBinding = getSKLandPlayerBinding(CRED);
        String uid = skLandPlayerBinding.get("uid");

        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", uid);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
        if (surveyUser == null) {
            throw new ServiceException(ResultCode.USER_NOT_BIND_UID);
        }
        UserDataVO response = new UserDataVO();
        response.setUserName(surveyUser.getUserName());
        return Result.success(response);
    }


    public void migrateLog() {
        List<SurveyUser> surveyUsers = surveyUserMapper.selectList(null);
        for(SurveyUser surveyUser : surveyUsers){
            if(surveyUser.getAkUid()==null)  continue;
            if(surveyUser.getAkUid().contains("delete")) continue;
            AkPlayerBindInfo akPlayerBindInfo = new AkPlayerBindInfo();
            akPlayerBindInfo.setId(surveyUser.getId());
            akPlayerBindInfo.setUserName(surveyUser.getUserName());
            akPlayerBindInfo.setUid(surveyUser.getAkUid());
            akPlayerBindInfo.setDeleteFlag(surveyUser.getDeleteFlag());
            akPlayerBindInfo.setLastTime(surveyUser.getUpdateTime().getTime());
            akPlayerBindInfo.setIp(surveyUser.getIp());
            QueryWrapper<AkPlayerBindInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id",surveyUser.getId());
            if(akPlayerBindInfoMapper.selectOne(queryWrapper)==null){
                akPlayerBindInfoMapper.insert(akPlayerBindInfo);
            }else {
                akPlayerBindInfoMapper.update(akPlayerBindInfo,queryWrapper);
            }
        }
    }


    public List<SurveyUser> getAllUserData(){
        return surveyUserMapper.selectList(null);
    }
}
