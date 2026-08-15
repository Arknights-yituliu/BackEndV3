package com.lhs.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.user.OAuth2UserInfo;
import com.lhs.entity.po.user.TokenRecord;
import com.lhs.entity.po.user.UserExternalAccountBinding;
import com.lhs.entity.po.user.UserInfo;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.user.TokenRecordMapper;
import com.lhs.mapper.user.UserExternalAccountBindingMapper;
import com.lhs.mapper.user.UserInfoMapper;
import com.lhs.service.user.OAuthUserService;
import com.lhs.service.util.TencentCloudService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OAuth2 用户中心接入后的用户服务实现
 * <p>
 * 用户中心迁移后本地不再自建账号，本实现以 UC uid 为准维护本地资料缓存表，
 * 并承载登录态校验、用户信息查询、登出等仍被业务使用的用户逻辑
 */
@Service
public class OAuthUserServiceImpl implements OAuthUserService {

    private final UserInfoMapper userInfoMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final TencentCloudService tencentCloudService;
    private final UserExternalAccountBindingMapper userExternalAccountBindingMapper;
    private final TokenRecordMapper tokenRecordMapper;
    private final IdGenerator idGenerator;

    public OAuthUserServiceImpl(UserInfoMapper userInfoMapper,
            RedisTemplate<String, String> redisTemplate,
            TencentCloudService tencentCloudService,
            UserExternalAccountBindingMapper userExternalAccountBindingMapper,
            TokenRecordMapper tokenRecordMapper) {
        this.userInfoMapper = userInfoMapper;
        this.redisTemplate = redisTemplate;
        this.tencentCloudService = tencentCloudService;
        this.userExternalAccountBindingMapper = userExternalAccountBindingMapper;
        this.tokenRecordMapper = tokenRecordMapper;
        this.idGenerator = new IdGenerator(1L);
    }

    @Override
    public String extractToken(HttpServletRequest httpServletRequest) {
        String token = httpServletRequest.getHeader("Authorization");

        if (token != null && token.startsWith("Authorization") && token.length() > 30) {
            return token.replace("Authorization", "");
        }

        throw new ServiceException(ResultCode.USER_NOT_LOGIN);
    }

    @Override
    public Boolean checkUserLoginStatus(HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("Authorization");

        return header != null && header.startsWith("Authorization") && header.length() > 30;
    }

    @Override
    public UserInfoVO getUserInfoVOByToken(String token) {

        if (!checkParamsValidity(token)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        token = token.replace("Authorization", "");

        UserInfo userInfo = getUserInfoPOByToken(token);
        // 用户信息 包括凭证，用户名，用户状态等
        UserInfoVO userInfoVO = getUserInfoVO(userInfo);
        userInfoVO.setToken(token);
        return userInfoVO;
    }

    /**
     * 组装用户信息 VO（含方舟绑定信息与邮箱/密码状态）
     *
     * @param userInfo 资料缓存中的用户信息
     * @return 用户信息 VO
     */
    private UserInfoVO getUserInfoVO(UserInfo userInfo) {
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setUid(userInfo.getId());
        userInfoVO.setUserName(userInfo.getUserName());
        userInfoVO.setStatus(userInfo.getStatus());
        userInfoVO.setEmail(userInfo.getEmail());
        userInfoVO.setAvatar(userInfo.getAvatar());
        userInfoVO.setAkUid("0");
        userInfoVO.setAkNickName(userInfo.getUserName());

        LambdaQueryWrapper<UserExternalAccountBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserExternalAccountBinding::getUid, userInfo.getId())
                .orderByDesc(UserExternalAccountBinding::getUpdateTime);
        List<UserExternalAccountBinding> externalAccountBindings = userExternalAccountBindingMapper
                .selectList(queryWrapper);

        if (userInfo.getPassword() != null && userInfo.getPassword().length() > 10) {
            userInfoVO.setHasPassword(true);
        }

        if (userInfo.getEmail() != null && userInfo.getEmail().contains("@")) {
            userInfoVO.setHasEmail(true);
        } else {
            userInfoVO.setEmail("未绑定");
        }

        if (externalAccountBindings.isEmpty()) {
            return userInfoVO;
        }

        Logger.info("用户绑定了" + externalAccountBindings.size() + "条方舟uid");

        userInfoVO.setAkUid(externalAccountBindings.get(0).getAkUid());

        return userInfoVO;
    }

    @Override
    public UserInfoVO getUserInfoVOByHttpServletRequest(HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        return getUserInfoVOByToken(token);
    }

    @Override
    public Long getUidByHttpServletRequest(HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("Authorization");

        if (header != null && header.startsWith("Authorization") && header.length() > 30) {
            UserInfoVO userInfoVO = getUserInfoVOByHttpServletRequest(httpServletRequest);
            return userInfoVO.getUid();
        }

        String uidByHeader = httpServletRequest.getHeader("uid");
        if (isNumericAndLengthy(uidByHeader)) {
            return Long.parseLong(uidByHeader);
        }

        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);

        Object value = redisTemplate.opsForHash().get("Commit_Ip", ipAddress);
        if (value == null) {
            Long id = idGenerator.nextId();
            redisTemplate.opsForHash().put("Commit_Ip", ipAddress, id);
            return id;
        }

        return Long.parseLong(value.toString());
    }

    /**
     * 判断字符串是否为超过 8 位的纯数字（用于区分临时 uid）
     *
     * @param str 待判断字符串
     * @return 是否为符合要求的数字
     */
    public static boolean isNumericAndLengthy(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        if (str.length() <= 8) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public UserInfo getUserInfoPOByHttpServletRequest(HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        return getUserInfoPOByToken(token);
    }

    @Override
    public UserInfo getUserInfoPOByToken(String token) {
        if (!checkParamsValidity(token)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        token = token.replace("Authorization", "");

        // 优先从 Redis 新格式获取 uid（key: loginToken:{token}）
        String uidStr = redisTemplate.opsForValue().get("loginToken:" + token);
        Long yituliuId;

        if (uidStr != null) {
            // 新格式命中，直接获取 uid
            yituliuId = Long.parseLong(uidStr);
        } else {
            // 旧 token 未写入 Redis，解密获取 uid，同时写入新格式完成迁移
            Logger.info("token走旧格式解密路径，已同步写入Redis");
            yituliuId = decryptToken(token);
            redisTemplate.opsForValue().set("loginToken:" + token, yituliuId.toString());
            // 同时补写数据库记录
            TokenRecord record = new TokenRecord();
            record.setId(idGenerator.nextId());
            record.setUid(yituliuId);
            record.setToken(token);
            record.setType("login");
            record.setRemark("旧token迁移");
            record.setCreateTime(new Date());
            tokenRecordMapper.insert(record);
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", yituliuId);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper); // 查询用户

        if (userInfo == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }
        if (userInfo.getStatus() < 0) {
            throw new ServiceException(ResultCode.USER_FORBIDDEN);
        }

        return userInfo;
    }

    @Override
    public void logout(HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        redisTemplate.delete("loginToken:" + token);
        // 删除数据库中的 token 记录
        tokenRecordMapper.delete(new LambdaQueryWrapper<TokenRecord>().eq(TokenRecord::getToken, token));
        Logger.info("用户token已登出撤销");
    }

    @Override
    public HashMap<String, Object> createSessionByOAuth2Uid(OAuth2UserInfo oAuth2UserInfo) {
        // UC uid 是本地会话的唯一标识
        Long ucUid = oAuth2UserInfo.getUid();
        if (ucUid == null) {
            throw new ServiceException(ResultCode.USER_TOKEN_FORMAT_ERROR_OR_USER_NOT_LOGIN);
        }

        Date now = new Date();
        // 查询本地资料缓存（UserInfo 表转型后仅存 UC 资料副本）
        UserInfo userInfo = userInfoMapper.selectById(ucUid);

        if (userInfo == null) {
            // 首次通过 UC 登录：插入资料缓存，id 即 UC uid
            userInfo = new UserInfo();
            userInfo.setId(ucUid);
            userInfo.setUserName(resolveOAuth2UserName(oAuth2UserInfo));
            userInfo.setAvatar(resolveOAuth2Avatar(oAuth2UserInfo));
            userInfo.setEmail(oAuth2UserInfo.getEmail());
            userInfo.setStatus(1);
            userInfo.setCreateTime(now);
            userInfo.setUpdateTime(now);
            userInfo.setDeleteFlag(false);
            userInfoMapper.insert(userInfo);
        } else {
            // 已存在：刷新资料缓存（资料以 UC 为准）
            userInfo.setUserName(resolveOAuth2UserName(oAuth2UserInfo));
            userInfo.setAvatar(resolveOAuth2Avatar(oAuth2UserInfo));
            if (oAuth2UserInfo.getEmail() != null) {
                userInfo.setEmail(oAuth2UserInfo.getEmail());
            }
            userInfo.setUpdateTime(now);
            userInfoMapper.updateById(userInfo);
        }

        // 生成本地会话 Token
        String token = tokenGenerator(userInfo);
        HashMap<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("uid", ucUid);
        return result;
    }

    /**
     * 解析用户名（UC 未返回时兜底为"博士+uid"）
     *
     * @param oAuth2UserInfo UC 用户信息
     * @return 用户名
     */
    private String resolveOAuth2UserName(OAuth2UserInfo oAuth2UserInfo) {
        String userName = oAuth2UserInfo.getUserName();
        if (!checkParamsValidity(userName)) {
            return "博士" + oAuth2UserInfo.getUid();
        }
        return userName;
    }

    /**
     * 解析头像（UC 未返回时兜底为默认头像）
     *
     * @param oAuth2UserInfo UC 用户信息
     * @return 头像
     */
    private String resolveOAuth2Avatar(OAuth2UserInfo oAuth2UserInfo) {
        String avatar = oAuth2UserInfo.getAvatar();
        if (!checkParamsValidity(avatar)) {
            return "char_377_gdglow";
        }
        return avatar;
    }

    @Override
    public void backupUserInfo() {

        List<UserInfo> userInfoList = userInfoMapper.selectList(null);
        String dayText = TimeUtil.getDayText();

        tencentCloudService.backupCOS(JsonMapper.toJSONString(userInfoList),
                "/mysql/user/" + dayText + "/user_info.json");
    }

    /**
     * 解密用户凭证
     *
     * @param token 用户凭证
     * @return 一图流 id
     */
    private Long decryptToken(String token) {
        try {
            String decrypt = AES.decrypt(token.replaceAll(" ", "+"), ConfigUtil.Secret);
            String idText = decrypt.split("\\.")[1];
            return Long.parseLong(idText);
        } catch (Exception e) {
            Logger.error("Token解密失败", e);
            throw new ServiceException(ResultCode.USER_TOKEN_FORMAT_ERROR_OR_USER_NOT_LOGIN);
        }
    }

    /**
     * 生成用户登录凭证并写入 Redis 与数据库记录
     *
     * @param userInfo 用户信息
     * @return 登录 token
     */
    private String tokenGenerator(UserInfo userInfo) {
        // 用户凭证 由用户部分信息+一图流id+时间戳 加密得到
        Map<String, Object> hashMap = new HashMap<>();
        String userName = userInfo.getUserName();
        Long id = userInfo.getId();
        hashMap.put("userName", userName.replace(".", "·"));
        hashMap.put("id", userInfo.getId());
        String header = JsonMapper.toJSONString(hashMap);
        long timeStamp = System.currentTimeMillis();
        String token = AES.encrypt(header + "." + id + "." + timeStamp, ConfigUtil.Secret);

        // 将 token 存入 Redis，支持登出撤销，不设置过期时间，token 永不过期
        redisTemplate.opsForValue().set("loginToken:" + token, id.toString());

        // 将 token 写入数据库记录
        TokenRecord record = new TokenRecord();
        record.setId(idGenerator.nextId());
        record.setUid(id);
        record.setToken(token);
        record.setType("login");
        record.setCreateTime(new Date());
        tokenRecordMapper.insert(record);

        return token;
    }

    /**
     * 验证参数是否为空，返回一个 Boolean 状态
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
