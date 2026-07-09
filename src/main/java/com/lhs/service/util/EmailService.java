package com.lhs.service.util;

import com.lhs.entity.dto.util.EmailFormDTO;

public interface EmailService {

    /**
     * 发送简单邮件
     */
    void sendSimpleEmail(EmailFormDTO email);

    /**
     * 生成一个验证码并存入 Redis
     *
     * @param emailAddress 邮箱地址
     * @param maxCodeNum   最大验证码数量
     * @return 验证码
     */
    Integer createVerificationCode(String emailAddress, Integer maxCodeNum);

    /**
     * 校验验证码
     *
     * @param inputCode 用户输入的验证码
     * @param key       邮箱地址
     */
    void compareVerificationCode(String inputCode, String key);
}
