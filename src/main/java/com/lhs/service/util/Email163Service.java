package com.lhs.service.util;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.Log;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.dto.util.EmailFormDTO;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class Email163Service {

    @Resource
    private JavaMailSender javaMailSender;

    private final RedisTemplate<String,Object> redisTemplate;

    public Email163Service(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void sendSimpleEmail(EmailFormDTO email){
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom(email.getFrom());
        simpleMailMessage.setTo(email.getTo());
        simpleMailMessage.setSubject(email.getSubject());
        simpleMailMessage.setText(email.getText());
        javaMailSender.send(simpleMailMessage);
    }


    public Integer CreateVerificationCode(String emailAddress,Integer maxCodeNum){
        int random = new Random().nextInt(8999) + 1000;
        String code = String.valueOf(random);
        redisTemplate.opsForValue().set("CODE:CODE." + emailAddress, code, 300, TimeUnit.SECONDS);
        return random;
    }

    public Boolean compareVerificationCode(String inputCode,String emailAddress){
        Object code = redisTemplate.opsForValue().get(emailAddress);
        Log.info("输入的验证码："+inputCode+"---------服务端验证码："+code);
        if(inputCode==null) throw new ServiceException(ResultCode.CODE_ERROR);

        if(code==null)throw new ServiceException(ResultCode.CODE_NOT_EXIST);

        if(!inputCode.equals(code)) throw new ServiceException(ResultCode.CODE_ERROR);

        return true;
    }

}
