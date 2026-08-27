package com.lhs.service.survey.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.HttpRequestUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.entity.vo.survey.SklandCredTokenVO;
import com.lhs.service.survey.SklandHgTokenService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 鹰角官网 token 换取森空岛 cred/token 的服务实现
 * <p>
 * 对应前端 src/utils/survey/skland.js 中的 getCredAndTokenByHgToken 方法
 */
@Service
public class SklandHgTokenServiceImpl implements SklandHgTokenService {

    /** grant 接口：用 hg token 换取一次性 code */
    private static final String OAUTH2_URL = "https://as.hypergryph.com/user/oauth2/v2/grant";

    /** code 换取 cred/token 接口 */
    private static final String GENERATE_CRED_BY_CODE_URL = "https://zonai.skland.com/web/v1/user/auth/generate_cred_by_code";

    /** 森空岛应用 code */
    private static final String APP_CODE = "4ca99fa6b56cc2ba";

    /** 设备 id（与前端 skland.js 保持一致） */
    private static final String DEVICE_ID = "bebd9eee5ad0411dacaee5075792ea2a";

    /** dId（与前端 skland.js 保持一致） */
    private static final String D_ID = "BqbjCY5HN8T+MFmjvDT4ceaUO6zSMuvdts2+67sjAL4QTA3GfE7M1rDkuUo8Hbhl03557VponqkJW2Z1wh+/nYA==";

    /** 通用 UA */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:153.0) Gecko/20100101 Firefox/153.0";

    @Override
    public SklandCredTokenVO getCredAndTokenByHgToken(String hgToken) {
        // 第一步：用 hg token 调 grant 接口换取一次性 code
        String code = grant(hgToken);
        // 第二步：用 code 换取 cred 和 token
        return generateCredByCode(code);
    }

    /**
     * 第一步：调 grant 接口，用 hg token 换取一次性 code
     *
     * @param hgToken 鹰角官网 token（data.content）
     * @return 一次性 code
     */
    private String grant(String hgToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("token", hgToken);
        body.put("appCode", APP_CODE);
        body.put("type", 0);

        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", USER_AGENT);
        headers.put("Accept", "*/*");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9");
        headers.put("X-DeviceModel", "Firefox");
        headers.put("X-DeviceType", "7");
        headers.put("X-OSVer", "Windows");
        headers.put("X-DeviceId", DEVICE_ID);
        headers.put("X-Captcha-Version", "4.0");

        JsonNode resp = postJson(OAUTH2_URL, body, headers);
        JsonNode data = resp.get("data");
        if (data == null || data.get("code") == null) {
            Logger.error("grant 接口调用失败，响应：{}", resp);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        return data.get("code").asText();
    }

    /**
     * 第二步：调 generate_cred_by_code 接口，用 code 换取 cred 和 token
     *
     * @param code 第一步换取的一次性 code
     * @return cred 和 token
     */
    private SklandCredTokenVO generateCredByCode(String code) {
        Map<String, Object> body = new HashMap<>();
        body.put("kind", 1);
        body.put("code", code);

        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", USER_AGENT);
        headers.put("Accept", "*/*");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9");
        headers.put("platform", "3");
        headers.put("vName", "1.0.0");
        headers.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        headers.put("dId", D_ID);

        JsonNode resp = postJson(GENERATE_CRED_BY_CODE_URL, body, headers);
        if (resp.get("code") == null || resp.get("code").asInt() != 0) {
            Logger.error("generate_cred_by_code 接口调用失败，响应：{}", resp);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        JsonNode data = resp.get("data");
        SklandCredTokenVO vo = new SklandCredTokenVO();
        vo.setCred(data.get("cred").asText());
        vo.setToken(data.get("token").asText());
        return vo;
    }

    /**
     * 发送 POST JSON 请求并解析响应
     *
     * @param url     请求地址
     * @param body    JSON 请求体
     * @param headers 请求头
     * @return 解析后的响应 JSON
     */
    private JsonNode postJson(String url, Map<String, Object> body, Map<String, String> headers) {
        String responseText = HttpRequestUtil.post(url, new HashMap<>(headers), JsonMapper.toJSONString(body));
        if (responseText == null) {
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        return JsonMapper.parseJSONObject(responseText);
    }
}
