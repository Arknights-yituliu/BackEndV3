package com.lhs.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.dto.util.EmailFormDTO;
import com.lhs.entity.po.admin.Admin;
import com.lhs.entity.vo.dev.LoginVo;
import com.lhs.mapper.admin.AdminMapper;
import com.lhs.mapper.admin.PageVisitsMapper;
import com.lhs.mapper.admin.VisitsMapper;
import com.lhs.service.admin.AdminService;
import com.lhs.service.util.Email163Service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AdminServiceImpl implements AdminService {


    private final RedisTemplate<String, Object> redisTemplate;

    private final AdminMapper adminMapper;

    private final VisitsMapper visitsMapper;

    private final PageVisitsMapper pageVisitsMapper;

    private final Email163Service email163Service;

    public AdminServiceImpl(RedisTemplate<String, Object> redisTemplate, AdminMapper adminMapper, VisitsMapper visitsMapper, PageVisitsMapper pageVisitsMapper, Email163Service email163Service) {
        this.redisTemplate = redisTemplate;
        this.adminMapper = adminMapper;
        this.visitsMapper = visitsMapper;
        this.pageVisitsMapper = pageVisitsMapper;
        this.email163Service = email163Service;
    }


    @Override
    public void emailSendCode(LoginVo loginVo) {
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("developer", loginVo.getDeveloper());
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
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("developer", loginVo.getDeveloper());
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
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("developer", admin.getDeveloper());
        hashMap.put("loginDate", date.getTime());
        hashMap.put("id", admin.getId());
        //header是登录名+登录时间的map字符串
        String header = JsonMapper.toJSONString(hashMap);
        //签名：header+签名key
        String sign = AES.encrypt(header + ConfigUtil.SignKey, ConfigUtil.Secret);
        //进行base64转换
        String headerBase64 = Base64.getEncoder().encodeToString(header.getBytes());
        //完整token：头信息.签名
        String token = headerBase64 + "." + sign;
        //更新用户的token和token过期时间
        UpdateWrapper<Admin> updateWrapper = new UpdateWrapper<>();
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

    @Override
    public Boolean checkToken(String token) {

        Admin admin = getAdminInfoByToken(token);

        return true;
    }

    @Override
    public HashMap<String, Object> getDeveloperInfo(String token) {

        Admin admin = getAdminInfoByToken(token);

        HashMap<String, Object> result = new HashMap<>();
        result.put("userName", admin.getDeveloper());
        result.put("token", admin.getToken());
        result.put("expire", admin.getExpire());

        return result;

    }

    @Override
    public Boolean developerLevel(HttpServletRequest request) {
        String token = request.getHeader("token");
        Admin admin = getAdminInfoByToken(token);
        return admin.getLevel() == 0;

    }

    private Admin getAdminInfoByToken(String token) {
        if (token == null||"null".equals(token)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
        String headerBase64 = token.split("\\.")[0];
        String headerText = new String(Base64.getDecoder().decode(headerBase64), StandardCharsets.UTF_8);
        JsonNode header = JsonMapper.parseJSONObject(headerText);
        long id = header.get("id").asLong();
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        Admin admin = adminMapper.selectOne(queryWrapper);
        if (admin == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }

        if (System.currentTimeMillis() > admin.getExpire().getTime()) {
            throw new ServiceException(ResultCode.LOGIN_EXPIRATION);
        }

        return admin;
    }

}
