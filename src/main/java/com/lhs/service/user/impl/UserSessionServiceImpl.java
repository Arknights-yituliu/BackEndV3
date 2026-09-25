package com.lhs.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.context.UserContext;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.user.DirectLoginUserVO;
import com.lhs.entity.po.user.OAuthUserInfo;
import com.lhs.entity.po.user.UserExternalAccountBinding;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.entity.vo.user.LoginSessionVO;
import com.lhs.mapper.user.OAuthUserInfoMapper;
import com.lhs.mapper.user.UserExternalAccountBindingMapper;
import com.lhs.service.user.UcTokenMigrateService;
import com.lhs.service.user.UserSessionService;
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
 * 用户会话服务实现
 * <p>
 * 用户中心迁移后本地不再自建账号，本实现以 UC uid 为准维护本地资料缓存表，
 * 并承载登录态校验、用户信息查询、登出等仍被业务使用的用户逻辑
 */
@Service
public class UserSessionServiceImpl implements UserSessionService {

    private final OAuthUserInfoMapper oauthUserInfoMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final TencentCloudService tencentCloudService;
    private final UserExternalAccountBindingMapper userExternalAccountBindingMapper;
    private final UcTokenMigrateService ucTokenMigrateService;

    public UserSessionServiceImpl(OAuthUserInfoMapper oauthUserInfoMapper,
            RedisTemplate<String, String> redisTemplate,
            TencentCloudService tencentCloudService,
            UserExternalAccountBindingMapper userExternalAccountBindingMapper,
            UcTokenMigrateService ucTokenMigrateService) {
        this.oauthUserInfoMapper = oauthUserInfoMapper;
        this.redisTemplate = redisTemplate;
        this.tencentCloudService = tencentCloudService;
        this.userExternalAccountBindingMapper = userExternalAccountBindingMapper;
        this.ucTokenMigrateService = ucTokenMigrateService;
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
    public UserInfoVO getUserInfoVOByToken(String token) {

        if (!checkParamsValidity(token)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        token = token.replace("Authorization", "");

        OAuthUserInfo userInfo = getUserInfoPOByToken(token);
        // 用户信息 包括凭证，用户状态等
        UserInfoVO userInfoVO = getUserInfoVO(userInfo);
        userInfoVO.setToken(token);
        return userInfoVO;
    }

    /**
     * 组装用户信息 VO（含方舟绑定信息与邮箱状态）
     *
     * @param userInfo 资料缓存中的用户信息
     * @return 用户信息 VO
     */
    private UserInfoVO getUserInfoVO(OAuthUserInfo userInfo) {
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setUid(userInfo.getId());
        userInfoVO.setNickname(userInfo.getNickname());
        userInfoVO.setStatus(userInfo.getStatus());
        userInfoVO.setEmail(userInfo.getEmail());
        userInfoVO.setAvatar(userInfo.getAvatar());
        userInfoVO.setAkUid("0");

        LambdaQueryWrapper<UserExternalAccountBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserExternalAccountBinding::getUid, userInfo.getId())
                .orderByDesc(UserExternalAccountBinding::getUpdateTime);
        List<UserExternalAccountBinding> externalAccountBindings = userExternalAccountBindingMapper
                .selectList(queryWrapper);

        if (userInfo.getEmail() != null && userInfo.getEmail().contains("@")) {
            userInfoVO.setHasEmail(true);
        } else {
            userInfoVO.setEmail("未绑定");
        }

        if (externalAccountBindings.isEmpty()) {
            return userInfoVO;
        }

//        Logger.info("用户绑定了" + externalAccountBindings.size() + "条方舟uid");

        userInfoVO.setAkUid(externalAccountBindings.get(0).getAkUid());

        return userInfoVO;
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
    public OAuthUserInfo getUserInfoPOByToken(String token) {
        if (!checkParamsValidity(token)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        token = token.replace("Authorization", "");

        // 从 Redis 获取 uid（key: loginToken:{token}），未命中视为未登录
        String uidStr = redisTemplate.opsForValue().get(RedisKeyUtil.loginToken(token));
        if (uidStr == null) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
        Long yituliuId = Long.parseLong(uidStr);

        // 从 UC 资料缓存表获取用户信息
        OAuthUserInfo userInfo = oauthUserInfoMapper.selectById(yituliuId);
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
        // 先按自签 token 反查 uid（登出后 loginToken 即被删除，必须先取）
        String uidStr = redisTemplate.opsForValue().get(RedisKeyUtil.loginToken(token));
        // 双撤：先撤销 UC 侧授权（含刷新能力），再删本地会话；缺一即留下仍可用的凭据
        if (uidStr != null) {
            ucTokenMigrateService.revokeByUid(Long.parseLong(uidStr));
        }
        redisTemplate.delete(RedisKeyUtil.loginToken(token));
        Logger.info("用户token已登出撤销");
    }

    @Override
    public void updateNickname(HttpServletRequest httpServletRequest, String nickname) {
        // 校验昵称非空，空值直接拒绝
        if (!checkParamsValidity(nickname)) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }
        // 从线程上下文获取当前登录用户（/auth/** 拦截器已写入）
        OAuthUserInfo userInfo = UserContext.getUserInfo();
        userInfo.setNickname(nickname);
        userInfo.setUpdateTime(new Date());
        oauthUserInfoMapper.updateById(userInfo);
    }

    @Override
    public void updateAvatar(HttpServletRequest httpServletRequest, String avatar) {
        // 校验头像地址非空，空值直接拒绝
        if (!checkParamsValidity(avatar)) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }
        // 从线程上下文获取当前登录用户（/auth/** 拦截器已写入）
        OAuthUserInfo userInfo = UserContext.getUserInfo();
        userInfo.setAvatar(avatar);
        userInfo.setUpdateTime(new Date());
        oauthUserInfoMapper.updateById(userInfo);
    }

    @Override
    public LoginSessionVO createSession(DirectLoginUserVO directLoginUserVO) {
        // UC uid 是本地会话的唯一标识
        Long ucUid = directLoginUserVO.getUid();
        if (ucUid == null) {
            throw new ServiceException(ResultCode.USER_TOKEN_FORMAT_ERROR_OR_USER_NOT_LOGIN);
        }

        Date now = new Date();
        // 查询本地资料缓存（oauth_user_info 表存 UC 资料副本）
        OAuthUserInfo userInfo = oauthUserInfoMapper.selectById(ucUid);

        if (userInfo == null) {
            // 首次通过 UC 登录：插入资料缓存，id 即 UC uid
            userInfo = new OAuthUserInfo();
            userInfo.setId(ucUid);
            userInfo.setNickname(resolveNickname(directLoginUserVO));
            userInfo.setAvatar(resolveAvatar(directLoginUserVO));
            userInfo.setEmail(directLoginUserVO.getEmail());
            userInfo.setStatus(1);
            userInfo.setCreateTime(now);
            userInfo.setUpdateTime(now);
            userInfo.setDeleteFlag(false);
            oauthUserInfoMapper.insert(userInfo);
        } else {
            // 已存在：刷新资料缓存（资料以 UC 为准）
            userInfo.setNickname(resolveNickname(directLoginUserVO));
            userInfo.setAvatar(resolveAvatar(directLoginUserVO));
            if (directLoginUserVO.getEmail() != null) {
                userInfo.setEmail(directLoginUserVO.getEmail());
            }
            userInfo.setUpdateTime(now);
            oauthUserInfoMapper.updateById(userInfo);
        }

        // 生成本地会话 Token（兼作 UC 令牌的刷新凭据，长期保留、不随迁移清理）
        String token = tokenGenerator(userInfo);

        // 接住 UC 一并签发的令牌：refresh_token 落服务端 uid 维度缓存用于刷新，access_token 随本响应回带前端
        ucTokenMigrateService.saveIssuedToken(ucUid, directLoginUserVO.getAccessToken(),
                directLoginUserVO.getRefreshToken(), directLoginUserVO.getExpiresIn(),
                directLoginUserVO.getScope());

        LoginSessionVO session = new LoginSessionVO();
        session.setToken(token);
        session.setUid(ucUid);
        session.setUcAccessToken(directLoginUserVO.getAccessToken());
        session.setUcTokenExpiresIn(directLoginUserVO.getExpiresIn());
        session.setUcTokenScope(directLoginUserVO.getScope());
        return session;
    }

    /**
     * 解析昵称（UC 未返回昵称时兜底为"博士+uid"）
     *
     * @param directLoginUserVO UC 用户信息
     * @return 昵称
     */
    private String resolveNickname(DirectLoginUserVO directLoginUserVO) {
        String nickname = directLoginUserVO.getNickname();
        if (!checkParamsValidity(nickname)) {
            return "博士" + directLoginUserVO.getUid();
        }
        return nickname;
    }

    /**
     * 解析头像（UC 未返回时兜底为默认头像）
     *
     * @param directLoginUserVO UC 用户信息
     * @return 头像
     */
    private String resolveAvatar(DirectLoginUserVO directLoginUserVO) {
        String avatar = directLoginUserVO.getAvatar();
        if (!checkParamsValidity(avatar)) {
            return "char_377_gdglow";
        }
        return avatar;
    }

    @Override
    public void backupUserInfo() {

        List<OAuthUserInfo> userInfoList = oauthUserInfoMapper.selectList(null);
        String dayText = TimeUtil.getDayText();

        tencentCloudService.backupCOS(JsonMapper.toJSONString(userInfoList),
                "/mysql/user/" + dayText + "/oauth_user_info.json");
    }

    /**
     * 生成用户登录凭证并写入 Redis
     *
     * @param userInfo 用户信息
     * @return 登录 token
     */
    private String tokenGenerator(OAuthUserInfo userInfo) {
        // 用户凭证 由用户id+时间戳 加密得到
        Map<String, Object> hashMap = new HashMap<>();
        Long id = userInfo.getId();
        hashMap.put("id", userInfo.getId());
        String header = JsonMapper.toJSONString(hashMap);
        long timeStamp = System.currentTimeMillis();
        String token = AES.encrypt(header + "." + id + "." + timeStamp, ConfigUtil.Secret);

        // 将 token 存入 Redis，支持登出撤销，有效期 90 天
        redisTemplate.opsForValue().set(RedisKeyUtil.loginToken(token), id.toString(), 90, TimeUnit.DAYS);

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
