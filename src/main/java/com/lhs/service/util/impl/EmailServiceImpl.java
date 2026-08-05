package com.lhs.service.util.impl;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.Logger;
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

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 腾讯云邮件推送 SES 服务实现
 */
@Service
public class EmailServiceImpl implements EmailService {

    private final String secretId;
    private final String secretKey;
    private final String region;
    private final String fromAddress;
    private final RedisTemplate<String, Object> redisTemplate;

    @Resource
    private JavaMailSender javaMailSender;

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

    @Override
    public void sendSimpleEmail(EmailFormDTO email) {
        // 优先使用163邮箱，单日超过300次后切换腾讯云
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String daily163Key = "email:daily163:" + today;
        Object count163Obj = redisTemplate.opsForValue().get(daily163Key);
        int count163 = count163Obj != null ? Integer.parseInt(count163Obj.toString()) : 0;
        sendTencentCloudEmail(email.getTo(), email.getSubject(), email.getText());
//        if (count163 < 300) {
//            send163Email(email);
//            redisTemplate.opsForValue().increment(daily163Key);
//            redisTemplate.expire(daily163Key, 1, TimeUnit.DAYS);
//        } else {
//            sendTencentCloudEmail(email.getTo(), email.getSubject(), email.getText());
//        }
    }

    private void send163Email(EmailFormDTO email) {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom(email.getFrom());
        simpleMailMessage.setTo(email.getTo());
        simpleMailMessage.setSubject(email.getSubject());
        simpleMailMessage.setText(email.getText());
        javaMailSender.send(simpleMailMessage);
    }

    private void sendTencentCloudEmail(String toAddress, String subject, String content) {
        // 单日发送上限检查（每日1000封）
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String dailyKey = "email:daily:" + today;
        Object countObj = redisTemplate.opsForValue().get(dailyKey);
        int dailyCount = countObj != null ? Integer.parseInt(countObj.toString()) : 0;
        if (dailyCount >= 1000) {
            Logger.error("单日邮件发送已达上限1000封");
            throw new ServiceException(ResultCode.INTERFACE_DAILY_SENDING_LIMIT);
        }

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
        req.setDestination(new String[]{toAddress});
        req.setSubject(subject);

        // 构造模板数据，模板变量为 {{code}}
        String templateData = buildTemplateData(content);
        Template template = new Template();
        template.setTemplateID(53553L);
        template.setTemplateData(templateData);

        req.setTemplate(template);

        try {
            SendEmailResponse resp = client.SendEmail(req);
            // 发送成功，递增当日计数器
            redisTemplate.opsForValue().increment(dailyKey);
            redisTemplate.expire(dailyKey, 1, TimeUnit.DAYS);
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
        redisTemplate.opsForValue().set("CODE:CODE." + emailAddress, code, 300, TimeUnit.SECONDS);
        return random;
    }

    @Override
    public void compareVerificationCode(String inputCode, String key) {
        Object code = redisTemplate.opsForValue().get("CODE:CODE." + key);

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
