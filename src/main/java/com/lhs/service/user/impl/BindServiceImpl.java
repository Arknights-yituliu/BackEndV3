package com.lhs.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.user.AkPlayerBindInfoDTO;
import com.lhs.entity.dto.user.EmailRequestDTO;
import com.lhs.entity.dto.util.EmailFormDTO;
import com.lhs.entity.po.user.AkPlayerBindInfo;
import com.lhs.entity.po.user.UserExternalAccountBinding;
import com.lhs.entity.po.user.UserInfo;
import com.lhs.mapper.user.AkPlayerBindInfoMapper;
import com.lhs.mapper.user.UserExternalAccountBindingMapper;
import com.lhs.mapper.user.UserInfoMapper;
import com.lhs.service.user.BindService;
import com.lhs.service.user.OAuthUserService;
import com.lhs.service.util.EmailService;
import com.lhs.service.util.TencentCloudService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class BindServiceImpl implements BindService {

    private final OAuthUserService oAuthUserService;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;
    private final UserInfoMapper userInfoMapper;
    private final UserExternalAccountBindingMapper userExternalAccountBindingMapper;
    private final AkPlayerBindInfoMapper akPlayerBindInfoMapper;
    private final TencentCloudService tencentCloudService;
    private final IdGenerator idGenerator;

    public BindServiceImpl(OAuthUserService oAuthUserService,
            RedisTemplate<String, String> redisTemplate,
            EmailService emailService,
            UserInfoMapper userInfoMapper,
            UserExternalAccountBindingMapper userExternalAccountBindingMapper,
            AkPlayerBindInfoMapper akPlayerBindInfoMapper,
            TencentCloudService tencentCloudService) {
        this.oAuthUserService = oAuthUserService;
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.userInfoMapper = userInfoMapper;
        this.userExternalAccountBindingMapper = userExternalAccountBindingMapper;
        this.akPlayerBindInfoMapper = akPlayerBindInfoMapper;
        this.tencentCloudService = tencentCloudService;
        this.idGenerator = new IdGenerator(1L);
    }

    @Override
    public void sendVerificationCode(HttpServletRequest httpServletRequest, EmailRequestDTO emailRequestDto) {
        String email = emailRequestDto.getEmail();
        String mailUsage = emailRequestDto.getMailUsage();

        // IP 频率限制：同一 IP 30 秒内最多 1 次
        String ip = IpUtil.getIpAddress(httpServletRequest);
        String ipKey = "rate_limit:verification_code:register:ip:" + ip;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(ipKey))) {
            throw new ServiceException(ResultCode.EMAIL_SENT_TOO_FREQUENTLY);
        }
        redisTemplate.opsForValue().set(ipKey, "1", 30, TimeUnit.SECONDS);

        // 邮箱频率限制：同一邮箱 30 秒内最多 1 次
        String emailKey = "rate_limit:verification_code:register:email:" + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(emailKey))) {
            throw new ServiceException(ResultCode.EMAIL_SENT_TOO_FREQUENTLY);
        }
        redisTemplate.opsForValue().set(emailKey, "1", 30, TimeUnit.SECONDS);

        validateEmail(email);

        // 设置查询构造器条件
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getEmail, email);
        // 查询是否有绑定这个邮箱的用户
        UserInfo userInfoByEmail = userInfoMapper.selectOne(queryWrapper);

        if ("register".equals(mailUsage)) {
            if (userInfoByEmail != null) {
                throw new ServiceException(ResultCode.USER_IS_EXIST);
            }
        }

        if ("login".equals(mailUsage)) {
            if (userInfoByEmail == null) {
                throw new ServiceException(ResultCode.USER_NOT_EXIST);
            }
        }

        seedEmail(email);
    }

    @Override
    public void backupUserExternalAccountBinding() {
        String dayText = TimeUtil.getDayText();
        List<UserExternalAccountBinding> list1 = userExternalAccountBindingMapper.selectList(null);
        tencentCloudService.backupCOS(JsonMapper.toJSONString(list1),
                "/mysql/user/" + dayText + "/user_external_account_binding.json");

        List<AkPlayerBindInfo> list2 = akPlayerBindInfoMapper.selectList(null);
        tencentCloudService.backupCOS(JsonMapper.toJSONString(list2),
                "/mysql/user/" + dayText + "/ak_player_bind_info.json");
    }

    @Override
    public void saveExternalAccountBindingInfoAndAKPlayerBindInfo(Long uid,
            AkPlayerBindInfoDTO akPlayerBindInfoDTO) {
        UserExternalAccountBinding userExternalAccountBinding = new UserExternalAccountBinding();
        userExternalAccountBinding.setId(idGenerator.nextId());

        userExternalAccountBinding.setUid(uid);
        userExternalAccountBinding.setAkUid(akPlayerBindInfoDTO.getAkUid());

        saveUserExternalAccountBinding(userExternalAccountBinding);

        AkPlayerBindInfo akPlayerBindInfo = new AkPlayerBindInfo();
        akPlayerBindInfo.copyByAkPlayerBindInfoDTO(akPlayerBindInfoDTO);
        saveAkPlayerBindInfo(akPlayerBindInfo);
    }

    /**
     * 保存或更新用户外部账号绑定信息
     */
    private void saveUserExternalAccountBinding(UserExternalAccountBinding userExternalAccountBinding) {
        LambdaQueryWrapper<UserExternalAccountBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserExternalAccountBinding::getAkUid, userExternalAccountBinding.getAkUid())
                .eq(UserExternalAccountBinding::getUid, userExternalAccountBinding.getUid());
        UserExternalAccountBinding existsData = userExternalAccountBindingMapper.selectOne(queryWrapper);
        long timeStamp = System.currentTimeMillis();

        userExternalAccountBinding.setUpdateTime(timeStamp);

        Logger.info("要添加的外部账号绑定信息 {} " + userExternalAccountBinding);
        if (existsData == null) {
            userExternalAccountBinding.setId(idGenerator.nextId());
            userExternalAccountBinding.setCreateTime(timeStamp);
            userExternalAccountBinding.setDeleteFlag(false);
            userExternalAccountBindingMapper.insert(userExternalAccountBinding);
        } else {
            userExternalAccountBinding.setId(existsData.getId());
            userExternalAccountBinding.setCreateTime(existsData.getCreateTime());
            userExternalAccountBindingMapper.updateById(userExternalAccountBinding);
        }
    }

    /**
     * 保存或更新明日方舟玩家绑定信息
     */
    private void saveAkPlayerBindInfo(AkPlayerBindInfo akPlayerBindInfo) {
        LambdaQueryWrapper<AkPlayerBindInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AkPlayerBindInfo::getAkUid, akPlayerBindInfo.getAkUid());
        AkPlayerBindInfo oldInfo = akPlayerBindInfoMapper.selectOne(queryWrapper);
        akPlayerBindInfo.setUpdateTime(System.currentTimeMillis());
        Logger.info("要添加的明日方舟账号绑定信息，id为" + akPlayerBindInfo);
        if (oldInfo == null) {
            akPlayerBindInfo.setId(idGenerator.nextId());
            akPlayerBindInfo.setDeleteFlag(false);
            akPlayerBindInfoMapper.insert(akPlayerBindInfo);
        } else {
            akPlayerBindInfo.setId(oldInfo.getId());
            akPlayerBindInfoMapper.updateById(akPlayerBindInfo);
        }
    }

    /**
     * 发送邮件验证码
     */
    private void seedEmail(String emailAddress) {
        Integer code = emailService.createVerificationCode(emailAddress, 9999);

        EmailFormDTO emailFormDTO = new EmailFormDTO();
        emailFormDTO.setTo(emailAddress);
        emailFormDTO.setSubject("【一图流】验证码");
        emailFormDTO.setText(String.valueOf(code));
        emailService.sendSimpleEmail(emailFormDTO);
    }

    /**
     * 验证邮箱格式是否正确
     *
     * @param email 邮箱地址
     */
    private static void validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new ServiceException(ResultCode.EMAIL_IS_ERROR);
        }
        // 邮箱格式正则表达式
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        if (!email.matches(emailRegex)) {
            throw new ServiceException(ResultCode.EMAIL_IS_ERROR);
        }
    }

    /**
     * 验证参数是否为空
     *
     * @param param 参数
     * @return 参数是否为空
     */
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
}
