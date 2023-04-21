package com.lhs.service;

import com.lhs.service.request.EmailRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class UserService {

//    @Resource
//    JavaMailSender javaMailSender;
//
//    public void sendMail(EmailRequest emailRequest){
//        SimpleMailMessage smm = new SimpleMailMessage();
//        smm.setFrom(emailRequest.getFrom());//发送者
//        smm.setTo(emailRequest.getTo());//收件人
//        smm.setSubject(emailRequest.getSubject());//邮件主题
//        smm.setText(emailRequest.getText());//邮件内容
//        javaMailSender.send(smm);//发送邮件
//    }


}
