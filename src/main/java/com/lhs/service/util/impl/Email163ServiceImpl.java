package com.lhs.service.util.impl;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.LogUtils;
import com.lhs.common.enums.ResultCode;
import com.lhs.entity.dto.util.EmailFormDTO;
import com.lhs.service.util.Email163Service;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class Email163ServiceImpl implements Email163Service {

    @Resource
    private JavaMailSender javaMailSender;

    private final RedisTemplate<String, Object> redisTemplate;

    public Email163ServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void sendSimpleEmail(EmailFormDTO email) {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom(email.getFrom());
        simpleMailMessage.setTo(email.getTo());
        simpleMailMessage.setSubject(email.getSubject());
        simpleMailMessage.setText(email.getText());
        javaMailSender.send(simpleMailMessage);
    }

    @Override
    public Integer CreateVerificationCode(String emailAddress, Integer maxCodeNum) {
        int random = new Random().nextInt(8999) + 1000;
        String code = String.valueOf(random);
        redisTemplate.opsForValue().set("CODE:CODE." + emailAddress, code, 300, TimeUnit.SECONDS);
        return random;
    }

    @Override
    public void compareVerificationCode(String inputCode, String emailAddress) {
        Object code = redisTemplate.opsForValue().get("CODE:CODE." + emailAddress);

        if (code == null) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_NOT_EXIST);
        }

        LogUtils.info("输入的验证码：" + inputCode + "---------服务端验证码：" + code);

        if (inputCode == null) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_NOT_ENTER);
        }

        if (!inputCode.equals(code)) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_ERROR);
        }

    }

}
