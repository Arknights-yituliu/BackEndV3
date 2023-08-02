package com.lhs.service.dev;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.Log;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.other.Developer;
import com.lhs.entity.other.PageVisits;
import com.lhs.entity.other.Visits;
import com.lhs.mapper.PageVisitsMapper;

import com.lhs.mapper.VisitsMapper;
import com.lhs.vo.user.LoginVo;
import com.lhs.mapper.DeveloperMapper;
import com.lhs.vo.user.EmailRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserService {

    @Resource
    JavaMailSender javaMailSender;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private DeveloperMapper developerMapper;
    @Resource
    private UserService userService;
    @Resource
    private VisitsMapper visitsMapper;


    private PageVisitsMapper pageVisitsMapper;

    public UserService(PageVisitsMapper pageVisitsMapper){
        this.pageVisitsMapper = pageVisitsMapper;
    }

    public void sendMail(EmailRequest emailRequest) {
        SimpleMailMessage smm = new SimpleMailMessage();
        smm.setFrom(emailRequest.getFrom());//发送者
        smm.setTo(emailRequest.getTo());//收件人
        smm.setSubject(emailRequest.getSubject());//邮件主题
        smm.setText(emailRequest.getText());//邮件内容
        javaMailSender.send(smm);//发送邮件
    }

    public Boolean developerLevel(HttpServletRequest request) {
        String token = request.getHeader("token");
        String developerBase64 = token.split("\\.")[0];
        String decode = new String(Base64.getDecoder().decode(developerBase64), StandardCharsets.UTF_8);
        QueryWrapper<Developer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("developer", decode);
        Developer developer = developerMapper.selectOne(queryWrapper);
        return developer.getLevel() == 0;
    }

    public void emailSendCode(LoginVo loginVo) {
        QueryWrapper<Developer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("developer", loginVo.getDeveloper());
        Developer developer = developerMapper.selectOne(queryWrapper);
        if (developer == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
//        String token =  developer.getDeveloper()+developer.getEmail()+new Date().getTime();
        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setFrom("1820702789@qq.com");
        emailRequest.setTo(developer.getEmail());
        emailRequest.setSubject("开发者登录");
        int random = new Random().nextInt(999999);
        String code = String.format("%6s", random).replace(" ", "0");
        redisTemplate.opsForValue().set("CODE:" + developer.getEmail() + "CODE", code, 300, TimeUnit.SECONDS);
        emailRequest.setText("本次登录验证码：" + code);
        userService.sendMail(emailRequest);
    }


    public String login(LoginVo loginVo) {
        QueryWrapper<Developer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("developer", loginVo.getDeveloper());
        Developer developer = developerMapper.selectOne(queryWrapper);
        if (developer == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        String code = String.valueOf(redisTemplate.opsForValue().get("CODE:" + developer.getEmail() + "CODE"));
        //检查邮件验证码
        if (!loginVo.getCode().equals(code)) {
            throw new ServiceException(ResultCode.USER_LOGIN_CODE_ERROR);
        }

        //登录时间
        String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        JSONObject header = new JSONObject();
        header.put("developer", loginVo.getDeveloper());
        header.put("loginDate", format);
        String headerStr = JSON.toJSONString(header);  //token头是登录名+登录时间的map
//        类似jwt
//        String HeaderBase64 =  Base64.getEncoder().encodeToString((headerStr).getBytes());
//        String sign = AES.encrypt(headerStr+ConfigUtil.SignKey, ConfigUtil.Secret);
//        String token =  HeaderBase64+"."+sign;
        String sign = AES.encrypt(headerStr + ApplicationConfig.SignKey, ApplicationConfig.Secret);  //组成签名：token头+签名key
        String developerBase64 = Base64.getEncoder().encodeToString(loginVo.getDeveloper().getBytes());  //进行base64转换

        String token = developerBase64 + "." + sign;  //完整token：token头.token尾
        redisTemplate.opsForValue().set("TOKEN:" + developerBase64, token, 45, TimeUnit.DAYS);
        return token;
    }

    public Boolean loginAndCheckToken(String token) {

//        log.info("开发者token：" + token);
        //        类似jwt
//        String headerStr =  new String(Base64.getDecoder().decode(token.split("\\.")[0].getBytes()), StandardCharsets.UTF_8);
//        String sign = token.split("\\.")[1];
//        String newSign = AES.encrypt(headerStr+ConfigUtil.SignKey, ConfigUtil.Secret);
//        if(!sign.equals(newSign)) {
//            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
//        }
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

        return true;
    }

    public void updatePageVisits(String path) {
        String format = new SimpleDateFormat("yyyy/MM/dd HH").format(new Date());
        String redisKey = "visits:" + format + "." + path;
        redisTemplate.opsForValue().increment(redisKey);
    }

    @Scheduled(cron = "0 0 0/1 * * ?")
    public void savePageVisits() {
        Date todayDate = new Date();
        long timesTamp = System.currentTimeMillis();

        //要保存的记录
        List<PageVisits> pageVisitsList = new ArrayList<>();

        //查询缓存中命名空间【visits:】下的所有key
        Set<String> keys = redisTemplate.keys("visits:*");
        if(keys == null) {
            Log.info("没有访问记录");
            return;
        }

        String yyyyMMddHH = new SimpleDateFormat("yyyy/MM/dd HH").format(new Date());

        for (String key : keys) {
            Object redisValue = redisTemplate.opsForValue().get(key);
            int visitsCount = Integer.parseInt(String.valueOf(redisValue));
            String[] keySplit = key.split("\\.");
            String path = keySplit[1];
            String namespaceAndTime = keySplit[0];
            String visitsTime = namespaceAndTime.split(":")[1];

            if(yyyyMMddHH.equals(visitsTime)){
                Log.info("当时小时的访问未结束，不保存");
                continue;
            }

            QueryWrapper<PageVisits> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("redis_key",key);
            PageVisits savedPageVisits = pageVisitsMapper.selectOne(queryWrapper);

            if (savedPageVisits!=null) {

                if(visitsCount> savedPageVisits.getVisitsCount()) {
                    QueryWrapper<PageVisits> updateWrapper = new QueryWrapper<>();
                    updateWrapper.eq("id",savedPageVisits.getId());
                    savedPageVisits.setVisitsCount(visitsCount);
                    pageVisitsMapper.update(savedPageVisits,updateWrapper);
                    Log.info("更新记录");
                }
                redisTemplate.delete(key);
                continue;
            }

            PageVisits pageVisits = new PageVisits();
            pageVisits.setVisitsCount(visitsCount);

//            Log.info("redis的key："+key+"   访问路径："+path+"   访问时间："+visitsTime);

            pageVisits.setId(timesTamp++);
            pageVisits.setVisitsTime(visitsTime);
            pageVisits.setPagePath(path);
            pageVisits.setCreateTime(todayDate);
            pageVisits.setRedisKey(key);
            pageVisitsList.add(pageVisits);

        }

        if (pageVisitsList.size() > 0) pageVisitsMapper.insertBatch(pageVisitsList);

    }


    public void updateVisits(String path) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Visits visitResult = visitsMapper.selectOne(new QueryWrapper<Visits>()
                .eq("date", today));
        if (visitResult == null) {
            visitResult = new Visits();
            visitResult.init();
            visitResult.update(path);
            visitsMapper.insert(visitResult);
        } else {
            visitResult.update(path);
            visitsMapper.updateById(visitResult);
        }
    }


    public HashMap<String, List<Object>> selectVisits(Date start, Date end) {

        QueryWrapper<Visits> create_time = new QueryWrapper<Visits>().ge("create_time", start).le("create_time", end);
        List<Visits> visitsList = visitsMapper.selectList(create_time);
        HashMap<String, List<Object>> hashMap = new HashMap<>();
        for (Visits visits : visitsList) {
            JSONObject visitsJson = JSONObject.parseObject(JSON.toJSONString(visits));
            Set<String> paths = JSONObject.parseObject(visitsJson.toString()).keySet();
            for (String path : paths) {
                if (hashMap.get(path) != null) {
                    List<Object> visitsResult = hashMap.get(path);
                    visitsResult.add(visitsJson.getString(path));
                    hashMap.put(path, visitsResult);
                } else {
                    List<Object> visitsResult = new ArrayList<>();
                    hashMap.put(path, visitsResult);
                }
            }
        }
        return hashMap;
    }


}
