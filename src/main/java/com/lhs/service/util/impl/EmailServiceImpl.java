package com.lhs.service.util.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.Logger;
import com.lhs.common.util.RedisKeyUtil;
import com.lhs.common.enums.ResultCode;
import com.lhs.entity.dto.util.EmailFormDTO;
import com.lhs.entity.po.util.SmtpConfig;
import com.lhs.mapper.util.SmtpConfigMapper;
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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 多渠道邮件发送服务实现
 * 渠道路由策略（基于每日累计发送量）：
 * 1. 每日 300 封以内：第一个 163 邮箱（mail-163-1）发送
 * 2. 超过 300 封且 600 封以内：第二个 163 邮箱（mail-163-2）发送
 * 3. 超过 600 封且 20000 封以内：腾讯云 SES 发送
 * 4. 超过 20000 封：拒绝发送（腾讯云上限，抛出异常）
 */
@Service
public class EmailServiceImpl implements EmailService {

    /** 第一个 163 邮箱（mail-163-1）每日发送额度：300 封 */
    private static final int MAIL_163_1_DAILY_LIMIT = 300;

    /** 第二个 163 邮箱（mail-163-2）每日发送额度：300 封，累计 600 封后切腾讯云 */
    private static final int MAIL_163_2_DAILY_LIMIT = 600;

    /** 腾讯云 SES 每日发送上限：20000 封，超过后拒绝发送 */
    private static final int TENCENT_DAILY_LIMIT = 21000;

    private final String secretId;
    private final String secretKey;
    private final String region;
    private final String fromAddress;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SmtpConfigMapper smtpConfigMapper;

    /** 渠道标识 -> 邮件发送器 缓存 */
    private final Map<String, JavaMailSenderImpl> senderCache = new ConcurrentHashMap<>();

    public EmailServiceImpl(
            @Value("${tencent.secretId}") String secretId,
            @Value("${tencent.secretKey}") String secretKey,
            @Value("${tencent.email.region}") String region,
            @Value("${tencent.email.from-address}") String fromAddress,
            RedisTemplate<String, Object> redisTemplate,
            SmtpConfigMapper smtpConfigMapper) {
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.region = region;
        this.fromAddress = fromAddress;
        this.redisTemplate = redisTemplate;
        this.smtpConfigMapper = smtpConfigMapper;
    }

    @Override
    public void sendSimpleEmail(EmailFormDTO email) {
        // 每日累计发送量作为渠道降级路由依据
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String dailyKey = RedisKeyUtil.emailDaily(today);
        Object countObj = redisTemplate.opsForValue().get(dailyKey);
        int dailyCount = countObj != null ? Integer.parseInt(countObj.toString()) : 0;
        dailyCount = 1000;
        if (dailyCount < MAIL_163_1_DAILY_LIMIT) {
            // 每日 300 封以内：第一个 163 邮箱发送
            Logger.info("邮件渠道路由：今日已发送 {} 封，使用 mail-163-1", dailyCount);
            send163Email(email, "mail-163-1");
        } else if (dailyCount < MAIL_163_2_DAILY_LIMIT) {
            // 超过 300 封：切换为第二个 163 邮箱发送
            Logger.info("邮件渠道路由：今日已发送 {} 封，切换为 mail-163-2", dailyCount);
            send163Email(email, "mail-163-2");
        } else if (dailyCount < TENCENT_DAILY_LIMIT) {
            // 超过 600 封且未达 21000 上限：腾讯云 SES 发送
            Logger.info("邮件渠道路由：今日已发送 {} 封，切换为腾讯云 SES", dailyCount);
            sendTencentCloudEmail(email.getTo(), email.getSubject(), email.getText());
        } else {
            // 超过 21000 封：达到腾讯云上限，拒绝发送
            Logger.error("邮件渠道路由：今日已发送 {} 封，达到腾讯云 21000 封上限，拒绝发送", dailyCount);
            throw new ServiceException(ResultCode.INTERFACE_DAILY_SENDING_LIMIT);
        }

        // 发送成功，递增当日累计计数
        redisTemplate.opsForValue().increment(dailyKey);
        redisTemplate.expire(dailyKey, 1, TimeUnit.DAYS);
    }

    /**
     * 使用指定渠道标识的 163 邮箱发送
     *
     * @param email      邮件内容
     * @param accountKey 渠道标识，如 mail-163-1 / mail-163-2
     */
    private void send163Email(EmailFormDTO email, String accountKey) {
        sendBySender(getSender(accountKey), email);
    }

    /**
     * 根据渠道标识获取邮件发送器（首次获取后缓存复用）
     *
     * @param accountKey 渠道标识，如 mail-163-1 / mail-163-2
     * @return 邮件发送器
     */
    private JavaMailSender getSender(String accountKey) {
        return senderCache.computeIfAbsent(accountKey, this::createSender);
    }

    /**
     * 根据渠道标识从数据库读取配置并创建邮件发送器
     *
     * @param accountKey 渠道标识
     * @return 配置好的邮件发送器
     */
    private JavaMailSenderImpl createSender(String accountKey) {
        SmtpConfig config = smtpConfigMapper.selectOne(new LambdaQueryWrapper<SmtpConfig>()
                .eq(SmtpConfig::getAccountKey, accountKey)
                .eq(SmtpConfig::getEnabled, Boolean.TRUE));
        if (config == null) {
            throw new ServiceException(ResultCode.SYSTEM_INNER_ERROR);
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());
        sender.setUsername(config.getUsername());
        sender.setPassword(config.getPassword());
        sender.setProtocol(config.getProtocol() != null ? config.getProtocol() : "smtp");
        sender.setDefaultEncoding(config.getDefaultEncoding() != null ? config.getDefaultEncoding() : "UTF-8");

        // 配置 SSL 相关属性
        if (Boolean.TRUE.equals(config.getSslEnable())) {
            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.ssl.enable", "true");
            if (config.getPort() != null) {
                props.put("mail.smtp.socketFactory.port", String.valueOf(config.getPort()));
            }
            props.put("mail.smtp.socketFactoryClass", "javax.net.ssl.SSLSocketFactory");
        }
        return sender;
    }

    /**
     * 通过指定 SMTP 发送器发送邮件
     *
     * @param sender SMTP 发送器（对应某个邮箱渠道）
     * @param email  邮件内容
     */
    private void sendBySender(JavaMailSender sender, EmailFormDTO email) {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        // 发件人使用当前SMTP账号（跟随渠道配置），不依赖业务层传入的 from
        simpleMailMessage.setFrom(((JavaMailSenderImpl) sender).getUsername());
        simpleMailMessage.setTo(email.getTo());
        simpleMailMessage.setSubject(email.getSubject());
        simpleMailMessage.setText(email.getText());
        sender.send(simpleMailMessage);
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
