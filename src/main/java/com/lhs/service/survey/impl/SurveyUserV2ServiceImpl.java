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
import com.lhs.entity.po.survey.SurveyUser;
import com.lhs.entity.vo.survey.AKPlayerBindingListVO;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.survey.AkPlayerBindInfoV2Mapper;
import com.lhs.mapper.survey.SurveyUserMapper;
import com.lhs.service.survey.SklandService;
import com.lhs.service.survey.SurveyUserV2Service;
import com.lhs.service.util.Email163Service;
import com.lhs.service.util.OSSService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SurveyUserV2ServiceImpl implements SurveyUserV2Service {


    private final SurveyUserMapper surveyUserMapper;

    private final RedisTemplate<String, String> redisTemplate;

    private final Email163Service email163Service;

    private final AkPlayerBindInfoV2Mapper akPlayerBindInfoV2Mapper;

    private final OSSService ossService;

    private final IdGenerator idGenerator;
    private final SklandService sklandService;

    public SurveyUserV2ServiceImpl(SurveyUserMapper surveyUserMapper,
                                   RedisTemplate<String, String> redisTemplate,
                                   Email163Service email163Service,
                                   OSSService ossService,
                                   AkPlayerBindInfoV2Mapper akPlayerBindInfoV2Mapper,
                                   SklandService sklandService) {
        this.surveyUserMapper = surveyUserMapper;
        this.redisTemplate = redisTemplate;
        this.email163Service = email163Service;
        this.ossService = ossService;
        this.akPlayerBindInfoV2Mapper = akPlayerBindInfoV2Mapper;
        this.sklandService = sklandService;
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
        SurveyUser surveyUser = null;

        if ("password".equals(accountType)) {
            surveyUser = registerByPassword(loginDataDTO, ipAddress);
        }

        if ("email".equals(accountType)) {
            surveyUser = registerByEmail(loginDataDTO, ipAddress);
        }

        if (surveyUser == null) {
            throw new ServiceException(ResultCode.USER_SIGN_IN_ERROR);
        }

        String token = tokenGenerator(surveyUser);
        HashMap<String, Object> result = new HashMap<>();
        result.put("token", token);
        return result;
    }


    private SurveyUser registerByPassword(LoginDataDTO loginDataDTO, String ipAddress) {

        //获取用户名，密码，邮箱
        String userName = loginDataDTO.getUserName().trim();
        String passWord = loginDataDTO.getPassword().trim();
        String email = loginDataDTO.getEmail().trim();

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

        LambdaQueryWrapper<SurveyUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SurveyUser::getUserName, userName);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
        if (surveyUser != null) {
            throw new ServiceException(ResultCode.USER_EXISTED);
        }

        checkUserName(userName);
        checkPassWord(passWord);
        // 密码加密
        passWord = AES.encrypt(passWord, ConfigUtil.Secret);

        SurveyUser surveyUserNew = new SurveyUser();

        surveyUserNew.setId(userId);
        surveyUserNew.setUserName(userName);
        surveyUserNew.setPassword(passWord);
        surveyUserNew.setAvatar("char_377_gdglow");
        surveyUserNew.setIp(ipAddress);
        surveyUserNew.setEmail(email);
        surveyUserNew.setCreateTime(date);
        surveyUserNew.setUpdateTime(date);
        surveyUserNew.setStatus(1);
        surveyUserNew.setDeleteFlag(false);

        surveyUserMapper.insert(surveyUserNew);

        return surveyUserNew;
    }


    private SurveyUser registerByEmail(LoginDataDTO loginDataDTO, String ipAddress) {

        //当前时间
        Date date = new Date();
        //一图流id
        long userId = idGenerator.nextId();
        String email = loginDataDTO.getEmail().trim();
        //用户输入的验证码
        String verificationCode = loginDataDTO.getVerificationCode();

        if (!checkParamsValidity(email)) {
            throw new ServiceException(ResultCode.ACCOUNT_IS_BLANK);
        }

        if (!checkParamsValidity(verificationCode)) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_ERROR);
        }

        LambdaQueryWrapper<SurveyUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SurveyUser::getEmail, email);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
        if (surveyUser != null) {
            throw new ServiceException(ResultCode.USER_EXISTED);
        }

        //检查验证码
        email163Service.compareVerificationCode(verificationCode, "CODE:CODE." + email);
        //给用户设置初始昵称
        String userName = "博士" + idGenerator.nextId();
        SurveyUser surveyUserNew = new SurveyUser();
        surveyUserNew.setId(userId);
        surveyUserNew.setUserName(userName);
        surveyUserNew.setAvatar("char_377_gdglow");
        surveyUserNew.setIp(ipAddress);
        surveyUserNew.setEmail(email);
        surveyUserNew.setCreateTime(date);
        surveyUserNew.setUpdateTime(date);
        surveyUserNew.setStatus(1);
        surveyUserNew.setDeleteFlag(false);

        surveyUserMapper.insert(surveyUserNew);

        return surveyUserNew;
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

        SurveyUser surveyUser = null;

        if ("email".equals(accountType)) {
            surveyUser = loginByEmail(loginDataDTO, ipAddress);
        }

        if ("password".equals(accountType)) {
            surveyUser = loginByPassword(loginDataDTO, ipAddress);
        }

        if ("hgToken".equals(accountType)) {
            surveyUser = loginByHGToken(loginDataDTO, ipAddress);
        }

        if (surveyUser == null) {
            throw new ServiceException(ResultCode.USER_SIGN_IN_ERROR);
        }

        String token = tokenGenerator(surveyUser);

        HashMap<String, Object> result = new HashMap<>();
        result.put("token", token);

        return result;
    }

    private SurveyUser loginByEmail(LoginDataDTO loginDataDTO, String ipAddress) {
        String email = loginDataDTO.getEmail().trim();
        //用户输入的邮件验证码
        String verificationCode = loginDataDTO.getVerificationCode().trim();

        if (!checkParamsValidity(email)) {
            throw new ServiceException(ResultCode.ACCOUNT_IS_BLANK);
        }

        if (!checkParamsValidity(verificationCode)) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_ERROR);
        }

        //检查验证码
        email163Service.compareVerificationCode(verificationCode, "CODE:CODE." + email);

        Logger.info("用户使用邮箱登录：" + email);


        //设置查询构造器条件
        LambdaQueryWrapper<SurveyUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SurveyUser::getEmail, email);

        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
        //查询用户是否存在
        if (surveyUser == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }
        //存在直接返回
        return surveyUser;

    }

    private SurveyUser loginByHGToken(LoginDataDTO loginDataDTO, String ipAddress) {
        Logger.info("用户使用token登录");
        String hgToken = loginDataDTO.getHgToken();
        if (!checkParamsValidity(hgToken)) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }
        //获取默认的方舟绑定信息和方舟绑定信息列表
        AKPlayerBindingListVO akPlayerBindingListVO = sklandService.getPlayerBindings(hgToken);
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
            LambdaQueryWrapper<SurveyUser> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userLambdaQueryWrapper.eq(SurveyUser::getId, akPlayerBindInfoV2.getUid());
            SurveyUser surveyUser = surveyUserMapper.selectOne(userLambdaQueryWrapper);
            if (surveyUser != null) {
                return surveyUser;
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

        SurveyUser surveyUserNew = SurveyUser.builder()
                .id(userId)
                .ip(ipAddress)
                .createTime(date)
                .updateTime(date)
                .deleteFlag(false)
                .status(1)
                .avatar("char_377_gdglow")
                .build();

        LambdaQueryWrapper<SurveyUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SurveyUser::getUserName, userName);
        List<SurveyUser> surveyUsers = surveyUserMapper.selectList(lambdaQueryWrapper);
        if (surveyUsers.isEmpty()) {
            surveyUserNew.setUserName(userName);
        }


        surveyUserMapper.insert(surveyUserNew);

        return surveyUserNew;
    }

    private SurveyUser loginByPassword(LoginDataDTO loginDataDTO, String ipAddress) {
        Logger.info("账号密码方式登录：");
        String userName = loginDataDTO.getUserName().trim();
        String passWord = loginDataDTO.getPassword().trim();

        if (!checkParamsValidity(userName)) {
            throw new ServiceException(ResultCode.ACCOUNT_IS_BLANK);
        }

        if (!checkParamsValidity(passWord)) {
            throw new ServiceException(ResultCode.PASSWORD_IS_BLANK);
        }


        //判断用户名/邮箱登录
        LambdaQueryWrapper<SurveyUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SurveyUser::getUserName, userName)
                .or()
                .eq(SurveyUser::getEmail, userName);
        SurveyUser surveyUser = surveyUserMapper.selectOne(lambdaQueryWrapper);

        if (surveyUser == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }


        // 密码加密
        passWord = AES.encrypt(passWord, ConfigUtil.Secret);
        // 获取用户密码
        if (surveyUser.getPassword() == null) {
            throw new ServiceException(ResultCode.NOT_SET_PASSWORD_OR_BIND_EMAIL);
        }
        String encryptedPasswordFromDB = surveyUser.getPassword();
        // 对比加密后的密码是否与数据库中存储的加密密码相等
        if (!encryptedPasswordFromDB.equals(passWord)) {
            throw new ServiceException(ResultCode.USER_PASSWORD_ERROR);
        }
        return surveyUser;
    }


    @Override
    public UserInfoVO getUserInfo(String token) {

        if (!checkParamsValidity(token)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        SurveyUser surveyUser = getSurveyUserByToken(token);
        //用户信息 包括凭证，用户名，用户状态等
        UserInfoVO userInfoVO = getUserDataVO(surveyUser);
        userInfoVO.setToken(token);
        return userInfoVO;
    }


    @Override
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
     *
     * @param emailRequestDto 邮件信息
     */
    private void sendEmailForRegister(EmailRequestDTO emailRequestDto) {
        String emailAddress = emailRequestDto.getEmail().trim();


        //设置查询构造器条件
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", emailAddress);
        //查询是否有绑定这个邮箱的用户
        SurveyUser surveyUserByEmail = surveyUserMapper.selectOne(queryWrapper);

        if (surveyUserByEmail != null) {
            throw new ServiceException(ResultCode.USER_EXISTED);
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
    private void sendEmailForLogin(EmailRequestDTO emailRequestDto) {
        String emailAddress = emailRequestDto.getEmail().trim();  //收件人地址
        //设置查询构造器条件
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", emailAddress);
        //查询是否有绑定这个邮箱的用户
        SurveyUser surveyUserByEmail = surveyUserMapper.selectOne(queryWrapper);
        if (surveyUserByEmail == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
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
     *
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
        SurveyUser surveyUserByToken = getSurveyUserByToken(token);

        if ("email".equals(property)) {
            return updateOrBindEmail(surveyUserByToken, updateUserDataDto);
        }

        if ("passWord".equals(property)) {
            return updatePassWord(surveyUserByToken, updateUserDataDto);
        }

        if ("userName".equals(property)) {
            return updateUserName(surveyUserByToken, updateUserDataDto);
        }

        if ("avatar".equals(property)) {
            return updateUserAvatar(surveyUserByToken, updateUserDataDto);
        }

        throw new ServiceException(ResultCode.PARAM_IS_INVALID);

    }

    /**
     * 更新或绑定邮箱
     *
     * @param surveyUser        用户信息
     * @param updateUserDataDto 用户修改的信息
     * @return 成功信息
     */
    private UserInfoVO updateOrBindEmail(SurveyUser surveyUser, UpdateUserDataDTO updateUserDataDto) {
        String email = updateUserDataDto.getEmail().trim();
        String emailCode = updateUserDataDto.getEmailCode().trim();

        //对比用户输入的验证码和后台的验证码
        email163Service.compareVerificationCode(emailCode, "CODE:CODE." + email);
        //设置用户邮箱
        surveyUser.setEmail(email);

        UserInfoVO response = new UserInfoVO();
        response.setStatus(surveyUser.getStatus());
        backupSurveyUser(surveyUser);
        return response;
    }

    /**
     * 更新密码
     *
     * @param surveyUser        用户信息
     * @param updateUserDataDto 用户修改的信息
     * @return 用户新信息
     */
    private UserInfoVO updatePassWord(SurveyUser surveyUser, UpdateUserDataDTO updateUserDataDto) {


        String newPassWord = updateUserDataDto.getNewPassWord().trim(); //新密码
        //检查新密码格式
        checkPassWord(newPassWord);
        //加密新密码
        newPassWord = AES.encrypt(newPassWord, ConfigUtil.Secret);
        //用户状态
        Integer status = surveyUser.getStatus();

        if (surveyUser.getPassword() != null && surveyUser.getPassword().length() > 5) {//替换旧密码
            //旧密码
            String oldPassWord = updateUserDataDto.getOldPassWord().trim();
            //加密旧密码
            oldPassWord = AES.encrypt(oldPassWord, ConfigUtil.Secret);
            //检查旧密码是否正确
            if (surveyUser.getPassword().equals(oldPassWord)) {
                //更新旧密码为新密码
                surveyUser.setPassword(newPassWord);
            } else {
                throw new ServiceException(ResultCode.USER_PASSWORD_ERROR);
            }
        } else { //设置新密码
            //向用户信息写入密码和状态
            surveyUser.setPassword(newPassWord);
        }


        backupSurveyUser(surveyUser);
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setHasPassword(true);

        return userInfoVO;
    }

    /**
     * 更新用户名
     *
     * @param surveyUser        用户信息
     * @param updateUserDataDto 用户修改的信息
     * @return 用户新信息
     */
    private UserInfoVO updateUserName(SurveyUser surveyUser, UpdateUserDataDTO updateUserDataDto) {
        String userName = updateUserDataDto.getUserName().trim();
        //检查用户名格式
        checkUserName(userName);
        //查询更新的用户名是否有同名的
        QueryWrapper<SurveyUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        if (surveyUserMapper.selectOne(queryWrapper) != null) {
            throw new ServiceException(ResultCode.USER_EXISTED);
        }

        //通过判断用户是否绑定了邮箱或设置了密码，用来区分v2版本注册的用户
        if (surveyUser.getEmail() != null || surveyUser.getPassword() != null) {
            //用户信息写入新用户名
            surveyUser.setUserName(userName);
            //备份用户信息

        } else {
            throw new ServiceException(ResultCode.NOT_SET_PASSWORD_OR_BIND_EMAIL);
        }

        backupSurveyUser(surveyUser);
        UserInfoVO response = new UserInfoVO();
        response.setUserName(surveyUser.getUserName());
        return response;
    }

    private UserInfoVO updateUserAvatar(SurveyUser surveyUser, UpdateUserDataDTO updateUserDataDto) {
        surveyUser.setAvatar(updateUserDataDto.getAvatar());
        UserInfoVO response = new UserInfoVO();
        response.setAvatar(surveyUser.getAvatar());
        backupSurveyUser(surveyUser);
        return response;
    }

    @Override
    public void backupSurveyUser(SurveyUser surveyUser) {
        surveyUser.setUpdateTime(new Date());
        surveyUserMapper.updateById(surveyUser);  //更新用户表

        Long id = surveyUser.getId();
        ossService.upload(JsonMapper.toJSONString(surveyUser), "survey/user/info/" + id + ".json");
    }


    @Override
    public void retrieveAccount(LoginDataDTO loginDataDTO) {
        String accountType = loginDataDTO.getAccountType().trim();
        if (!checkParamsValidity(accountType)) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }
        if ("email".equals(accountType)) {
            retrieveAccountByEmail(loginDataDTO);
        }

        if ("hgToken".equals(accountType)) {
            retrieveAccountByHGToken(loginDataDTO);
        }

        if ("skland".equals(accountType)) {

        }

    }

    private void retrieveAccountByEmail(LoginDataDTO loginDataDTO) {
        String email = loginDataDTO.getEmail().trim();
        if (!checkParamsValidity(email)) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }

        LambdaQueryWrapper<SurveyUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SurveyUser::getEmail, email);
        SurveyUser surveyUser = surveyUserMapper.selectOne(queryWrapper);
        if (surveyUser == null) {
            throw new ServiceException(ResultCode.USER_NOT_BIND_EMAIL);
        }

        createTemporaryCertificate(surveyUser);
    }


    private void retrieveAccountBySkland(LoginDataDTO loginDataDTO){
        String cred = loginDataDTO.getSklandCred().trim();
        cred = cred.replaceAll("[\"']+", "");
        String[] split = cred.split("\\.");

    }

    private void retrieveAccountByHGToken(LoginDataDTO loginDataDTO) {
        Logger.info("用户使用找回登录");
        String hgToken = loginDataDTO.getHgToken();
        if (!checkParamsValidity(hgToken)) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }
        //获取默认的方舟绑定信息和方舟绑定信息列表
        AKPlayerBindingListVO akPlayerBindingListVO = sklandService.getPlayerBindings(hgToken);
        //默认的方舟绑定信息
        PlayerBinding playerBinding = akPlayerBindingListVO.getPlayerBinding();
        //默认的方舟uid
        String akUid = playerBinding.getUid();
        //根据默认的方舟uid查询本地的绑定信息表中是否存在这个uid的记录
        LambdaQueryWrapper<AkPlayerBindInfoV2> akPlayerBindInfoV2LambdaQueryWrapper = new LambdaQueryWrapper<>();
        akPlayerBindInfoV2LambdaQueryWrapper.eq(AkPlayerBindInfoV2::getAkUid, akUid);
        List<AkPlayerBindInfoV2> akPlayerBindInfoV2List = akPlayerBindInfoV2Mapper
                .selectList(akPlayerBindInfoV2LambdaQueryWrapper);

        //如果查询到曾经导入过
        if (akPlayerBindInfoV2List.isEmpty()) {
            throw new ServiceException(ResultCode.USER_NOT_BIND_UID);
        }

        AkPlayerBindInfoV2 akPlayerBindInfoV2 = akPlayerBindInfoV2List.get(0);
        //根据本地的绑定信息表最后活跃的账号进行查询
        for (AkPlayerBindInfoV2 element : akPlayerBindInfoV2List) {
            if (akPlayerBindInfoV2.getLastActiveTime() < element.getLastActiveTime()) {
                akPlayerBindInfoV2 = element;
            }
        }
        //根据这最后一个信息进行查询
        LambdaQueryWrapper<SurveyUser> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(SurveyUser::getId, akPlayerBindInfoV2.getUid());
        SurveyUser surveyUser = surveyUserMapper.selectOne(userLambdaQueryWrapper);

        if (surveyUser == null) {
            throw new ServiceException(ResultCode.USER_NOT_BIND_UID);
        }

        createTemporaryCertificate(surveyUser);

    }


    private void createTemporaryCertificate(SurveyUser surveyUser) {
        String decrypt = AES.decrypt(surveyUser.getUserName() + surveyUser.getId() + System.currentTimeMillis(),
                ConfigUtil.Secret);
        redisTemplate.opsForValue().set(decrypt, "1", 10, TimeUnit.MINUTES);
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


    private String tokenGenerator(SurveyUser surveyUser) {
        //用户凭证  由用户部分信息+一图流id+时间戳 加密得到
        Map<String, Object> hashMap = new HashMap<>();
        String userName = surveyUser.getUserName();
        Long id = surveyUser.getId();
        hashMap.put("userName", userName.replace(".", "·"));
        hashMap.put("id", surveyUser.getId());
        hashMap.put("ip", surveyUser.getIp());
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


    private UserInfoVO getUserDataVO(SurveyUser surveyUser) {
        UserInfoVO userInfoVO = new UserInfoVO();
//        response.setUid(surveyUser.getId());
        userInfoVO.setUserName(surveyUser.getUserName());
        userInfoVO.setStatus(surveyUser.getStatus());
        userInfoVO.setEmail(surveyUser.getEmail());
        userInfoVO.setAvatar(surveyUser.getAvatar());
        userInfoVO.setAkUid("0");
        userInfoVO.setAkNickName(surveyUser.getUserName());

        QueryWrapper<AkPlayerBindInfoV2> akPlayerBindInfoQueryWrapper = new QueryWrapper<>();
        akPlayerBindInfoQueryWrapper.eq("uid", surveyUser.getId()).orderByDesc("last_active_time");
        List<AkPlayerBindInfoV2> akPlayerBindInfoV2List = akPlayerBindInfoV2Mapper.selectList(akPlayerBindInfoQueryWrapper);


        if (surveyUser.getPassword() != null) {
            userInfoVO.setHasPassword(true);
        }
        if (surveyUser.getEmail() != null) {
            userInfoVO.setHasEmail(true);
        }

        if (akPlayerBindInfoV2List.isEmpty()) {
            return userInfoVO;
        }

        Logger.info("用户绑定了" + akPlayerBindInfoV2List.size() + "条方舟uid");
        for (AkPlayerBindInfoV2 akPlayerBindInfo : akPlayerBindInfoV2List) {

            if (akPlayerBindInfo.getDefaultFlag()) {
                userInfoVO.setAkUid(akPlayerBindInfo.getAkUid());
                userInfoVO.setAkNickName(akPlayerBindInfo.getAkNickName());
            }
        }

        userInfoVO.setAkUid(akPlayerBindInfoV2List.get(0).getAkUid());

        return userInfoVO;
    }
}
