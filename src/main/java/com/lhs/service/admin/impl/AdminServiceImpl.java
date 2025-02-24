package com.lhs.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.enums.ResultCode;
import com.lhs.entity.dto.util.EmailFormDTO;
import com.lhs.entity.po.admin.Admin;
import com.lhs.entity.vo.dev.LoginVo;
import com.lhs.mapper.admin.AdminMapper;
import com.lhs.mapper.admin.PageVisitsMapper;
import com.lhs.mapper.admin.VisitsMapper;
import com.lhs.service.admin.AdminService;
import com.lhs.service.user.UserService;
import com.lhs.service.util.Email163Service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AdminServiceImpl implements AdminService {


    private final RedisTemplate<String, Object> redisTemplate;

    private final AdminMapper adminMapper;

    private final VisitsMapper visitsMapper;

    private final PageVisitsMapper pageVisitsMapper;

    private final UserService userService;

    private final Email163Service email163Service;

    public AdminServiceImpl(RedisTemplate<String, Object> redisTemplate,
                            AdminMapper adminMapper,
                            VisitsMapper visitsMapper,
                            PageVisitsMapper pageVisitsMapper,
                            UserService userService,
                            Email163Service email163Service) {
        this.redisTemplate = redisTemplate;
        this.adminMapper = adminMapper;
        this.visitsMapper = visitsMapper;
        this.pageVisitsMapper = pageVisitsMapper;
        this.userService = userService;
        this.email163Service = email163Service;
    }


    @Override
    public void emailSendCode(LoginVo loginVo) {
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getDeveloper, loginVo.getDeveloper()).or(wrapper -> wrapper.eq(Admin::getEmail, loginVo.getDeveloper()));
        Admin admin = adminMapper.selectOne(queryWrapper);
        if (admin == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        String email = admin.getEmail();
        int random = new Random().nextInt(999999);
        String code = String.format("%6s", random).replace(" ", "0");
        redisTemplate.opsForValue().set("CODE:" + admin.getEmail() + "CODE", code, 300, TimeUnit.SECONDS);
        String text = "本次登录验证码：" + code;
        String subject = "开发者登录—本次登录验证码：" + code;
        EmailFormDTO emailFormDTO = new EmailFormDTO();
        emailFormDTO.setFrom("ark_yituliu@163.com");
        emailFormDTO.setTo(email);
        emailFormDTO.setSubject(subject);
        emailFormDTO.setText(text);
        email163Service.sendSimpleEmail(emailFormDTO);
    }


    @Override
    public Map<String, Object> login(LoginVo loginVo) {
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getDeveloper, loginVo.getDeveloper()).or(wrapper -> wrapper.eq(Admin::getEmail, loginVo.getDeveloper()));
        Admin admin = adminMapper.selectOne(queryWrapper);
        if (admin == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }
        String code = String.valueOf(redisTemplate.opsForValue().get("CODE:" + admin.getEmail() + "CODE"));
        //检查邮件验证码
        if (!loginVo.getVerificationCode().equals(code)) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_ERROR);
        }

        //登录时间
        Date date = new Date();
        String token = tokenGenerator(admin);

        //更新用户的token和token过期时间
        UpdateWrapper<Admin> updateWrapper = new UpdateWrapper<>();
        System.out.println(token);
        //将生成的token和过期时间存入
        updateWrapper.set("token", token)
                .set("expire", new Date(date.getTime() + 60 * 60 * 24 * 90 * 1000L))
                .eq("id", admin.getId());
        adminMapper.update(null, updateWrapper);
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("developer", admin.getDeveloper());
        return response;
    }

    private String tokenGenerator(Admin admin) {
        //用户凭证  由用户部分信息+一图流id+时间戳 加密得到
        HashMap<Object, Object> map = new HashMap<>();
        map.put("developer", admin.getDeveloper());
        map.put("id", admin.getId());
        Long id = admin.getId();
        String header = JsonMapper.toJSONString(map);
        long timeStamp = System.currentTimeMillis();
        return AES.encrypt(header + "." + id + "." + timeStamp, ConfigUtil.Secret);
    }


    @Override
    public HashMap<String, Object> getDeveloperInfo(String token) {
        Admin admin = getAdminInfoByToken(token);
        HashMap<String, Object> result = new HashMap<>();
        result.put("userName", admin.getDeveloper());
        result.put("token", admin.getToken());
        result.put("expire", admin.getExpire());
        result.put("status",1);
        return result;

    }

    @Override
    public Set<String> getCacheKeys() {

        return redisTemplate.keys("item:*");
    }

    @Override
    public String deleteCacheKey(String key) {
        Boolean delete = redisTemplate.delete(key);
        return delete?"删除成功":"删除失败";
    }

    @Override
    public Boolean developerLevel(HttpServletRequest request) {
        String token = request.getHeader("token");
        Admin admin = getAdminInfoByToken(token);
        return admin.getLevel() == 0;

    }

    private Admin getAdminInfoByToken(String token) {
        if (token == null || "null".equals(token)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        token = token.replace("Authorization", "");

        Long id = decryptToken(token);

        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getId, id);
        Admin admin = adminMapper.selectOne(queryWrapper);
        if (admin == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }

        if (System.currentTimeMillis() > admin.getExpire().getTime()) {
            throw new ServiceException(ResultCode.LOGIN_EXPIRATION);
        }

        return admin;
    }


    /**
     * 解密用户凭证
     *
     * @param token 用户凭证
     * @return 一图流id
     */
    private Long decryptToken(String token) {
        long id;

        try {
            String decrypt = AES.decrypt(token.replaceAll(" ", "+"), ConfigUtil.Secret);
            String idText = decrypt.split("\\.")[1];
            id = Long.parseLong(idText);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(ResultCode.USER_TOKEN_FORMAT_ERROR_OR_USER_NOT_LOGIN);
        }
        return id;
    }

}
