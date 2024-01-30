package com.lhs.service.dev.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.LogUtil;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.dto.util.EmailFormDTO;
import com.lhs.entity.po.dev.Developer;
import com.lhs.entity.vo.dev.LoginVo;
import com.lhs.mapper.dev.DeveloperMapper;
import com.lhs.mapper.dev.PageVisitsMapper;
import com.lhs.mapper.dev.VisitsMapper;
import com.lhs.service.dev.UserService;
import com.lhs.service.util.Email163Service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {


    private final RedisTemplate<String, Object> redisTemplate;

    private final DeveloperMapper developerMapper;

    private final VisitsMapper visitsMapper;

    private final PageVisitsMapper pageVisitsMapper;

    private final Email163Service email163Service;

    public UserServiceImpl(RedisTemplate<String, Object> redisTemplate, DeveloperMapper developerMapper, VisitsMapper visitsMapper, PageVisitsMapper pageVisitsMapper, Email163Service email163Service) {
        this.redisTemplate = redisTemplate;
        this.developerMapper = developerMapper;
        this.visitsMapper = visitsMapper;
        this.pageVisitsMapper = pageVisitsMapper;
        this.email163Service = email163Service;
    }

    @Override
    public Boolean developerLevel(HttpServletRequest request) {
        String token = request.getHeader("token");
        String developerBase64 = token.split("\\.")[0];
        String decode = new String(Base64.getDecoder().decode(developerBase64), StandardCharsets.UTF_8);
        QueryWrapper<Developer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("developer", decode);
        Developer developer = developerMapper.selectOne(queryWrapper);
        return developer.getLevel() == 0;
    }

    @Override
    public void emailSendCode(LoginVo loginVo) {
        QueryWrapper<Developer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("developer", loginVo.getDeveloper());
        Developer developer = developerMapper.selectOne(queryWrapper);
        if (developer == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        String email = developer.getEmail();
        int random = new Random().nextInt(999999);
        String code = String.format("%6s", random).replace(" ", "0");
        redisTemplate.opsForValue().set("CODE:" + developer.getEmail() + "CODE", code, 300, TimeUnit.SECONDS);
        String text = "本次登录验证码：" + code;
        String subject = "开发者登录";


        EmailFormDTO emailFormDTO = new EmailFormDTO();
        emailFormDTO.setFrom("ark_yituliu@163.com");
        emailFormDTO.setTo(email);
        emailFormDTO.setSubject(subject);
        emailFormDTO.setText(text);
        email163Service.sendSimpleEmail(emailFormDTO);
    }


    @Override
    public String login(LoginVo loginVo) {
        QueryWrapper<Developer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("developer", loginVo.getDeveloper());
        Developer developer = developerMapper.selectOne(queryWrapper);
        if (developer == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        String code = String.valueOf(redisTemplate.opsForValue().get("CODE:" + developer.getEmail() + "CODE"));
        //检查邮件验证码
        if (!loginVo.getVerificationCode().equals(code)) {
            throw new ServiceException(ResultCode.CODE_ERROR);
        }

        //登录时间
        String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("developer", loginVo.getDeveloper());
        hashMap.put("loginDate", format);
        String headerText = JsonMapper.toJSONString(hashMap);  //token头是登录名+登录时间的map
//        类似jwt
//        String HeaderBase64 =  Base64.getEncoder().encodeToString((headerStr).getBytes());
//        String sign = AES.encrypt(headerStr+ConfigUtil.SignKey, ConfigUtil.Secret);
//        String token =  HeaderBase64+"."+sign;
        String sign = AES.encrypt(headerText + ApplicationConfig.SignKey, ApplicationConfig.Secret);  //组成签名：token头+签名key
        String developerBase64 = Base64.getEncoder().encodeToString(loginVo.getDeveloper().getBytes());  //进行base64转换

        String token = developerBase64 + "." + sign;  //完整token：token头.token尾
        redisTemplate.opsForValue().set("TOKEN:" + developerBase64, token, 45, TimeUnit.DAYS);
        return token;
    }

    @Override
    public Boolean loginAndCheckToken(String token) {


        String developerBase64 = token.split("\\.")[0];

        if (redisTemplate.opsForValue().get("TOKEN:" + developerBase64) == null) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }


        String decode = new String(Base64.getDecoder().decode(developerBase64), StandardCharsets.UTF_8);

        String redisToken = String.valueOf(redisTemplate.opsForValue().get("TOKEN:" + developerBase64));
        if (!token.equals(redisToken)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        redisTemplate.opsForValue().set("TOKEN:" + developerBase64, redisToken, 30, TimeUnit.DAYS);

        LogUtil.info("开发者验证通过");
        return true;
    }



}
