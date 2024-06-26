package com.lhs.service.survey.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.HttpRequestUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.dto.hypergryph.PlayerBinding;
import com.lhs.entity.vo.survey.AKPlayerBindingListVO;
import com.lhs.service.survey.HypergryphService;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class HypergryphServiceImpl implements HypergryphService {

    private final static String SKLAND_DOMAIN = "https://zonai.skland.com";
    private final static String OAUTH2_URL = "https://as.hypergryph.com/user/oauth2/v2/grant";
    private final static String GENERATE_CRED_BY_CODE_URL = "/api/v1/user/auth/generate_cred_by_code";
    private final static String PLAYER_INFO_URL = "/api/v1/game/player/info";
    private final static String PLAYER_BINDING_URL = "/api/v1/game/player/binding";


    @Override
    public AKPlayerBindingListVO getPlayerBindingsByHGToken(String hgToken) {

        HashMap<String, String> sklandCredAndToken = getSklandCredAndToken(hgToken);
        return getPlayerBindingsBySkland(sklandCredAndToken);
    }

    @Override
    public Map<String, Object> getCredAndTokenAndPlayerBindingsByHgToken(String hgToken) {
        HashMap<String, String> sklandCredAndToken = getSklandCredAndToken(hgToken);

        Map<String, Object> result = new HashMap<>();
        String cred = sklandCredAndToken.get("cred");
        String sklandToken = sklandCredAndToken.get("sklandToken");
        AKPlayerBindingListVO playerBindingsBySkland = getPlayerBindingsBySkland(sklandCredAndToken);

        result.put("cred",cred);
        result.put("token",sklandToken);
        result.put("playerBindingList",playerBindingsBySkland.getPlayerBindingList());

        return result;
    }


    @Override
    public AKPlayerBindingListVO getPlayerBindingsBySkland(HashMap<String, String> sklandCredAndToken) {

        String timestamp = String.valueOf((System.currentTimeMillis() - 800) / 1000);

        String cred = sklandCredAndToken.get("cred");
        String token = sklandCredAndToken.get("sklandToken");


        //根据长期token生成一个短期token
        String sign = GenerateSign(timestamp, token);

        Map<String, String> header = new LinkedHashMap<>();
        header.put("platform", "3");
        header.put("timestamp", timestamp);
        header.put("dId", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/118.0");
        header.put("vName", "1.2.0");
        header.put("cred", cred);
        header.put("sign", sign);
        String playerBindingResponseText = HttpRequestUtil.get(SKLAND_DOMAIN + PLAYER_BINDING_URL, header);
        JsonNode playerBindingResponse = JsonMapper.parseJSONObject(playerBindingResponseText);
        int code = playerBindingResponse.get("code").asInt();

        List<PlayerBinding> playerBindingList = new ArrayList<>();

        AKPlayerBindingListVO akPlayerBindingListVO = new AKPlayerBindingListVO();

        if (code == 0) {
            JsonNode data = playerBindingResponse.get("data");
            JsonNode list = data.get("list");
            for (JsonNode jsonNode : list) {
                String appCode = jsonNode.get("appCode").asText();
                if ("arknights".equals(appCode)) {
                    String defaultUid = jsonNode.get("defaultUid").asText();
                    PlayerBinding defaultBinding = null;
                    JsonNode bindingList = jsonNode.get("bindingList");

                    for (JsonNode binding : bindingList) {
                        String uid = binding.get("uid").asText();
                        boolean isOfficial = binding.get("isOfficial").asBoolean();
                        boolean isDefault = binding.get("isDefault").asBoolean();
                        String channelMasterId = binding.get("channelMasterId").asText();
                        String channelName = binding.get("channelName").asText();
                        String nickName = binding.get("nickName").asText();

                        PlayerBinding playerBinding = new PlayerBinding();
                        playerBinding.setUid(uid);
                        playerBinding.setDefaultFlag(isDefault);
                        playerBinding.setNickName(nickName);
                        playerBinding.setChannelMasterId(channelMasterId);
                        playerBinding.setChannelName(channelName);
                        playerBindingList.add(playerBinding);



                        if (uid.equals(defaultUid)) {
                            defaultBinding = playerBinding;
                        }

                        if (defaultBinding != null) {
                            continue;
                        }

                        if (isDefault) {
                            defaultBinding = playerBinding;
                        }

                        if (isOfficial) {
                            defaultBinding = playerBinding;
                        }


                    }

                    if (defaultBinding == null) {
                        defaultBinding = playerBindingList.get(0);
                    }
                    akPlayerBindingListVO.setPlayerBinding(defaultBinding);
                    akPlayerBindingListVO.setPlayerBindingList(playerBindingList);
                }
            }

        }

        return akPlayerBindingListVO;
    }



    private HashMap<String,String> getSklandCredAndToken(String hgToken){
        HashMap<Object, Object> requestParams = new HashMap<>();
        requestParams.put("token", hgToken);
        requestParams.put("appCode", "4ca99fa6b56cc2ba");
        requestParams.put("type", 0);

        String dataString = JsonMapper.toJSONString(requestParams);

        //向鹰角通行证请求授权
        String OAUTH2ResponseText = HttpRequestUtil.post(OAUTH2_URL, new HashMap<>(), dataString);
        if (OAUTH2ResponseText == null) {
            throw new ServiceException(ResultCode.AUTHORIZATION_FAILURE);
        }

        //返回的授权凭证
        JsonNode oauth2Response = JsonMapper.parseJSONObject(OAUTH2ResponseText);
        requestParams.clear();
        requestParams.put("kind", 1);
        requestParams.put("code", oauth2Response.get("data").get("code").asText());
        dataString = JsonMapper.toJSONString(requestParams);

        //获取森空岛凭证和临时token
        String CREDResponseText = HttpRequestUtil.post(SKLAND_DOMAIN + GENERATE_CRED_BY_CODE_URL, new HashMap<>(), dataString);

        //返回的森空岛凭证和token
        JsonNode credResponse = JsonMapper.parseJSONObject(CREDResponseText).get("data");
        String cred = credResponse.get("cred").asText();
        String sklandToken = credResponse.get("token").asText();

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("cred",cred);
        hashMap.put("sklandToken",sklandToken);

        return hashMap;
    }

    private String GenerateSign(String timestamp, String token) {

        Map<String, String> map = new LinkedHashMap<>();
        map.put("platform", "3");
        map.put("timestamp", timestamp);
        map.put("dId", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/118.0");
        map.put("vName", "1.2.0");
        String text = HypergryphServiceImpl.PLAYER_BINDING_URL + timestamp + JsonMapper.toJSONString(map);
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
        return sign;
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
