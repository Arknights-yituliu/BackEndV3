package com.lhs.service.survey.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.hypergryph.PlayerBinding;
import com.lhs.entity.dto.survey.EmailRequestDTO;
import com.lhs.entity.dto.survey.LoginDataDTO;
import com.lhs.entity.dto.survey.UpdateUserDataDTO;
import com.lhs.entity.dto.util.EmailFormDTO;
import com.lhs.entity.po.survey.AkPlayerBindInfoV2;
import com.lhs.entity.po.survey.UserInfo;
import com.lhs.entity.vo.survey.AKPlayerBindingListVO;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.survey.AkPlayerBindInfoV2Mapper;
import com.lhs.mapper.survey.SurveyUserMapper;
import com.lhs.service.survey.HypergryphService;
import com.lhs.service.survey.SurveyUserService;
import com.lhs.service.util.Email163Service;
import com.lhs.service.util.OSSService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SurveyUserServiceImpl implements SurveyUserService {

    private final SurveyUserMapper surveyUserMapper;

    private final RedisTemplate<String, String> redisTemplate;

    private final Email163Service email163Service;

    private final AkPlayerBindInfoV2Mapper akPlayerBindInfoV2Mapper;

    private final OSSService ossService;

    private final IdGenerator idGenerator;
    private final HypergryphService hypergryphService;

    public SurveyUserServiceImpl(SurveyUserMapper surveyUserMapper,
                                 RedisTemplate<String, String> redisTemplate,
                                 Email163Service email163Service,
                                 OSSService ossService,
                                 AkPlayerBindInfoV2Mapper akPlayerBindInfoV2Mapper,
                                 HypergryphService hypergryphService) {
        this.surveyUserMapper = surveyUserMapper;
        this.redisTemplate = redisTemplate;
        this.email163Service = email163Service;
        this.ossService = ossService;
        this.akPlayerBindInfoV2Mapper = akPlayerBindInfoV2Mapper;
        this.hypergryphService = hypergryphService;
        idGenerator = new IdGenerator(1L);
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

        //当前时间
        Date date = new Date();
        //一图流id 当前时间戳加随机4位数字
        long userId = idGenerator.nextId();


        //获取用户名，密码，邮箱
        String userName = loginDataDTO.getUserName().trim();
        String passWord = loginDataDTO.getPassword().trim();
        String email = loginDataDTO.getEmail().trim();

        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getUserName, userName);
        UserInfo userInfo = surveyUserMapper.selectOne(queryWrapper);
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
        userInfoNew.setEmail(email);
        userInfoNew.setCreateTime(date);
        userInfoNew.setUpdateTime(date);
        userInfoNew.setStatus(1);
        userInfoNew.setDeleteFlag(false);

        surveyUserMapper.insert(userInfoNew);

        return userInfoNew;
    }


    private UserInfo registerByEmail(LoginDataDTO loginDataDTO, String ipAddress) {

        //当前时间
        Date date = new Date();
        //一图流id
        long userId = idGenerator.nextId();
        String email = loginDataDTO.getEmail().trim();
        //用户输入的验证码
        String inputCode = loginDataDTO.getEmailCode();

        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getEmail, email);
        UserInfo userInfo = surveyUserMapper.selectOne(queryWrapper);
        if (userInfo != null) {
            throw new ServiceException(ResultCode.USER_IS_EXIST);
        }

        //检查验证码
        email163Service.compareVerificationCode(inputCode, "CODE:CODE." + email);
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

        surveyUserMapper.insert(userInfoNew);

        return userInfoNew;
    }

    @Override
    public HashMap<String, Object> loginV3(HttpServletRequest httpServletRequest, LoginDataDTO loginDataDTO) {

        //账号类型
        String accountType = loginDataDTO.getAccountType();
        //账号类型不能为空或未知
        if (accountType == null || "undefined".equals(accountType) || "null".equals(accountType)) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }

        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);
        UserInfo userInfo = null;

        if ("email".equals(accountType)) {
            userInfo = loginByEmail(loginDataDTO, ipAddress);
        }

        if ("password".equals(accountType)) {
            userInfo = loginByPassword1(loginDataDTO, ipAddress);
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
        String email = loginDataDTO.getEmail().trim();
        //用户输入的邮件验证码
        String inputEmailCode = loginDataDTO.getEmailCode().trim();
        //检查验证码
        email163Service.compareVerificationCode(inputEmailCode, "CODE:CODE." + email);

        Logger.info("用户使用邮箱登录：" + email);



        //设置查询构造器条件
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getEmail, email);

        UserInfo userInfo = surveyUserMapper.selectOne(queryWrapper);
        //查询用户是否存在
        if (userInfo == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }
        //存在直接返回
        return userInfo;

    }

    private UserInfo loginByHGToken(LoginDataDTO loginDataDTO, String ipAddress) {
        Logger.info("用户使用token登录");
        String hgToken = loginDataDTO.getHgToken();
        //获取默认的方舟绑定信息和方舟绑定信息列表
        AKPlayerBindingListVO akPlayerBindingListVO = hypergryphService.getPlayerBindingsByHGToken(hgToken);
        //默认的方舟绑定信息
        PlayerBinding playerBinding = akPlayerBindingListVO.getPlayerBinding();
        //默认的方舟uid
        String akUid = playerBinding.getUid();
        //根据默认的方舟uid查询本地的绑定信息表中是否存在这个uid的记录
        LambdaQueryWrapper<AkPlayerBindInfoV2> akPlayerBindInfoV2LambdaQueryWrapper = new LambdaQueryWrapper<>();
        akPlayerBindInfoV2LambdaQueryWrapper.eq(AkPlayerBindInfoV2::getAkUid, akUid);
        List<AkPlayerBindInfoV2> akPlayerBindInfoV2List = akPlayerBindInfoV2Mapper
                .selectList(akPlayerBindInfoV2LambdaQueryWrapper);

        //如果查询到记录
        if (!akPlayerBindInfoV2List.isEmpty()) {
            AkPlayerBindInfoV2 akPlayerBindInfoV2 = akPlayerBindInfoV2List.get(0);
            //根据本地的绑定信息表最后活跃的账号进行查询
            for (AkPlayerBindInfoV2 element : akPlayerBindInfoV2List) {
                if (akPlayerBindInfoV2.getLastActiveTime() < element.getLastActiveTime()) {
                    akPlayerBindInfoV2 = element;
                }
            }
            //根据这最后一个信息进行查询
            LambdaQueryWrapper<UserInfo> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userLambdaQueryWrapper.eq(UserInfo::getId, akPlayerBindInfoV2.getUid());
            UserInfo userInfo = surveyUserMapper.selectOne(userLambdaQueryWrapper);
            if (userInfo != null) {
                return userInfo;
            }
        }

        String nickName = playerBinding.getNickName();
        String[] split = nickName.split("#");
        nickName = split[0];

        //一图流id 当前时间戳加随机4位数字
        long userId = idGenerator.nextId();
        //用户初始状态
        int status = 1;
        //当前时间
        Date date = new Date();

        String userName = nickName + "ID" + userId;

        UserInfo userInfoNew = UserInfo.builder()
                .id(userId)
                .ip(ipAddress)
                .createTime(date)
                .updateTime(date)
                .deleteFlag(false)
                .status(1)
                .avatar("char_377_gdglow")
                .build();

        LambdaQueryWrapper<UserInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserInfo::getUserName, userName);
        List<UserInfo> userInfos = surveyUserMapper.selectList(lambdaQueryWrapper);
        if (userInfos.isEmpty()) {
            userInfoNew.setUserName(userName);
        }


        surveyUserMapper.insert(userInfoNew);

        return userInfoNew;
    }

    private UserInfo loginByPassword1(LoginDataDTO loginDataDTO, String ipAddress) {
        Logger.info("账号密码方式登录：");
        String userName = loginDataDTO.getUserName().trim();
        String passWord = loginDataDTO.getPassword().trim();


        //判断用户名/邮箱登录
        LambdaQueryWrapper<UserInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserInfo::getUserName, userName)
                .or()
                .eq(UserInfo::getEmail, userName);
        UserInfo userInfo = surveyUserMapper.selectOne(lambdaQueryWrapper);

        if (userInfo == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }


        // 密码加密
        passWord = AES.encrypt(passWord, ConfigUtil.Secret);
        // 获取用户密码
        String encryptedPasswordFromDB = userInfo.getPassword();
        // 对比加密后的密码是否与数据库中存储的加密密码相等
        if (!encryptedPasswordFromDB.equals(passWord)) {
            throw new ServiceException(ResultCode.USER_PASSWORD_ERROR);
        }


        return userInfo;

    }

    @Override
    public UserInfoVO registerV2(String ipAddress, LoginDataDTO loginDataDTO) {
        //注册类型
        String accountType = loginDataDTO.getAccountType();

        //注册类型不能为空或未知
        if (accountType == null || "undefined".equals(accountType) || "null".equals(accountType)) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }

        //获取用户名，密码，邮箱
        String userName = loginDataDTO.getUserName().trim();
        String passWord = loginDataDTO.getPassword().trim();
        String email = loginDataDTO.getEmail().trim();

//        Logger.info("用户输入的信息："+loginDataDTO);

        //当前时间
        Date date = new Date();
        //一图流id 当前时间戳加随机4位数字
        long userId = idGenerator.nextId();
        //用户初始状态
        int status = 1;

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        //组装新用户信息
        UserInfo userInfoNew = UserInfo.builder()
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
            UserInfo userInfo = surveyUserMapper.selectOne(queryWrapper);
            if (userInfo != null) throw new ServiceException(ResultCode.USER_IS_EXIST);
            //给用户信息写入用户名
            userInfoNew.setUserName(userName);
            //检查密码格式
            checkPassWord(passWord);
            //密码加密
            passWord = AES.encrypt(passWord, ConfigUtil.Secret);
            // 更新用户状态为有密码
            status = UserStatus.addPermission(status, UserStatusCode.HAS_PASSWORD);
            //给用户信息写入密码
            userInfoNew.setPassword(passWord);
            Logger.info("账号密码注册——用户名：" + userName + "密码：" + passWord);

        }

        if ("emailCode".equals(accountType)) {
            //查看邮箱是否注册过
            queryWrapper.eq("email", email);
            UserInfo userInfo = surveyUserMapper.selectOne(queryWrapper);
            if (userInfo != null) throw new ServiceException(ResultCode.USER_IS_EXIST);
            //用户输入的验证码
            String inputCode = loginDataDTO.getEmailCode();
            //检查验证码
            email163Service.compareVerificationCode(inputCode, "CODE:CODE." + email);
            //更新用户状态为有密码
            status = UserStatus.addPermission(status, UserStatusCode.HAS_EMAIL);
            //给用户设置初始昵称
            userName = "博士" + idGenerator.nextId();
            userInfoNew.setUserName(userName);
            userInfoNew.setEmail(email);
            Logger.info("账号密码注册——邮箱：" + email + "验证码：" + inputCode);
        }

        //给用户写入状态
        userInfoNew.setStatus(status);
        userInfoNew.setDeleteFlag(false);
        userInfoNew.setAvatar("char_377_gdglow");

        surveyUserMapper.insert(userInfoNew);


        String token = tokenGenerator(userInfoNew);

        UserInfoVO response = new UserInfoVO();  //返回的用户信息实体类  包括凭证，用户名，用户状态等
        response.setUserName(userInfoNew.getUserName());
        response.setToken(token);
        response.setStatus(userInfoNew.getStatus());
        response.setEmail(userInfoNew.getEmail());
        response.setAvatar("char_377_gdglow");

        return response;
    }


    @Override
    public UserInfoVO loginV2(String ipAddress, LoginDataDTO loginDataDto) {
        String accountType = loginDataDto.getAccountType();

        if (accountType == null || "undefined".equals(accountType) || "null".equals(accountType)) {
            throw new ServiceException(ResultCode.PARAM_TYPE_BIND_ERROR);
        }

        UserInfo userInfo = null;

        if ("passWord".equals(accountType)) {
            userInfo = loginByPassWordOld(loginDataDto);
        }

        if ("emailCode".equals(accountType)) {
            userInfo = loginByEmailCode(loginDataDto);
        }

        if (userInfo == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }


        String token = tokenGenerator(userInfo);

        //用户信息,包括凭证，用户名，用户状态等
        UserInfoVO response = getUserDataVO(userInfo);
        response.setToken(token);
        return response;
    }

    @Override
    public UserInfoVO getUserInfo(String token) {

        if (token == null || "undefined".equals(token) || "null".equals(token)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }


        UserInfo userInfo = getSurveyUserByToken(token);
        //用户信息 包括凭证，用户名，用户状态等

        UserInfoVO userInfoVO = getUserDataVO(userInfo);
        userInfoVO.setToken(token);
        return userInfoVO;
    }


    public UserInfo loginByPassWordOld(LoginDataDTO loginDataDto) {
        String userName = loginDataDto.getUserName().trim();
        String passWord = loginDataDto.getPassword().trim();

        passWord = AES.encrypt(passWord, ConfigUtil.Secret); //密码加密

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();

        //判断用户名/邮箱登录
        if (userName.contains("@")) {
            queryWrapper.eq("email", userName);  //邮箱
        } else {
            queryWrapper.eq("user_name", userName); //用户名
        }

        //查询用户信息是否存在
        UserInfo userInfo = surveyUserMapper.selectOne(queryWrapper);
        if (userInfo == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);

        //用户是否设置了密码
        if (UserStatus.hasPermission(userInfo.getStatus(), UserStatusCode.HAS_PASSWORD)) {
            //对比密码是否正确
            if (!userInfo.getPassword().equals(passWord)) {
                throw new ServiceException(ResultCode.USER_PASSWORD_OR_ACCOUNT_ERROR);
            }
        }
        return userInfo;
    }

    /**
     * 通过邮箱验证码登录
     *
     * @param loginDataDto 前端传来的邮箱和验证码
     * @return 用户数据
     */
    private UserInfo loginByEmailCode(LoginDataDTO loginDataDto) {
        String email = loginDataDto.getEmail().trim();
        String emailCode = loginDataDto.getEmailCode().trim();

        //设置查询构造器条件
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);

        //查询用户是否存在
        UserInfo userInfo = surveyUserMapper.selectOne(queryWrapper);
        if (userInfo == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);

        //对比输入的验证码和redis中的验证码
        email163Service.compareVerificationCode(emailCode, "CODE:CODE." + email);

        return userInfo;
    }


    @Override
    public void sendVerificationCode(EmailRequestDTO emailRequestDto) {
        String mailUsage = emailRequestDto.getMailUsage();
        //注册
        if ("register".equals(mailUsage)) {
            sendEmailForRegister(emailRequestDto);
        }
        //登录
        if ("login".equals(mailUsage)) {
            sendEmailForLogin(emailRequestDto);
        }
        //修改邮箱
        if ("UpdateEmail".equals(mailUsage)) {
            sendEmailForUpdateEmail(emailRequestDto);
        }
    }

    /**
     * 设置注册验证码的邮件信息
     * @param emailRequestDto 邮件信息
     */
    public void sendEmailForRegister(EmailRequestDTO emailRequestDto) {
        String emailAddress = emailRequestDto.getEmail().trim();
        //设置查询构造器条件
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", emailAddress);
        //查询是否有绑定这个邮箱的用户
        UserInfo userInfoByEmail = surveyUserMapper.selectOne(queryWrapper);
        if (userInfoByEmail != null) throw new ServiceException(ResultCode.USER_IS_EXIST);

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
     * @param emailRequestDto 前端输入的收件人地址
     */
    public void sendEmailForLogin(EmailRequestDTO emailRequestDto) {
        String emailAddress = emailRequestDto.getEmail().trim();  //收件人地址
        //设置查询构造器条件
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", emailAddress);
        //查询是否有绑定这个邮箱的用户
        UserInfo userInfoByEmail = surveyUserMapper.selectOne(queryWrapper);
        if (userInfoByEmail == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
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

    /**
     * 设置修改邮箱验证码的邮件信息
     * @param emailRequestDto 邮件信息
     */
    private void sendEmailForUpdateEmail(EmailRequestDTO emailRequestDto) {
        String emailAddress = emailRequestDto.getEmail().trim();
        String token = emailRequestDto.getToken();  //用户凭证
        //变更邮箱
        getSurveyUserByToken(token);

        Integer code = email163Service.CreateVerificationCode(emailAddress, 9999);
        String subject = "一图流邮箱变更验证码";
        String text = "您本次变更邮箱验证码：" + code + ",验证码有效时间5分钟";

        EmailFormDTO emailFormDTO = new EmailFormDTO();
        emailFormDTO.setFrom("ark_yituliu@163.com");
        emailFormDTO.setTo(emailAddress);
        emailFormDTO.setSubject(subject);
        emailFormDTO.setText(text);
        email163Service.sendSimpleEmail(emailFormDTO);
    }

    @Override
    public UserInfoVO updateUserData(UpdateUserDataDTO updateUserDataDto) {

        String property = updateUserDataDto.getProperty();
        String token = updateUserDataDto.getToken();
        UserInfo userInfoByToken = getSurveyUserByToken(token);

        if ("email".equals(property)) {
            return updateOrBindEmail(userInfoByToken, updateUserDataDto);
        }

        if ("passWord".equals(property)) {
            return updatePassWord(userInfoByToken, updateUserDataDto);
        }

        if ("userName".equals(property)) {
            return updateUserName(userInfoByToken, updateUserDataDto);
        }

        if ("avatar".equals(property)) {
            return updateUserAvatar(userInfoByToken, updateUserDataDto);
        }

        throw new ServiceException(ResultCode.PARAM_IS_INVALID);

    }

    /**
     * 更新或绑定邮箱
     *
     * @param userInfoByToken 用户信息
     * @param updateUserDataDto 用户修改的信息
     * @return 成功信息
     */
    private UserInfoVO updateOrBindEmail(UserInfo userInfoByToken, UpdateUserDataDTO updateUserDataDto) {
        String email = updateUserDataDto.getEmail().trim();
        String emailCode = updateUserDataDto.getEmailCode().trim();

        //对比用户输入的验证码和后台的验证码
        email163Service.compareVerificationCode(emailCode, "CODE:CODE." + email);
        //设置用户邮箱
        userInfoByToken.setEmail(email);
        if (!UserStatus.hasPermission(userInfoByToken.getStatus(), UserStatusCode.HAS_EMAIL)) {
            userInfoByToken.setStatus(UserStatus.addPermission(userInfoByToken.getStatus(), UserStatusCode.HAS_EMAIL));
        }

        backupSurveyUser(userInfoByToken);
        UserInfoVO response = new UserInfoVO();
        response.setStatus(userInfoByToken.getStatus());
        return response;
    }

    /**
     * 更新密码
     *
     * @param userInfoByToken 用户信息
     * @param updateUserDataDto 用户修改的信息
     * @return 用户新信息
     */
    private UserInfoVO updatePassWord(UserInfo userInfoByToken, UpdateUserDataDTO updateUserDataDto) {


        String newPassWord = updateUserDataDto.getNewPassWord().trim(); //新密码
        //检查新密码格式
        checkPassWord(newPassWord);
        //加密新密码
        newPassWord = AES.encrypt(newPassWord, ConfigUtil.Secret);
        //用户状态
        Integer status = userInfoByToken.getStatus();

        if (UserStatus.hasPermission(status, UserStatusCode.HAS_PASSWORD)) {//替换旧密码
            //旧密码
            String oldPassWord = updateUserDataDto.getOldPassWord().trim();
            //加密旧密码
            oldPassWord = AES.encrypt(oldPassWord, ConfigUtil.Secret);
            //检查旧密码是否正确
            if (userInfoByToken.getPassword().equals(oldPassWord)) {
                //更新旧密码为新密码
                userInfoByToken.setPassword(newPassWord);
            } else {
                throw new ServiceException(ResultCode.USER_PASSWORD_ERROR);
            }
        } else { //设置新密码
            //更新用户状态
            status = UserStatus.addPermission(status, UserStatusCode.HAS_PASSWORD);
            //想用户信息写入密码和状态
            userInfoByToken.setPassword(newPassWord);
            userInfoByToken.setStatus(status);
        }

        backupSurveyUser(userInfoByToken);

        UserInfoVO response = new UserInfoVO();
        response.setStatus(userInfoByToken.getStatus());

        return response;
    }

    /**
     * 更新用户名
     *
     * @param userInfoByToken 用户信息
     * @param updateUserDataDto 用户修改的信息
     * @return 用户新信息
     */
    private UserInfoVO updateUserName(UserInfo userInfoByToken, UpdateUserDataDTO updateUserDataDto) {
        String userName = updateUserDataDto.getUserName().trim();
        //检查用户名格式
        checkUserName(userName);
        //查询更新的用户名是否有同名的
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        if (surveyUserMapper.selectOne(queryWrapper) != null) throw new ServiceException(ResultCode.USER_IS_EXIST);

        //通过判断用户是否绑定了邮箱或设置了密码，用来区分v2版本注册的用户
        if (UserStatus.hasPermission(userInfoByToken.getStatus(), UserStatusCode.HAS_PASSWORD) ||
                UserStatus.hasPermission(userInfoByToken.getStatus(), UserStatusCode.HAS_EMAIL)) {
            //用户信息写入新用户名
            userInfoByToken.setUserName(userName);
            //备份用户信息
            backupSurveyUser(userInfoByToken);
        } else {
            throw new ServiceException(ResultCode.NOT_SET_PASSWORD_OR_BIND_EMAIL);
        }

        UserInfoVO response = new UserInfoVO();
        response.setUserName(userInfoByToken.getUserName());
        return response;
    }

    private UserInfoVO updateUserAvatar(UserInfo userInfoByToken, UpdateUserDataDTO updateUserDataDto) {
        userInfoByToken.setAvatar(updateUserDataDto.getAvatar());
        UserInfoVO response = new UserInfoVO();
        response.setAvatar(userInfoByToken.getAvatar());
        backupSurveyUser(userInfoByToken);
        return response;
    }


    @Override
    public UserInfo getSurveyUserByToken(String token) {
        if (token == null || "undefined".equals(token)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
        Long yituliuId = decryptToken(token);

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", yituliuId);
        UserInfo userInfo = surveyUserMapper.selectOne(queryWrapper); //查询用户

        if (userInfo == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        if (userInfo.getStatus() < 0) throw new ServiceException(ResultCode.USER_FORBIDDEN);

        return userInfo;
    }


    @Override
    public void backupSurveyUser(UserInfo userInfo) {
        userInfo.setUpdateTime(new Date());
        surveyUserMapper.updateUserById(userInfo);   //更新用户表

        Long id = userInfo.getId();
        ossService.upload(JsonMapper.toJSONString(userInfo), "survey/user/info/" + id + ".json");
    }


    private UserInfoVO getUserDataVO(UserInfo userInfo) {
        UserInfoVO response = new UserInfoVO();
        response.setUid(userInfo.getId());
        response.setUserName(userInfo.getUserName());
        response.setStatus(userInfo.getStatus());
        response.setEmail(userInfo.getEmail());
        response.setAvatar(userInfo.getAvatar());
        response.setAkUid("0");
        response.setAkNickName(userInfo.getUserName());

        QueryWrapper<AkPlayerBindInfoV2> akPlayerBindInfoQueryWrapper = new QueryWrapper<>();
        akPlayerBindInfoQueryWrapper.eq("uid", userInfo.getId()).orderByDesc("last_active_time");
        List<AkPlayerBindInfoV2> akPlayerBindInfoV2List = akPlayerBindInfoV2Mapper.selectList(akPlayerBindInfoQueryWrapper);


        if (akPlayerBindInfoV2List.isEmpty()) {
            return response;
        }

        Logger.info("用户绑定了" + akPlayerBindInfoV2List.size() + "条方舟uid");
        for (AkPlayerBindInfoV2 akPlayerBindInfo : akPlayerBindInfoV2List) {

            if (akPlayerBindInfo.getDefaultFlag()) {
                response.setAkUid(akPlayerBindInfo.getAkUid());
                response.setAkNickName(akPlayerBindInfo.getAkNickName());
            }
        }

        response.setAkUid(akPlayerBindInfoV2List.get(0).getAkUid());

        return response;
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


    @Override
    public Integer getTableIndex(Long id) {
        return 1;
    }

    /**
     * 解密用户凭证
     * @param token 用户凭证
     * @return 一图流id
     */
    private Long decryptToken(String token) {
        String decrypt = AES.decrypt(token.replaceAll(" ", "+"), ConfigUtil.Secret);
        String idText = decrypt.split("\\.")[1];
        return Long.valueOf(idText);
    }


    /**
     * 检查用户名是否为中文，英文，数字
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


    private String getSuffix(Integer digit) {
        int random = new Random().nextInt(9999);
        String end4 = String.format("%0" + digit + "d", random);
        return "#" + end4;
    }


}
