package com.lhs.service.util.impl;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.Logger;
import com.lhs.common.util.RedisKeyUtil;
import com.lhs.common.enums.ResultCode;
import com.lhs.entity.dto.util.EmailFormDTO;
import com.lhs.service.util.EmailService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ses.v20201002.SesClient;
import com.tencentcloudapi.ses.v20201002.models.SendEmailRequest;
import com.tencentcloudapi.ses.v20201002.models.SendEmailResponse;
import com.tencentcloudapi.ses.v20201002.models.Template;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 邮件发送服务实现（腾讯云 SES 单渠道）
 * <p>
 * 原「163 邮箱 + 腾讯云 SES」多渠道路由已下线：163 SMTP 通道废弃，其配置来源
 * smtp_config 表及对应实体、Mapper 一并移除；当前仅使用腾讯云 SES 模板发送，
 * 并保留每日发送量计数作为腾讯云配额的上限保护
 */
@Service
public class EmailServiceImpl implements EmailService {

    /** 腾讯云 SES 每日发送上限：超过后拒绝发送，避免超配额产生失败调用 */
    private static final int TENCENT_DAILY_LIMIT = 21000;

    private final String secretId;
    private final String secretKey;
    private final String region;
    private final String fromAddress;
    private final RedisTemplate<String, Object> redisTemplate;

    public EmailServiceImpl(
            @Value("${tencent.secretId}") String secretId,
            @Value("${tencent.secretKey}") String secretKey,
            @Value("${tencent.email.region}") String region,
            @Value("${tencent.email.from-address}") String fromAddress,
            RedisTemplate<String, Object> redisTemplate) {
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.region = region;
        this.fromAddress = fromAddress;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 发送简单文本邮件（腾讯云 SES 模板发送）
     * <p>
     * 每日发送量达到上限时直接拒绝，避免超配额调用；计数按自然日归零，
     * 仅在发送成功后递增
     *
     * @param email 邮件内容（收件人、主题、正文/验证码）
     */
    @Override
    public void sendSimpleEmail(EmailFormDTO email) {
        // 每日累计发送量：用于腾讯云配额的日上限保护
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String dailyKey = RedisKeyUtil.emailDaily(today);
        Object countObj = redisTemplate.opsForValue().get(dailyKey);
        int dailyCount = countObj != null ? Integer.parseInt(countObj.toString()) : 0;
        if (dailyCount >= TENCENT_DAILY_LIMIT) {
            Logger.error("邮件发送已达腾讯云每日上限 {} 封，本次拒绝发送", TENCENT_DAILY_LIMIT);
            throw new ServiceException(ResultCode.INTERFACE_DAILY_SENDING_LIMIT);
        }

        sendTencentCloudEmail(email.getTo(), email.getSubject(), email.getText());

        // 发送成功，递增当日累计计数
        redisTemplate.opsForValue().increment(dailyKey);
        redisTemplate.expire(dailyKey, 1, TimeUnit.DAYS);
    }

    private void sendTencentCloudEmail(String toAddress, String subject, String content) {
        // 初始化认证对象
        Credential cred = new Credential(secretId, secretKey);

        // 配置 HTTP 选项
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("ses.tencentcloudapi.com");

        // 配置客户端选项
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        // 实例化 SesClient
        SesClient client = new SesClient(cred, region, clientProfile);

        // 构造请求参数
        SendEmailRequest req = new SendEmailRequest();
        req.setFromEmailAddress(fromAddress);
        req.setDestination(new String[] { toAddress });
        req.setSubject(subject);

        // 构造模板数据，模板变量为 {{code}}
        String templateData = buildTemplateData(content);
        Template template = new Template();
        template.setTemplateID(53553L);
        template.setTemplateData(templateData);

        req.setTemplate(template);

        try {
            SendEmailResponse resp = client.SendEmail(req);
            Logger.info("腾讯云邮件发送成功，MessageId: {}", resp.getMessageId());
        } catch (TencentCloudSDKException e) {
            Logger.error("腾讯云邮件发送失败: {}", e.getMessage());
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
    }

    /**
     * 构建模板数据的 JSON 字符串，模板变量为 {{code}}
     */
    private String buildTemplateData(String code) {
        return "{\"code\":\"" + code.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    @Override
    public Integer createVerificationCode(String emailAddress, Integer maxCodeNum) {
        int random = new Random().nextInt(8999) + 1000;
        String code = String.valueOf(random);
        redisTemplate.opsForValue().set(RedisKeyUtil.emailCode(emailAddress), code, 300, TimeUnit.SECONDS);
        return random;
    }

    @Override
    public void compareVerificationCode(String inputCode, String key) {
        Object code = redisTemplate.opsForValue().get(RedisKeyUtil.emailCode(key));

        if (code == null) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_NOT_EXIST);
        }

        Logger.info("输入的验证码：" + inputCode + "---------服务端验证码：" + code);

        if (inputCode == null) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_NOT_ENTER);
        }

        if (!inputCode.equals(code)) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_ERROR);
        }
    }
}
