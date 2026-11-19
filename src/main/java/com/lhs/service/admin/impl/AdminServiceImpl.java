package com.lhs.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.Logger;
import com.lhs.common.util.RedisKeyUtil;
import com.lhs.common.util.RedisRateLimiter;
import com.lhs.common.util.SignUtil;
import com.lhs.common.util.TokenGenerator;
import com.lhs.common.enums.ResultCode;
import com.lhs.entity.dto.util.EmailFormDTO;
import com.lhs.entity.po.admin.Admin;
import com.lhs.entity.vo.dev.LoginVo;
import com.lhs.mapper.admin.AdminMapper;
import com.lhs.service.admin.AdminService;
import com.lhs.service.util.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AdminServiceImpl implements AdminService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final AdminMapper adminMapper;

    private final EmailService emailService;

    /** 验证码生成使用的密码学随机源 */
    private static final SecureRandom CODE_RANDOM = new SecureRandom();

    /** 验证码有效期（秒） */
    private static final long CODE_TTL_SECONDS = 300L;

    /** 验证码允许的最大尝试次数：超过即作废当前验证码 */
    private static final int MAX_LOGIN_FAIL_TIMES = 5;

    /** 登录失败计数窗口（分钟）：窗口过后自动解锁 */
    private static final long LOGIN_FAIL_WINDOW_MINUTES = 10L;

    /** SCAN 每批返回条数的提示值（服务端不保证精确） */
    private static final int CACHE_SCAN_BATCH = 200;

    /** 单次查询返回的 key 上限，防止缓存较多时把大对象一次性拉进内存 */
    private static final int CACHE_MAX_KEYS = 500;

    public AdminServiceImpl(RedisTemplate<String, Object> redisTemplate,
                            AdminMapper adminMapper,
                            EmailService emailService) {
        this.redisTemplate = redisTemplate;
        this.adminMapper = adminMapper;
        this.emailService = emailService;
    }


    @Override
    public void emailSendCode(LoginVo loginVo) {
        String account = loginVo.getDeveloper();
        if (account == null || account.isBlank()) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }

        // 先扣配额再查库：无论账号是否存在都消耗额度，避免用"能否收到邮件"的差异枚举管理员账号
        RedisRateLimiter.tryAcquire(redisTemplate, RedisKeyUtil.adminCodeSendMinRate(account),
                1, 60, ResultCode.EMAIL_SENT_TOO_FREQUENTLY);
        RedisRateLimiter.tryAcquire(redisTemplate, RedisKeyUtil.adminCodeSendHourRate(account),
                5, 5 * 60 * 60, ResultCode.INTERFACE_TOO_MANY_EMAIL_SENT);

        Admin admin = selectAdminByAccount(account);
        if (admin == null) {
            // 静默忽略：不抛 USER_NOT_EXIST，防止通过响应差异判断账号是否存在
            Logger.warn("管理端验证码请求：账号不存在，已静默忽略: account={}", account);
            return;
        }

        String email = admin.getEmail();
        String code = String.format("%06d", CODE_RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(RedisKeyUtil.adminEmailCode(email), code,
                CODE_TTL_SECONDS, TimeUnit.SECONDS);
        // 重新获取验证码时清空历史失败计数，避免旧失败次数影响新一轮校验
        redisTemplate.delete(RedisKeyUtil.adminLoginFail(email));

        EmailFormDTO emailFormDTO = new EmailFormDTO();
        emailFormDTO.setTo(email);
        emailFormDTO.setSubject("开发者登录验证码");
        emailFormDTO.setText(code);
        emailService.sendSimpleEmail(emailFormDTO);
    }


    @Override
    public Map<String, Object> login(LoginVo loginVo) {
        Admin admin = selectAdminByAccount(loginVo.getDeveloper());
        if (admin == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }

        String email = admin.getEmail();
        String codeKey = RedisKeyUtil.adminEmailCode(email);
        String failKey = RedisKeyUtil.adminLoginFail(email);

        // 每次尝试计一次（10 分钟窗口），超过上限即作废当前验证码，强制重新获取
        Long fails = redisTemplate.opsForValue().increment(failKey);
        if (fails != null && fails == 1L) {
            redisTemplate.expire(failKey, LOGIN_FAIL_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        if (fails != null && fails > MAX_LOGIN_FAIL_TIMES) {
            redisTemplate.delete(codeKey);
            throw new ServiceException(ResultCode.VERIFICATION_CODE_LOCKED);
        }

        //检查邮件验证码
        String inputCode = loginVo.getVerificationCode();
        if (inputCode == null || inputCode.isBlank()) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_NOT_ENTER);
        }
        Object cachedCode = redisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null) {
            // 未发送、已过期或已被消费
            throw new ServiceException(ResultCode.VERIFICATION_CODE_NOT_EXIST);
        }
        // 常量时间比较，避免通过响应耗时推断验证码
        if (!MessageDigest.isEqual(inputCode.trim().getBytes(StandardCharsets.UTF_8),
                String.valueOf(cachedCode).getBytes(StandardCharsets.UTF_8))) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_ERROR);
        }

        // 一次性消费：验证码立即失效，避免在有效期内被重放
        redisTemplate.delete(codeKey);
        redisTemplate.delete(failKey);

        //登录时间
        Date date = new Date();
        // 不透明随机 token：库中只保存其 SHA-256 摘要，即使库泄漏也拿不到可用 token
        String token = TokenGenerator.newAdminToken();

        //更新管理员的 token 摘要和 token 过期时间
        UpdateWrapper<Admin> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("token", SignUtil.sha256(token))
                .set("expire", new Date(date.getTime() + 60 * 60 * 24 * 90 * 1000L))
                .eq("id", admin.getId());
        adminMapper.update(null, updateWrapper);
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("developer", admin.getDeveloper());
        return response;
    }

    @Override
    public HashMap<String, Object> getDeveloperInfo(String token) {
        Admin admin = getAdminInfoByToken(token);
        HashMap<String, Object> result = new HashMap<>();
        result.put("userName", admin.getDeveloper());
        // 仅回显调用方自带的 token（保持前端契约），绝不回传服务端保存的摘要
        result.put("token", stripAuthorization(token));
        result.put("expire", admin.getExpire());
        result.put("status", 1);
        return result;

    }

    @Override
    public Map<String, Object> getCacheKeys() {
        // 用 SCAN 游标分批遍历，避免 KEYS 这类 O(N) 阻塞命令卡住整个实例
        ScanOptions options = ScanOptions.scanOptions()
                .match(RedisKeyUtil.PREFIX_ITEM_CACHE + "*")
                .count(CACHE_SCAN_BATCH)
                .build();

        List<String> keys = new ArrayList<>(CACHE_SCAN_BATCH);
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext() && keys.size() < CACHE_MAX_KEYS) {
                keys.add(cursor.next());
            }
        }

        if (keys.isEmpty()) {
            return new HashMap<>();
        }

        // 一次 MGET 取回全部 value，避免 N 次往返；返回条数有上限，防止把大对象全拉进内存
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        Map<String, Object> result = new LinkedHashMap<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            result.put(keys.get(i), values == null || i >= values.size() ? null : values.get(i));
        }
        return result;
    }

    @Override
    public String deleteCacheKey(String key) {
        // 只允许删除缓存前缀下的 key，防止误删会话/频控等关键 key
        if (key == null || key.isBlank()) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }
        if (!key.startsWith(RedisKeyUtil.PREFIX_ITEM_CACHE)) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }
        Boolean deleted = redisTemplate.delete(key);
        return Boolean.TRUE.equals(deleted) ? "删除成功" : "删除失败";
    }

    @Override
    public Boolean developerLevel(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Admin admin = getAdminInfoByToken(token);
        return admin.getLevel() == 0;

    }

    /**
     * 按账号（开发者标识或邮箱）查询管理员
     * <p>
     * developer 与 email 分别有索引，但两列 or 查询可能命中两条不同记录，
     * 这里限定 1 条避免 selectOne 抛 TooManyResultsException。
     *
     * @param account 开发者标识或邮箱
     * @return 管理员实体，账号为空或不存在时返回 null
     */
    private Admin selectAdminByAccount(String account) {
        if (account == null || account.isBlank()) {
            return null;
        }
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getDeveloper, account)
                .or(wrapper -> wrapper.eq(Admin::getEmail, account))
                .last("limit 1");
        return adminMapper.selectOne(queryWrapper);
    }

    /**
     * 根据 token 查询管理员并校验有效期
     * <p>
     * token 为不透明随机串，服务端只保存其 SHA-256 摘要，校验时按摘要反查，
     * 不做任何解密——因此密钥泄漏无法伪造身份，且无法通过构造 id 冒充任意管理员。
     *
     * @param token 请求携带的 token（可含 Authorization 前缀）
     * @return 管理员实体
     */
    private Admin getAdminInfoByToken(String token) {
        if (token == null || token.isBlank() || "null".equals(token)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        token = stripAuthorization(token).trim();

        // 类型隔离：管理端只接受 at_ 前缀，避免用户 token 越界使用
        if (!TokenGenerator.isAdminToken(token)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getToken, SignUtil.sha256(token));
        Admin admin = adminMapper.selectOne(queryWrapper);
        if (admin == null) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        if (admin.getExpire() == null || System.currentTimeMillis() > admin.getExpire().getTime()) {
            throw new ServiceException(ResultCode.LOGIN_EXPIRATION);
        }

        return admin;
    }

    /**
     * 去除 Authorization 前缀（兼容前端把前缀与 token 一起放进 header）
     *
     * @param token 原始 token
     * @return 去掉前缀后的 token
     */
    private static String stripAuthorization(String token) {
        return token.startsWith("Authorization") ? token.substring("Authorization".length()) : token;
    }

}
