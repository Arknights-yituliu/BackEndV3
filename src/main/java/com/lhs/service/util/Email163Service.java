package com.lhs.service.util;


import com.lhs.entity.dto.util.EmailFormDTO;

public interface Email163Service {


    void sendSimpleEmail(EmailFormDTO email);

    /**
     * 生成一个验证码并发送至邮箱
     * @param emailAddress  邮箱地址
     * @param maxCodeNum 最大验证码数量
     * @return 验证码
     */
    Integer CreateVerificationCode(String emailAddress, Integer maxCodeNum);

    /**
     * 校验验证码
     * @param inputCode 输入的验证码
     * @param emailAddress 邮箱地址
     */
    void compareVerificationCode(String inputCode, String emailAddress);
}
