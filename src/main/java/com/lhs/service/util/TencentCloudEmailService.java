package com.lhs.service.util;

import com.lhs.entity.dto.util.EmailFormDTO;

public interface TencentCloudEmailService {

    /**
     * 发送简单邮件（使用腾讯云邮件推送 SES）
     *
     * @param email 邮件表单（from/to/subject/text）
     */
    void sendSimpleEmail(EmailFormDTO email);

    /**
     * 发送邮件（直接指定收件人、主题、内容）
     *
     * @param toAddress 收件人邮箱地址
     * @param subject   邮件主题
     * @param content   邮件正文（支持 HTML）
     */
    void sendEmail(String toAddress, String subject, String content);

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
