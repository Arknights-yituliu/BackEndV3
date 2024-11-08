package com.lhs.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.hypergryph.PlayerBinding;
import com.lhs.entity.dto.user.AkPlayerBindInfoDTO;
import com.lhs.entity.dto.user.EmailRequestDTO;
import com.lhs.entity.dto.user.LoginDataDTO;
import com.lhs.entity.dto.user.UpdateUserDataDTO;
import com.lhs.entity.dto.util.EmailFormDTO;
import com.lhs.entity.po.user.AkPlayerBindInfo;
import com.lhs.entity.po.user.UserConfig;
import com.lhs.entity.po.user.UserExternalAccountBinding;
import com.lhs.entity.po.user.UserInfo;
import com.lhs.entity.vo.survey.AkPlayerBindingListVO;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.user.AkPlayerBindInfoMapper;
import com.lhs.mapper.user.UserConfigMapper;
import com.lhs.mapper.user.UserExternalAccountBindingMapper;
import com.lhs.mapper.user.UserInfoMapper;
import com.lhs.service.survey.HypergryphService;
import com.lhs.service.user.UserService;
import com.lhs.service.util.Email163Service;
import com.lhs.service.util.OSSService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {


    private final UserInfoMapper userInfoMapper;

    private final RedisTemplate<String, String> redisTemplate;

    private final Email163Service email163Service;


    private final OSSService ossService;

    private final IdGenerator idGenerator;
    private final HypergryphService HypergryphService;

    private final UserExternalAccountBindingMapper userExternalAccountBindingMapper;

    private final AkPlayerBindInfoMapper akPlayerBindInfoMapper;

    private final UserConfigMapper userConfigMapper;


    public UserServiceImpl(UserInfoMapper userInfoMapper,
                           RedisTemplate<String, String> redisTemplate,
                           Email163Service email163Service,
                           OSSService ossService,
                           HypergryphService HypergryphService,
                           UserExternalAccountBindingMapper userExternalAccountBindingMapper,
                           AkPlayerBindInfoMapper akPlayerBindInfoMapper, UserConfigMapper userConfigMapper) {
        this.userInfoMapper = userInfoMapper;
        this.redisTemplate = redisTemplate;
        this.email163Service = email163Service;
        this.ossService = ossService;
        this.HypergryphService = HypergryphService;
        this.userExternalAccountBindingMapper = userExternalAccountBindingMapper;
        this.akPlayerBindInfoMapper = akPlayerBindInfoMapper;
        this.userConfigMapper = userConfigMapper;
        idGenerator = new IdGenerator(1L);
    }

    @Override
    public String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        Logger.info("获取的用户token{}"+header);
        if (header != null && header.startsWith("Bearer ")) {
            return header.replace("Bearer ", "");
        }
        throw new ServiceException(ResultCode.USER_NOT_LOGIN);
    }


    @Override
    public HashMap<String, Object> registerV3(HttpServletRequest httpServletRequest, LoginDataDTO loginDataDTO) {
        //账号类型
        String accountType = loginDataDTO.getAccountType();
        //账号类型不能为空或未知
        if (accountType == null || "undefined".equals(accountType) || "null".equals(accountType)) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }

        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);
        UserInfo userInfo = null;

        if ("password".equals(accountType)) {
            userInfo = registerByPassword(loginDataDTO, ipAddress);
        }

        if ("email".equals(accountType)) {
            userInfo = registerByEmail(loginDataDTO, ipAddress);
        }

        if (userInfo == null) {
            throw new ServiceException(ResultCode.USER_SIGN_IN_ERROR);
        }

        String token = tokenGenerator(userInfo);
        HashMap<String, Object> result = new HashMap<>();
        result.put("token", token);
        return result;
    }


    private UserInfo registerByPassword(LoginDataDTO loginDataDTO, String ipAddress) {

        //获取用户名，密码，邮箱
        String userName = loginDataDTO.getUserName();
        String passWord = loginDataDTO.getPassword();
        String email = loginDataDTO.getEmail();

        if (!checkParamsValidity(userName)) {
            throw new ServiceException(ResultCode.ACCOUNT_IS_BLANK);
        }

        if (!checkParamsValidity(passWord)) {
            throw new ServiceException(ResultCode.PASSWORD_IS_BLANK);
        }



        //当前时间
        Date date = new Date();
        //一图流id 当前时间戳加随机4位数字
        long userId = idGenerator.nextId();

        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getUserName, userName);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        if (userInfo != null) {
            throw new ServiceException(ResultCode.USER_IS_EXIST);
        }

        checkUserName(userName);
        checkPassWord(passWord);
        // 密码加密
        passWord = AES.encrypt(passWord, ConfigUtil.Secret);

        UserInfo userInfoNew = new UserInfo();

        userInfoNew.setId(userId);
        userInfoNew.setUserName(userName);
        userInfoNew.setPassword(passWord);
        userInfoNew.setAvatar("char_377_gdglow");
        userInfoNew.setIp(ipAddress);

        userInfoNew.setCreateTime(date);
        userInfoNew.setUpdateTime(date);
        userInfoNew.setStatus(1);
        userInfoNew.setDeleteFlag(false);

        if(checkParamsValidity(email)){
            LambdaQueryWrapper<UserInfo> emailQueryWrapper = new LambdaQueryWrapper<>();
            emailQueryWrapper.eq(UserInfo::getEmail,email);
            UserInfo userInfoByEmail = userInfoMapper.selectOne(emailQueryWrapper);
            if(userInfoByEmail!=null){
                throw new ServiceException(ResultCode.EMAIL_REGISTERED);
            }
            userInfoNew.setEmail(email);
        }

        userInfoMapper.insert(userInfoNew);

        return userInfoNew;
    }


    private UserInfo registerByEmail(LoginDataDTO loginDataDTO, String ipAddress) {

        //当前时间
        Date date = new Date();
        //一图流id
        long userId = idGenerator.nextId();
        String email = loginDataDTO.getEmail();
        //用户输入的验证码
        String verificationCode = loginDataDTO.getVerificationCode();

        if (!checkParamsValidity(email)) {
            throw new ServiceException(ResultCode.ACCOUNT_IS_BLANK);
        }

        if (!checkParamsValidity(verificationCode)) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_ERROR);
        }

        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getEmail, email);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        if (userInfo != null) {
            throw new ServiceException(ResultCode.USER_IS_EXIST);
        }

        //检查验证码
        email163Service.compareVerificationCode(verificationCode, "CODE:CODE." + email);
        //给用户设置初始昵称
        String userName = "博士" + idGenerator.nextId();
        UserInfo userInfoNew = new UserInfo();
        userInfoNew.setId(userId);
        userInfoNew.setUserName(userName);
        userInfoNew.setAvatar("char_377_gdglow");
        userInfoNew.setIp(ipAddress);
        userInfoNew.setEmail(email);
        userInfoNew.setCreateTime(date);
        userInfoNew.setUpdateTime(date);
        userInfoNew.setStatus(1);
        userInfoNew.setDeleteFlag(false);

        userInfoMapper.insert(userInfoNew);

        return userInfoNew;
    }

    @Override
    public HashMap<String, Object> loginV3(HttpServletRequest httpServletRequest, LoginDataDTO loginDataDTO) {

        //账号类型
        String accountType = loginDataDTO.getAccountType();
        //账号类型不能为空或未知
        if (!checkParamsValidity(accountType)) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);

        UserInfo userInfo = null;

        if ("email".equals(accountType)) {
            userInfo = loginByEmail(loginDataDTO, ipAddress);
        }

        if ("password".equals(accountType)) {
            userInfo = loginByPassword(loginDataDTO, ipAddress);
        }

        if ("hgToken".equals(accountType)) {
            userInfo = loginByHGToken(loginDataDTO, ipAddress);
        }

        if (userInfo == null) {
            throw new ServiceException(ResultCode.USER_SIGN_IN_ERROR);
        }

        String token = tokenGenerator(userInfo);

        HashMap<String, Object> result = new HashMap<>();
        result.put("token", token);

        return result;
    }

    private UserInfo loginByEmail(LoginDataDTO loginDataDTO, String ipAddress) {
        Logger.info("用户使用邮箱登录");

        String email = loginDataDTO.getEmail();
        //用户输入的邮件验证码
        String verificationCode = loginDataDTO.getVerificationCode();

        if (!checkParamsValidity(email)) {
            throw new ServiceException(ResultCode.ACCOUNT_IS_BLANK);
        }

        if (!checkParamsValidity(verificationCode)) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_ERROR);
        }

        //检查验证码
        email163Service.compareVerificationCode(verificationCode, "CODE:CODE." + email);

        //设置查询构造器条件
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getEmail, email);

        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        //查询用户是否存在
        if (userInfo == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }
        //存在直接返回
        return userInfo;

    }

    private UserInfo loginByHGToken(LoginDataDTO loginDataDTO, String ipAddress) {
//        Logger.info("用户使用token登录");
//        String hgToken = loginDataDTO.getHgToken();
//        if (!checkParamsValidity(hgToken)) {
//            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
//        }
//        //获取默认的方舟绑定信息和方舟绑定信息列表
//        AkPlayerBindingListVO akPlayerBindingListVO = HypergryphService.getPlayerBindingsByHGToken(hgToken);
//        //默认的方舟绑定信息
//        PlayerBinding playerBinding = akPlayerBindingListVO.getPlayerBinding();
//        //默认的方舟uid
//        String akUid = playerBinding.getUid();
//        //根据默认的方舟uid查询本地的绑定信息表中是否存在这个uid的记录
//        LambdaQueryWrapper<AkPlayerBindInfo> akPlayerBindInfoV2LambdaQueryWrapper = new LambdaQueryWrapper<>();
//        akPlayerBindInfoV2LambdaQueryWrapper.eq(AkPlayerBindInfo::getAkUid, akUid);
//        List<AkPlayerBindInfo> akPlayerBindInfoV2List = akPlayerBindInfoMapper
//                .selectList(akPlayerBindInfoV2LambdaQueryWrapper);
//
//        //如果查询到记录
//        if (!akPlayerBindInfoV2List.isEmpty()) {
//            AkPlayerBindInfo akPlayerBindInfo = akPlayerBindInfoV2List.get(0);
//            //根据本地的绑定信息表最后活跃的账号进行查询
//            for (AkPlayerBindInfo element : akPlayerBindInfoV2List) {
//                if (akPlayerBindInfo.getUpdateTime() < element.getUpdateTime()) {
//                    akPlayerBindInfo = element;
//                }
//            }
//            //根据这最后一个信息进行查询
//            LambdaQueryWrapper<UserInfo> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
//            userLambdaQueryWrapper.eq(UserInfo::getId, akPlayerBindInfo.getUid());
//            UserInfo userInfo = userInfoMapper.selectOne(userLambdaQueryWrapper);
//            if (userInfo != null) {
//                return userInfo;
//            }
//        }
//
//        String nickName = playerBinding.getNickName();
//        String[] split = nickName.split("#");
//        nickName = split[0];
//
//        //一图流id 当前时间戳加随机4位数字
//        long userId = idGenerator.nextId();
//        //用户初始状态
//        int status = 1;
//        //当前时间
//        Date date = new Date();
//
//        String userName = nickName + "ID" + userId;
//
//        UserInfo userInfoNew = UserInfo.builder()
//                .id(userId)
//                .ip(ipAddress)
//                .createTime(date)
//                .updateTime(date)
//                .deleteFlag(false)
//                .status(1)
//                .avatar("char_377_gdglow")
//                .build();
//
//        LambdaQueryWrapper<UserInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
//        lambdaQueryWrapper.eq(UserInfo::getUserName, userName);
//        List<UserInfo> userInfos = userInfoMapper.selectList(lambdaQueryWrapper);
//        if (userInfos.isEmpty()) {
//            userInfoNew.setUserName(userName);
//        }
//
//
//        userInfoMapper.insert(userInfoNew);

        return null;
    }

    private UserInfo loginByPassword(LoginDataDTO loginDataDTO, String ipAddress) {
        Logger.info("账号密码方式登录：");
        String userName = loginDataDTO.getUserName();
        String passWord = loginDataDTO.getPassword();

        if (!checkParamsValidity(userName)) {
            throw new ServiceException(ResultCode.ACCOUNT_IS_BLANK);
        }

        if (!checkParamsValidity(passWord)) {
            throw new ServiceException(ResultCode.PASSWORD_IS_BLANK);
        }


        //判断用户名/邮箱登录
        LambdaQueryWrapper<UserInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserInfo::getUserName, userName)
                .or()
                .eq(UserInfo::getEmail, userName);
        UserInfo userInfo = userInfoMapper.selectOne(lambdaQueryWrapper);

        if (userInfo == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }


        // 密码加密
        passWord = AES.encrypt(passWord, ConfigUtil.Secret);
        // 获取用户密码
        if (userInfo.getPassword() == null) {
            throw new ServiceException(ResultCode.NOT_SET_PASSWORD_OR_BIND_EMAIL);
        }
        String encryptedPasswordFromDB = userInfo.getPassword();
        // 对比加密后的密码是否与数据库中存储的加密密码相等
        if (!encryptedPasswordFromDB.equals(passWord)) {
            throw new ServiceException(ResultCode.USER_PASSWORD_ERROR);
        }
        return userInfo;
    }


    @Override
    public UserInfoVO getUserInfoVOByToken(String token) {

        Logger.info("要检验的用户token {} "+token);
        if (!checkParamsValidity(token)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        UserInfo userInfo = getUserInfoPOByToken(token);
        //用户信息 包括凭证，用户名，用户状态等
        UserInfoVO userInfoVO = getUserDataVO(userInfo);
        userInfoVO.setToken(token);
        return userInfoVO;
    }


    @Override
    public UserInfo getUserInfoPOByToken(String token) {
        if (!checkParamsValidity(token)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        Long yituliuId = decryptToken(token);

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", yituliuId);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper); //查询用户

        if (userInfo == null){
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }
        if (userInfo.getStatus() < 0) {
            throw new ServiceException(ResultCode.USER_FORBIDDEN);
        }

        return userInfo;
    }

    @Override
    public void sendVerificationCode(EmailRequestDTO emailRequestDto) {
        String mailUsage = emailRequestDto.getMailUsage();
        //注册
        if ("register".equals(mailUsage)) {
            sendVerificationCodeIsNotExistEmail(emailRequestDto);
        }
        //登录
        if ("login".equals(mailUsage)) {
            sendVerificationCodeIsExistEmail(emailRequestDto);
        }
        //修改邮箱
        if ("updateEmail".equals(mailUsage)) {
            sendVerificationCodeIsNotExistEmail(emailRequestDto);
        }
    }


    /**
     * 设置注册验证码的邮件信息
     *
     * @param emailRequestDto 邮件信息
     */
    private void sendVerificationCodeIsNotExistEmail(EmailRequestDTO emailRequestDto) {
        String emailAddress = emailRequestDto.getEmail();
        //设置查询构造器条件
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", emailAddress);
        //查询是否有绑定这个邮箱的用户
        UserInfo userInfoByEmail = userInfoMapper.selectOne(queryWrapper);


        if (userInfoByEmail != null) {
            throw new ServiceException(ResultCode.USER_IS_EXIST);
        }

        Integer code = email163Service.CreateVerificationCode(emailAddress, 9999);
        String subject = "一图流注册验证码";
        String text = "您本次注册验证码： " + code + ",验证码有效时间5分钟";

        EmailFormDTO emailFormDTO = new EmailFormDTO();
        emailFormDTO.setFrom("ark_yituliu@163.com");
        emailFormDTO.setTo(emailAddress);
        emailFormDTO.setSubject(subject);
        emailFormDTO.setText(text);
        email163Service.sendSimpleEmail(emailFormDTO);
    }

    /**
     * 设置登录验证码的邮件信息
     *
     * @param emailRequestDto 前端输入的收件人地址
     */
    private void sendVerificationCodeIsExistEmail(EmailRequestDTO emailRequestDto) {
        String emailAddress = emailRequestDto.getEmail();  //收件人地址
        //设置查询构造器条件
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", emailAddress);
        //查询是否有绑定这个邮箱的用户
        UserInfo userInfoByEmail = userInfoMapper.selectOne(queryWrapper);
        if (userInfoByEmail == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }
        Integer code = email163Service.CreateVerificationCode(emailAddress, 9999);
        String subject = "一图流登录验证码";
        String text = "您本次登录验证码：" + code + ",验证码有效时间5分钟";

        EmailFormDTO emailFormDTO = new EmailFormDTO();
        emailFormDTO.setFrom("ark_yituliu@163.com");
        emailFormDTO.setTo(emailAddress);
        emailFormDTO.setSubject(subject);
        emailFormDTO.setText(text);
        email163Service.sendSimpleEmail(emailFormDTO);
    }


    @Override
    public UserInfoVO updateUserData(UpdateUserDataDTO updateUserDataDto) {

        //兼容之前的命名
        String action = updateUserDataDto.getProperty()==null?updateUserDataDto.getAction(): updateUserDataDto.getProperty();

        String token = updateUserDataDto.getToken();
        UserInfo userInfoByToken = getUserInfoPOByToken(token);

        if ("email".equals(action)) {
            return updateOrBindEmail(userInfoByToken, updateUserDataDto);
        }

        if ("passWord".equals(action)) {
            return updatePassWord(userInfoByToken, updateUserDataDto);
        }

        if ("userName".equals(action)) {
            return updateUserName(userInfoByToken, updateUserDataDto);
        }

        if ("avatar".equals(action)) {
            return updateUserAvatar(userInfoByToken, updateUserDataDto);
        }

        throw new ServiceException(ResultCode.PARAM_IS_INVALID);

    }

    /**
     * 更新或绑定邮箱
     *
     * @param userInfo          用户信息
     * @param updateUserDataDto 用户修改的信息
     * @return 成功信息
     */
    private UserInfoVO updateOrBindEmail(UserInfo userInfo, UpdateUserDataDTO updateUserDataDto) {
        String email = updateUserDataDto.getEmail();
        String emailCode = updateUserDataDto.getEmailCode();

        //对比用户输入的验证码和后台的验证码
        email163Service.compareVerificationCode(emailCode, "CODE:CODE." + email);
        //设置用户邮箱
        userInfo.setEmail(email);

        UserInfoVO response = new UserInfoVO();
        response.setStatus(userInfo.getStatus());
        backupSurveyUser(userInfo);
        return response;
    }

    /**
     * 更新密码
     *
     * @param userInfo          用户信息
     * @param updateUserDataDto 用户修改的信息
     * @return 用户新信息
     */
    private UserInfoVO updatePassWord(UserInfo userInfo, UpdateUserDataDTO updateUserDataDto) {


        String newPassWord = updateUserDataDto.getNewPassWord(); //新密码
        //检查新密码格式
        checkPassWord(newPassWord);
        //加密新密码
        newPassWord = AES.encrypt(newPassWord, ConfigUtil.Secret);
        //用户状态
        Integer status = userInfo.getStatus();
        if (userInfo.getPassword() != null && userInfo.getPassword().length() > 5) {//替换旧密码
            //旧密码
            String oldPassWord = updateUserDataDto.getOldPassWord();
            //加密旧密码
            oldPassWord = AES.encrypt(oldPassWord, ConfigUtil.Secret);
            //检查旧密码是否正确
            if (userInfo.getPassword().equals(oldPassWord)) {
                //更新旧密码为新密码
                userInfo.setPassword(newPassWord);
            } else {
                throw new ServiceException(ResultCode.USER_PASSWORD_ERROR);
            }
        } else { //设置新密码
            //向用户信息写入密码和状态
            userInfo.setPassword(newPassWord);
        }


        backupSurveyUser(userInfo);
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setHasPassword(true);

        return userInfoVO;
    }

    /**
     * 更新用户名
     *
     * @param userInfo          用户信息
     * @param updateUserDataDto 用户修改的信息
     * @return 用户新信息
     */
    private UserInfoVO updateUserName(UserInfo userInfo, UpdateUserDataDTO updateUserDataDto) {
        String userName = updateUserDataDto.getUserName();
        //检查用户名格式
        checkUserName(userName);
        //查询更新的用户名是否有同名的
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        if (userInfoMapper.selectOne(queryWrapper) != null) {
            throw new ServiceException(ResultCode.USER_IS_EXIST);
        }

        //通过判断用户是否绑定了邮箱或设置了密码，用来区分v2版本注册的用户
        if (userInfo.getEmail() != null || userInfo.getPassword() != null) {
            //用户信息写入新用户名
            userInfo.setUserName(userName);
            //备份用户信息

        } else {
            throw new ServiceException(ResultCode.NOT_SET_PASSWORD_OR_BIND_EMAIL);
        }

        backupSurveyUser(userInfo);
        UserInfoVO response = new UserInfoVO();
        response.setUserName(userInfo.getUserName());
        return response;
    }

    private UserInfoVO updateUserAvatar(UserInfo userInfo, UpdateUserDataDTO updateUserDataDto) {
        userInfo.setAvatar(updateUserDataDto.getAvatar());
        UserInfoVO response = new UserInfoVO();
        response.setAvatar(userInfo.getAvatar());
        backupSurveyUser(userInfo);
        return response;
    }

    @Override
    public void backupSurveyUser(UserInfo userInfo) {
        userInfo.setUpdateTime(new Date());
        userInfoMapper.updateById(userInfo);  //更新用户表

        Long id = userInfo.getId();
        ossService.upload(JsonMapper.toJSONString(userInfo), "survey/user/info/" + id + ".json");
    }


    @Override
    public HashMap<String, String> retrieveAccount(LoginDataDTO loginDataDTO) {
        String accountType = loginDataDTO.getAccountType();
        if (!checkParamsValidity(accountType)) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }
        if ("email".equals(accountType)) {
           return retrieveAccountByEmail(loginDataDTO);
        }

        if ("hgToken".equals(accountType)) {
            return  retrieveAccountByHGToken(loginDataDTO);
        }

        if ("skland".equals(accountType)) {
            return retrieveAccountBySkland(loginDataDTO);
        }

        return null;
    }

    @Override
    public HashMap<String, String> resetAccount(LoginDataDTO loginDataDTO) {

        String tmpToken = loginDataDTO.getToken();
        String userId = redisTemplate.opsForValue().get(tmpToken);
        if(userId==null){
            throw new ServiceException(ResultCode.USER_PERMISSION_NO_ACCESS_OR_TIME_OUT);
        }

        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getId,userId);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        if(userInfo==null){
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }

        String newPassword = loginDataDTO.getPassword();
        if(!checkParamsValidity(newPassword)){
            throw new ServiceException(ResultCode.PASSWORD_IS_BLANK);
        }
        checkPassWord(newPassword);

        newPassword = AES.encrypt(newPassword,ConfigUtil.Secret);

        userInfo.setPassword(newPassword);

        userInfoMapper.updateById(userInfo);
        String token = tokenGenerator(userInfo);
        HashMap<String, String> result = new HashMap<>();
        result.put("token", token);

        return result;

    }

    @Override
    public Boolean saveUserExternalAccountBinding(UserExternalAccountBinding userExternalAccountBinding) {

        LambdaQueryWrapper<UserExternalAccountBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserExternalAccountBinding::getAkUid,userExternalAccountBinding.getAkUid())
                .eq(UserExternalAccountBinding::getUid,userExternalAccountBinding.getUid());
        UserExternalAccountBinding existsData = userExternalAccountBindingMapper.selectOne(queryWrapper);
        long timeStamp = System.currentTimeMillis();
        userExternalAccountBinding.setUpdateTime(timeStamp);

        boolean insert = false;
        Logger.info("要添加的外部账号绑定信息 {} "+ userExternalAccountBinding);
        if(existsData==null){
            userExternalAccountBinding.setId(idGenerator.nextId());
            userExternalAccountBinding.setCreateTime(timeStamp);
            userExternalAccountBinding.setDeleteFlag(false);
            int row = userExternalAccountBindingMapper.insert(userExternalAccountBinding);
            insert = row>0;
        }else {
            userExternalAccountBinding.setId(existsData.getId());
            userExternalAccountBinding.setCreateTime(existsData.getCreateTime());
            int i = userExternalAccountBindingMapper.updateById(userExternalAccountBinding);
        }

        return insert;
    }

    @Override
    public AkPlayerBindInfo getAkPlayerBindInfo(String akUid, Long uid) {
        //根据一图流用户uid和明日方舟玩家uid查询是否导入过森空岛数据
        LambdaQueryWrapper<AkPlayerBindInfo> bindQueryWrapper = new LambdaQueryWrapper<>();
        bindQueryWrapper.eq(AkPlayerBindInfo::getAkUid, akUid);
        return akPlayerBindInfoMapper.selectOne(bindQueryWrapper);
    }

    @Override
    public void saveAkPlayerBindInfo(AkPlayerBindInfo akPlayerBindInfo) {

        LambdaQueryWrapper<AkPlayerBindInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AkPlayerBindInfo::getAkUid,akPlayerBindInfo.getAkUid());
        AkPlayerBindInfo oldInfo = akPlayerBindInfoMapper.selectOne(queryWrapper);
        akPlayerBindInfo.setUpdateTime(System.currentTimeMillis());
        Logger.info("要添加的明日方舟账号绑定信息，id为"+akPlayerBindInfo);
        if(oldInfo==null){
            akPlayerBindInfo.setId(idGenerator.nextId());
            akPlayerBindInfo.setDeleteFlag(false);
            akPlayerBindInfoMapper.insert(akPlayerBindInfo);

        }else {
            akPlayerBindInfo.setId(oldInfo.getId());
            akPlayerBindInfoMapper.updateById(akPlayerBindInfo);
        }

    }

    @Override
    public void saveBindInfo(UserInfoVO userInfoVO, AkPlayerBindInfoDTO akPlayerBindInfoDTO) {
        UserExternalAccountBinding userExternalAccountBinding = new UserExternalAccountBinding();
        userExternalAccountBinding.setId(idGenerator.nextId());

        userExternalAccountBinding.setUid(userInfoVO.getUid());
        userExternalAccountBinding.setAkUid(akPlayerBindInfoDTO.getAkUid());

        saveUserExternalAccountBinding(userExternalAccountBinding);

        AkPlayerBindInfo akPlayerBindInfo = new AkPlayerBindInfo();
        akPlayerBindInfo.copyByAkPlayerBindInfoDTO(akPlayerBindInfoDTO);
        saveAkPlayerBindInfo(akPlayerBindInfo);

    }

    @Override
    public void updateUserConfig(Map<String, Object> config,HttpServletRequest httpServletRequest) {

        UserInfoVO userInfoVOByToken = getUserInfoVOByToken(extractToken(httpServletRequest));
        Long uid = userInfoVOByToken.getUid();
        UserConfig userConfig = userConfigMapper.selectById(uid);
        Date date = new Date();
        if(userConfig==null){
            userConfig = new UserConfig();
            String configText = JsonMapper.toJSONString(config);
            userConfig.setConfig(configText);
            userConfig.setUid(uid);
            userConfig.setCreateTime(date);
            userConfig.setUpdateTime(date);
            userConfigMapper.insert(userConfig);
        }else {
            String oldConfig = userConfig.getConfig();
            Map<String, Object> map = JsonMapper.parseObject(oldConfig, new TypeReference<>() {
            });
            map.putAll(config);
            String newConfig = JsonMapper.toJSONString(map);
            userConfig.setConfig(newConfig);
            userConfig.setUpdateTime(date);
            userConfigMapper.updateById(userConfig);
        }

    }

    private HashMap<String, String> retrieveAccountByEmail(LoginDataDTO loginDataDTO) {
        String email = loginDataDTO.getEmail();
        if (!checkParamsValidity(email)) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }
        //用户输入的邮件验证码
        String verificationCode = loginDataDTO.getVerificationCode();
        if (!checkParamsValidity(verificationCode)) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_ERROR);
        }

        //检查验证码
        email163Service.compareVerificationCode(verificationCode, "CODE:CODE." + email);


        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getEmail, email);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        if (userInfo == null) {
            throw new ServiceException(ResultCode.USER_NOT_BIND_EMAIL);
        }

        return createTemporaryCertificate(userInfo);
    }


    private HashMap<String, String> retrieveAccountBySkland(LoginDataDTO loginDataDTO) {
        String cred = loginDataDTO.getSklandCred();
        cred = cred.replaceAll("[\"']+", "");
        String[] split = cred.split(",");
        if (split.length < 2) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("cred", split[0]);
        hashMap.put("token", split[1]);
        AkPlayerBindingListVO akPlayerBindingListVO = HypergryphService.getPlayerBindingsBySkland(hashMap);
        return getUserInfoBySkland(akPlayerBindingListVO);
    }

    private HashMap<String, String> retrieveAccountByHGToken(LoginDataDTO loginDataDTO) {
        Logger.info("用户使用找回登录");
        String hgToken = loginDataDTO.getHgToken();
        if (!checkParamsValidity(hgToken)) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }
        //获取默认的方舟绑定信息和方舟绑定信息列表
        AkPlayerBindingListVO akPlayerBindingListVO = HypergryphService.getPlayerBindingsByHGToken(hgToken);
        return getUserInfoBySkland(akPlayerBindingListVO);
    }

    private HashMap<String, String> getUserInfoBySkland(AkPlayerBindingListVO akPlayerBindingListVO) {
        //默认的方舟绑定信息
        PlayerBinding playerBinding = akPlayerBindingListVO.getPlayerBinding();
        //默认的方舟uid
        String akUid = playerBinding.getUid();
        //根据默认的方舟uid查询本地的绑定信息表中是否存在这个uid的记录

        LambdaQueryWrapper<UserExternalAccountBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserExternalAccountBinding::getAkUid, akUid);
        List<UserExternalAccountBinding> externalAccountBindingList = userExternalAccountBindingMapper.
                selectList(queryWrapper);

        //如果查询到曾经导入过
        if (externalAccountBindingList.isEmpty()) {
            throw new ServiceException(ResultCode.USER_NOT_BIND_UID);
        }

        UserExternalAccountBinding externalAccountBinding = externalAccountBindingList.get(0);
        //根据本地的绑定信息表最后活跃的账号进行查询
        for (UserExternalAccountBinding element : externalAccountBindingList) {
            if (externalAccountBinding.getUpdateTime() < element.getUpdateTime()) {
                externalAccountBinding = element;
            }
        }


        //根据这最后一个信息进行查询
        LambdaQueryWrapper<UserInfo> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(UserInfo::getId, externalAccountBinding.getUid());
        UserInfo userInfo = userInfoMapper.selectOne(userLambdaQueryWrapper);

        if (userInfo == null) {
            throw new ServiceException(ResultCode.USER_NOT_BIND_UID);
        }

        return createTemporaryCertificate(userInfo);
    }


    private HashMap<String, String> createTemporaryCertificate(UserInfo userInfo) {
        String tmpToken = userInfo.getUserName() + userInfo.getId() + System.currentTimeMillis();

        tmpToken = AES.encrypt(tmpToken,
                ConfigUtil.Secret);
        redisTemplate.opsForValue().set(tmpToken, userInfo.getId().toString(), 10, TimeUnit.MINUTES);
        HashMap<String, String> result = new HashMap<>();
        result.put("tmpToken",tmpToken);
        result.put("userName", userInfo.getUserName());
        return result;
    }

    /**
     * 解密用户凭证
     *
     * @param token 用户凭证
     * @return 一图流id
     */
    private Long decryptToken(String token) {
        String decrypt = AES.decrypt(token.replaceAll(" ", "+"), ConfigUtil.Secret);
        String idText = decrypt.split("\\.")[1];
        return Long.valueOf(idText);
    }


    private String tokenGenerator(UserInfo userInfo) {
        //用户凭证  由用户部分信息+一图流id+时间戳 加密得到
        Map<String, Object> hashMap = new HashMap<>();
        String userName = userInfo.getUserName();
        Long id = userInfo.getId();
        hashMap.put("userName", userName.replace(".", "·"));
        hashMap.put("id", userInfo.getId());
        hashMap.put("ip", userInfo.getIp());
        String header = JsonMapper.toJSONString(hashMap);
        long timeStamp = System.currentTimeMillis();
        return AES.encrypt(header + "." + id + "." + timeStamp, ConfigUtil.Secret);
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
            char c = userName.charAt(i);
            // 检查中文字符
            if (c >= '一' && c <= '龥') {
                continue;
            }
            // 检查英文或数字字符
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                continue;
            }
            // 遇到除中英文数字之外的字符，直接抛出异常
            throw new ServiceException(ResultCode.USER_NAME_MUST_BE_IN_CHINESE_OR_ENGLISH);
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
            char c = passWord.charAt(i);
            // 检查英文或数字字符
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                continue;
            }
            // 遇到除中英文数字之外的字符，直接抛出异常
            throw new ServiceException(ResultCode.PASS_WORD_MUST_BE_IN_CHINESE_OR_ENGLISH);
        }

    }


    private static Boolean checkParamsValidity(String param) {

        if (param == null) {
            return false;
        }

        if ("undefined".equals(param) || "null".equals(param)) {
            return false;
        }


        if (param.isEmpty()) {
            return false;
        }

        return true;
    }


    private UserInfoVO getUserDataVO(UserInfo userInfo) {
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setUid(userInfo.getId());
        userInfoVO.setUserName(userInfo.getUserName());
        userInfoVO.setStatus(userInfo.getStatus());
        userInfoVO.setEmail(userInfo.getEmail());
        userInfoVO.setAvatar(userInfo.getAvatar());
        userInfoVO.setAkUid("0");
        userInfoVO.setAkNickName(userInfo.getUserName());

        LambdaQueryWrapper<UserExternalAccountBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserExternalAccountBinding::getUid, userInfo.getId()).orderByDesc(UserExternalAccountBinding::getUpdateTime);
        List<UserExternalAccountBinding> externalAccountBindings = userExternalAccountBindingMapper
                .selectList(queryWrapper);

        //根据uid查询是否有自定义配置
        UserConfig userConfig = userConfigMapper.selectById(userInfo.getId());
        //不为空则为VO写入配置
        if(userConfig!=null){
            Map<String, Object> map = JsonMapper.parseObject(userConfig.getConfig(), new TypeReference<>() {
            });
            userInfoVO.setConfig(map);
        }

        if (userInfo.getPassword() != null) {
            userInfoVO.setHasPassword(true);
        }
        if (userInfo.getEmail() != null) {
            userInfoVO.setHasEmail(true);
        }
        if (externalAccountBindings.isEmpty()) {
            return userInfoVO;
        }

        Logger.info("用户绑定了" + externalAccountBindings.size() + "条方舟uid");

        userInfoVO.setAkUid(externalAccountBindings.get(0).getAkUid());

        return userInfoVO;
    }

    private void ensureIdempotent(String key){
        Boolean lock = redisTemplate.opsForValue().setIfAbsent(key, "1", 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(lock)) {
            throw new ServiceException(ResultCode.NOT_REPEAT_REQUESTS);
        }
    }
}
