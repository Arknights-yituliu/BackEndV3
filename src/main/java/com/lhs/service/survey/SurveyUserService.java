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
     * @param ipAddress       ip
     * @param surveyRequestVo 自定义的请求实体类（具体内容看实体类的注释，每个方法一般只会用到其中几个属性）
     * @return 用户状态信息
     */

    public UserDataResponse register(String ipAddress, SurveyRequestVo surveyRequestVo) {
        String userName = surveyRequestVo.getUserName();
        String passWord = surveyRequestVo.getPassWord();
        checkUserName(userName);
        Date date = new Date();  //当前时间
        String userNameAndSuffix = userName + getSuffix(4); //游客注册的用户名可重复，但是有后缀，格式：用户名#xxxx
        SurveyUser surveyUserExist = null;  //是否存在的旧用户信息

        String idStr = date.getTime() + randomEnd4_id();
        long yituliuId = Long.parseLong(idStr);   //一图流id 当前时间戳加随机4位数字

        int status = 1;

        SurveyUser  surveyUserNew = SurveyUser.builder()  //新用户信息
                .id(yituliuId)
                .createTime(date)
                .updateTime(date)
                .status(status) //无密码的账号状态值为2
                .ip(ipAddress)
                .build();


        if (passWord != null && !"undefined".equals(passWord) && !"null".equals(passWord) && passWord.length() > 5) {  //注册正式账号，密码不为空或未知值且长度大于5
            Log.info("注册有密码的账号");
            checkPassWord(passWord); //检查密码
            QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_name", userName);  //查询用户名是否存在
            surveyUserExist = surveyUserMapper.selectOne(queryWrapper);

            passWord = AES.encrypt(passWord, ApplicationConfig.Secret);
            surveyUserNew.setUserName(userName);  //非游客用户设置的用户名不带后缀
            surveyUserNew.setPassWord(passWord);
            status = UserStatus.addPermission(surveyUserNew.getStatus(),UserStatusCode.HAS_PASSWORD); //有密码的账号增加权限
            surveyUserNew.setStatus(status);
        }else { //注册游客账号
            Log.info("注册无密码的账号");
            for (int i = 0; i < 6; i++) {
                QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("user_name", userNameAndSuffix);  //查询用户名#后缀是否存在
                surveyUserExist = surveyUserMapper.selectOne(queryWrapper);
                if (surveyUserExist == null) break;  //不存在就跳出开始注册
                if (i < 4) {
                    userNameAndSuffix = userName + getSuffix(4); //用户id后四位后缀  #xxxx
                    Log.error("id后缀重复");
                } else {
                    userNameAndSuffix = userName + getSuffix(5);
                    Log.error("id后缀重复次数过多扩充位数");
                }
            }
            surveyUserNew.setUserName(userNameAndSuffix);  //设置游客用户名
        }

        if (surveyUserExist != null) throw new ServiceException(ResultCode.USER_EXISTED);

        int row = surveyUserMapper.save(surveyUserNew);
        if (row < 1) throw new ServiceException(ResultCode.SYSTEM_INNER_ERROR);

        long timeStamp = date.getTime();

        //用户凭证  由用户名+一图流id+时间戳 加密得到
        String token = AES.encrypt(surveyUserNew.getUserName() + "." + yituliuId + "." + timeStamp, ApplicationConfig.Secret);

        UserDataResponse response = new UserDataResponse();  //返回的用户信息实体类  包括凭证，用户名，用户状态等
        response.setUserName(userNameAndSuffix);
        response.setToken(token);
        response.setStatus(surveyUserNew.getStatus());

        return response;
    }


    /**
     * 通过森空岛cred登录
     *
     * @param ipAddress       ip地址
     * @param surveyRequestVo 自定义的请求实体类（具体内容看实体类的注释，每个方法一般只会用到其中几个属性）
     * @return 用户状态信息
     */
    public Result<UserDataResponse> loginByCRED(String ipAddress, SurveyRequestVo surveyRequestVo) {
        UserDataResponse response = new UserDataResponse();
        Date date = new Date();  //当前时间
        long timeStamp = date.getTime();

        String uid = surveyRequestVo.getUid();
        String nickName = surveyRequestVo.getNickName();

        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", uid);
        SurveyUser surveyUserByUid = surveyUserMapper.selectOne(queryWrapper);

        if (surveyUserByUid != null) {
            response.setUserName(surveyUserByUid.getUserName());
            String token = AES.encrypt(surveyUserByUid.getUserName() + "." + surveyUserByUid.getId() + "." + timeStamp, ApplicationConfig.Secret);
            response.setToken(token);
            response.setStatus(surveyUserByUid.getStatus());
            surveyUserByUid.setNickName(nickName);
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
        surveyUser.setNickName(nickName);
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
     * 调查站登录
     * @param ipAddress       ip
     * @param surveyRequestVo 用户信息
     * @return 用户状态信息
     */
    public UserDataResponse login(String ipAddress, SurveyRequestVo surveyRequestVo) {
        String userName = surveyRequestVo.getUserName();
        String passWord = surveyRequestVo.getPassWord();
        String email = surveyRequestVo.getEmail();

        if(userName==null) {
            userName = "";
        }

        //根据用户名查询用户信息
        QueryWrapper<SurveyUser> queryWrapperByUserName = new QueryWrapper<>();
        queryWrapperByUserName.eq("user_name", userName);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapperByUserName);

        //根据邮箱查询用户信息
        if (surveyUser == null){
            if(email==null) {
                email = "";
            }
            Log.info("通过邮箱查询");
            QueryWrapper<SurveyUser> queryWrapperByEmail = new QueryWrapper<>();
            queryWrapperByEmail.eq("email", email);
            surveyUser = surveyUserMapper.selectOne(queryWrapperByEmail);
        }

        if (surveyUser == null) throw new ServiceException(ResultCode.USER_ACCOUNT_NOT_EXIST);

        //用户状态为2则为正式账号 需匹配密码
        if(UserStatus.hasPermission(surveyUser.getStatus(),UserStatusCode.HAS_PASSWORD)){
            String encrypt = AES.encrypt(passWord, ApplicationConfig.Secret);
            if(surveyUser.getPassWord()==null||(!surveyUser.getPassWord().equals(encrypt))){
                throw new ServiceException(ResultCode.USER_LOGIN_ERROR);
            }
        }

        long timeStamp = System.currentTimeMillis();
        Long yituliuId = surveyUser.getId();  //一图流id

        //用户凭证  由用户名+一图流id+时间戳 加密得到
        String token = AES.encrypt(surveyUser.getUserName() + "." + yituliuId + "." + timeStamp, ApplicationConfig.Secret);

        UserDataResponse response = new UserDataResponse();  //返回的用户信息实体类  包括凭证，用户名，用户状态等
        response.setUserName(userName);
        response.setToken(token);
        response.setStatus(surveyUser.getStatus());

        return response;
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

    public Result<Object> retrievalAccount(String uid) {
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

    /**
     * 发送邮件验证码
     * @param surveyRequestVo 自定义的请求实体类（具体内容看实体类的注释，每个方法一般只会用到其中几个属性）
     * @return
     */
    public Result<Object> sendEmailCode(SurveyRequestVo surveyRequestVo) {

        String email = surveyRequestVo.getEmail();  //绑定的邮箱
        String token = surveyRequestVo.getToken();  //用户凭证
        SurveyUser surveyUserByToken = getSurveyUserByToken(token);
        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setFrom("1820702789@qq.com");

        if(UserStatus.hasPermission(surveyUserByToken.getStatus(),UserStatusCode.HAS_EMAIL)){   //判断是否绑定过邮箱
            emailRequest.setTo(surveyUserByToken.getEmail()); //以前绑定过邮箱则发给旧邮箱
            emailRequest.setSubject("一图流账号换绑新邮箱");
        }else {
            emailRequest.setTo(email); //没有绑定过邮箱则发给新邮箱
            emailRequest.setSubject("一图流账号绑定新邮箱");
        }
        int random = new Random().nextInt(9999);
        String code = String.format("%4s", random).replace(" ", "0");
        redisTemplate.opsForValue().set("CODE:CODE." + email, code, 300, TimeUnit.SECONDS);
        emailRequest.setText("本次绑定验证码：" + code);
        emailService.sendMail(emailRequest);
        return Result.success();
    }

    /**
     * 更新或绑定邮箱
     * @param surveyRequestVo 自定义的请求实体类（具体内容看实体类的注释，每个方法一般只会用到其中几个属性）
     * @return
     */
    public Result<Object> updateOrBindEmail(SurveyRequestVo surveyRequestVo) {
        String email = surveyRequestVo.getEmail();
        String token = surveyRequestVo.getToken();
        String emailCode = surveyRequestVo.getEmailCode();
        SurveyUser surveyUserByToken = getSurveyUserByToken(token);
        String code = redisTemplate.opsForValue().get("CODE:CODE." + email);
        //检查验证码
        if (code == null) {
            throw new ServiceException(ResultCode.CODE_NOT_EXIST);
        }
        if (!emailCode.equals(code)) {
            throw new ServiceException(ResultCode.CODE_ERROR);
        }

        surveyUserByToken.setEmail(email);
        if(!UserStatus.hasPermission(surveyUserByToken.getStatus(),UserStatusCode.HAS_EMAIL)){
            surveyUserByToken.setStatus(UserStatus.addPermission(surveyUserByToken.getStatus(),UserStatusCode.HAS_EMAIL));
        }

        updateSurveyUser(surveyUserByToken);
        return Result.success();
    }

    /**
     * 更新密码
     * @param surveyRequestVo 自定义的请求实体类（具体内容看实体类的注释，每个方法一般只会用到其中几个属性）
     * @return
     */
    public Result<Object> updatePassWord(SurveyRequestVo surveyRequestVo) {

        String token = surveyRequestVo.getToken();
        SurveyUser surveyUserByToken = getSurveyUserByToken(token); //用户信息
        String newPassWord = surveyRequestVo.getNewPassWord(); //新密码
        checkPassWord(newPassWord);  //检查新密码格式

        newPassWord = AES.encrypt(newPassWord, ApplicationConfig.Secret);  //加密新密码

        Integer status = surveyUserByToken.getStatus();
        //设置新密码
        if(UserStatus.hasPermission(status,UserStatusCode.HAS_PASSWORD)){
            String oldPassWord = surveyRequestVo.getOldPassWord(); //旧密码
            checkPassWord(oldPassWord);  //检查旧密码
            if(surveyUserByToken.getPassWord().equals(oldPassWord)){  //检查旧密码是否正确
                surveyUserByToken.setPassWord(newPassWord);  //更新旧密码为新密码
            }else {
                throw new ServiceException(ResultCode.USER_PASSWORD_ERROR);
            }

        }else {
            status = UserStatus.addPermission(status,UserStatusCode.HAS_PASSWORD);
            surveyUserByToken.setPassWord(newPassWord);
            surveyUserByToken.setStatus(status);
        }

        updateSurveyUser(surveyUserByToken);

        return Result.success();
    }


    /**
     * 通过cred进行身份验证
     *
     * @param surveyRequestVo 自定义的请求实体类（具体内容看实体类的注释，每个方法一般只会用到其中几个属性）
     * @return
     */
    public Result<UserDataResponse> authentication(SurveyRequestVo surveyRequestVo) {
        String token = surveyRequestVo.getToken();
        SurveyUser surveyUser = getSurveyUserByToken(token);
        String cred = surveyRequestVo.getCred();
        Map<String, String> skLandPlayerBinding = getSKLandPlayerBinding(cred);
        String uid = skLandPlayerBinding.get("uid");
        String nickName = skLandPlayerBinding.get("nickName");
        if (!surveyUser.getUid().equals(uid)) {
            throw new ServiceException(ResultCode.USER_NOT_BIND_UID);
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
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_ACCOUNT_NOT_EXIST);
        if(surveyUser.getStatus()<0) throw new ServiceException(ResultCode.USER_FORBIDDEN);
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
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_ACCOUNT_NOT_EXIST);
        if(surveyUser.getStatus()<0) throw new ServiceException(ResultCode.USER_FORBIDDEN);
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

            String uid = surveyUser.getUid();
            if (uid == null) continue;
            if (uid.length() < 10)
            if (uid.contains("delete")) continue;


            QueryWrapper<SurveyOperator> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("uid", yituliuId);
            Long count = surveyOperatorMapper.selectCount(queryWrapper);


            //查询是否统计过这个用户
            QueryWrapper<SurveyStatisticsUser> userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper.eq("id", yituliuId);
            SurveyStatisticsUser savedData = surveyStatisticsUserMapper.selectOne(userQueryWrapper);


            if (savedData == null) {
                SurveyStatisticsUser statistics = new SurveyStatisticsUser();
                statistics.setId(yituliuId);
                statistics.setUid(uid);
                statistics.setOperatorCount(count.intValue());

            } else {
                savedData.setUid(uid);
                savedData.setOperatorCount(count.intValue());
                surveyStatisticsUserMapper.update(savedData, userQueryWrapper);
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
     * @param userName
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
     * @param passWord
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


}
