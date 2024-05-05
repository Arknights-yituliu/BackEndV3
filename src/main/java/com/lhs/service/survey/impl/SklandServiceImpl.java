package com.lhs.service.survey.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.HttpRequestUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.dto.hypergryph.PlayerBinding;
import com.lhs.service.survey.SklandService;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SklandServiceImpl implements SklandService {

    private final static String SKLAND_DOMAIN = "https://zonai.skland.com";
    private final static String OAUTH2_URL = "https://as.hypergryph.com/user/oauth2/v2/grant";
    private final static String GENERATE_CRED_BY_CODE_URL = "/api/v1/user/auth/generate_cred_by_code";
    private final static String PLAYER_INFO_URL = "/api/v1/game/player/info";
    private final static String PLAYER_BINDING_URL = "/api/v1/game/player/binding";


    @Override
    public PlayerBinding getPlayerBindings(String token) {


        HashMap<Object, Object> data = new HashMap<>();
        data.put("token",token);
        data.put("appCode","4ca99fa6b56cc2ba");
        data.put("type",0);

        String dataString = JsonMapper.toJSONString(data);

        String OAUTH2ResponseText = HttpRequestUtil.post(OAUTH2_URL, new HashMap<>(), dataString);
        if(OAUTH2ResponseText==null){
            throw new ServiceException(ResultCode.AUTHORIZATION_FAILURE);
        }

        JsonNode oauth2Response = JsonMapper.parseJSONObject(OAUTH2ResponseText);
        data.clear();
        data.put("kind",1);
        data.put("code",oauth2Response.get("data").get("code").asText());
        dataString = JsonMapper.toJSONString(data);
        String CREDResponseText = HttpRequestUtil.post(SKLAND_DOMAIN+GENERATE_CRED_BY_CODE_URL, new HashMap<>(), dataString);

        JsonNode credResponse = JsonMapper.parseJSONObject(CREDResponseText).get("data");
        String timestamp =  String.valueOf ((System.currentTimeMillis()-1000)/1000);

        String cred = credResponse.get("cred").asText();
        String tempToken = credResponse.get("token").asText();

        String sign = GenerateSign(PLAYER_BINDING_URL,"",timestamp,tempToken);

        Map<String, String> header = new LinkedHashMap<>();

        header.put("platform","3");
        header.put("timestamp",timestamp);
        header.put("dId","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/118.0");
        header.put("vName","1.2.0");
        header.put("cred",cred);

        header.put("sign",sign);
        System.out.println(sign);
        String playerResponseText = HttpRequestUtil.get(SKLAND_DOMAIN+PLAYER_BINDING_URL, header);
        System.out.println(playerResponseText);


        return null;
    }



    private String GenerateSign(String path,String params,String timestamp,String token){

        Map<String, String> map = new LinkedHashMap<>();
        map.put("platform","3");
        map.put("timestamp",timestamp);
        map.put("dId","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/118.0");
        map.put("vName","1.2.0");

        String text = path+params+timestamp+JsonMapper.toJSONString(map);
        System.out.println(text);
        String sign = "";

        try {

            // 创建一个HMAC-SHA256的Mac实例

            Mac sha256Hmac = Mac.getInstance("HmacSHA256");

            // 初始化Mac实例，使用密钥

            SecretKeySpec secretKeySpec = new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            sha256Hmac.init(secretKeySpec);


            // 执行HMAC计算

            byte[] hmacBytes = sha256Hmac.doFinal(text.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(hmacBytes.length * 2);
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }

            String sha256HmacText = sb.toString();

            sign = encryptToMD5(sha256HmacText);


        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Logger.error(e.getMessage());
        }


        return  sign;

    }


    private static String encryptToMD5(String input) throws NoSuchAlgorithmException {

        // 获取MD5实例
        MessageDigest md = MessageDigest.getInstance("MD5");
        // 更新要计算摘要的信息
        md.update(input.getBytes());

        // 计算消息摘要
        byte[] digest = md.digest();
        // 将摘要转为16进制字符串表示
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString(); // 返回32位小写的MD5散列值

    }
}
