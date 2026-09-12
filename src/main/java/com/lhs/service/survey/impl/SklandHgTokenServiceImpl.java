package com.lhs.service.survey.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.HttpRequestUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.entity.vo.survey.SklandCredTokenVO;
import com.lhs.entity.vo.survey.SklandQrCheckVO;
import com.lhs.entity.vo.survey.SklandQrCreateVO;
import com.lhs.service.survey.SklandHgTokenService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 鹰角官网 token 换取森空岛 cred/token 的服务实现
 * <p>
 * 包含两条链路：
 * 1. hg token 直接换取：grant 换一次性 code → generate_cred_by_code 换 cred/token
 * 2. 扫码登录：申请二维码 → 轮询扫码状态 → scanCode 换通行证 token → 走链路 1
 * <p>
 * 对应前端 src/utils/survey/skland.js 中的 getCredAndTokenByHgToken 方法
 */
@Service
public class SklandHgTokenServiceImpl implements SklandHgTokenService {

    /** grant 接口：用 hg token 换取一次性 code */
    private static final String OAUTH2_URL = "https://as.hypergryph.com/user/oauth2/v2/grant";

    /** code 换取 cred/token 接口 */
    private static final String GENERATE_CRED_BY_CODE_URL = "https://zonai.skland.com/web/v1/user/auth/generate_cred_by_code";

    /** 申请扫码二维码接口 */
    private static final String GEN_SCAN_LOGIN_URL = "https://as.hypergryph.com/general/v1/gen_scan/login";

    /** 扫码状态查询接口 */
    private static final String SCAN_STATUS_URL = "https://as.hypergryph.com/general/v1/scan_status";

    /** 一次性 scanCode 换取通行证 token 接口 */
    private static final String TOKEN_BY_SCAN_CODE_URL = "https://as.hypergryph.com/user/auth/v1/token_by_scan_code";

    /** 森空岛应用 code */
    private static final String APP_CODE = "4ca99fa6b56cc2ba";

    /** 设备 id（与前端 skland.js 保持一致） */
    private static final String DEVICE_ID = "bebd9eee5ad0411dacaee5075792ea2a";

    /** dId（与前端 skland.js 保持一致） */
    private static final String D_ID = "BqbjCY5HN8T+MFmjvDT4ceaUO6zSMuvdts2+67sjAL4QTA3GfE7M1rDkuUo8Hbhl03557VponqkJW2Z1wh+/nYA==";

    /** 通用 UA */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:153.0) Gecko/20100101 Firefox/153.0";

    /** 扫码二维码 deep link 前缀，scanUrl 缺省时用 scanId 拼接 */
    private static final String DEEP_LINK_PREFIX = "hypergryph://scan_login?scanId=";

    /**
     * 通过鹰角官网 hg token 换取森空岛 cred 和临时 token
     *
     * @param hgToken 鹰角官网 token（data.content）
     * @return 森空岛 cred 和 token
     */
    @Override
    public SklandCredTokenVO getCredAndTokenByHgToken(String hgToken) {
        // 第一步：用 hg token 调 grant 接口换取一次性 code
        String code = grant(hgToken);
        // 第二步：用 code 换取 cred 和 token
        return generateCredByCode(code);
    }

    /**
     * 申请扫码登录二维码
     *
     * @return 扫码会话（scanId + 二维码内容）
     */
    @Override
    public SklandQrCreateVO createQrLogin() {
        Map<String, Object> body = new HashMap<>();
        body.put("appCode", APP_CODE);

        JsonNode resp = postJson(GEN_SCAN_LOGIN_URL, body, buildScanHeaders());
        if (resp.get("status") == null || resp.get("status").asInt() != 0 || resp.get("data") == null) {
            Logger.error("森空岛申请扫码二维码失败，响应：{}", resp);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        JsonNode data = resp.get("data");
        String scanId = data.get("scanId").asText();
        // scanUrl 可能缺省，缺省时用 scanId 自行拼接 deep link
        JsonNode scanUrlNode = data.get("scanUrl");
        String scanUrl = scanUrlNode != null && !scanUrlNode.isNull() ? scanUrlNode.asText() : null;
        String qrContent = (scanUrl != null && !scanUrl.isEmpty()) ? scanUrl : DEEP_LINK_PREFIX + scanId;

        SklandQrCreateVO vo = new SklandQrCreateVO();
        vo.setScanId(scanId);
        vo.setQrContent(qrContent);
        return vo;
    }

    /**
     * 查询扫码状态；用户确认后自动完成换取，返回森空岛凭证
     *
     * @param scanId 扫码会话 ID
     * @return 扫码状态（100/101/102 时前端继续轮询，0 时携带 cred/token）
     */
    @Override
    public SklandQrCheckVO checkQrLogin(String scanId) {
        JsonNode resp = getJson(SCAN_STATUS_URL + "?scanId=" + scanId, buildScanHeaders());
        int status = resp.get("status") == null ? -1 : resp.get("status").asInt();

        SklandQrCheckVO vo = new SklandQrCheckVO();
        vo.setStatus(status);
        vo.setMsg(resp.get("msg") != null ? resp.get("msg").asText() : null);

        // 用户已确认，用 scanCode 换通行证 token，再换森空岛凭证
        if (status == 0) {
            JsonNode data = resp.get("data");
            if (data == null || data.get("scanCode") == null) {
                Logger.error("森空岛扫码确认成功但缺少 scanCode，响应：{}", resp);
                throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
            }
            String scanCode = data.get("scanCode").asText();
            String hgToken = exchangeHgToken(scanCode);
            SklandCredTokenVO credToken = getCredAndTokenByHgToken(hgToken);
            vo.setCred(credToken.getCred());
            vo.setToken(credToken.getToken());
        }
        return vo;
    }

    /**
     * 用一次性 scanCode 换取鹰角通行证 token
     *
     * @param scanCode 确认后的一次性授权码
     * @return 鹰角通行证 token
     */
    private String exchangeHgToken(String scanCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("scanCode", scanCode);

        JsonNode resp = postJson(TOKEN_BY_SCAN_CODE_URL, body, buildScanHeaders());
        if (resp.get("status") == null || resp.get("status").asInt() != 0
                || resp.get("data") == null || resp.get("data").get("token") == null) {
            Logger.error("森空岛扫码换通行证 token 失败，响应：{}", resp);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        return resp.get("data").get("token").asText();
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
            // 提取森空岛返回的 msg 透传给前端（如"需要进行设备验证"）
            JsonNode msgNode = resp.get("msg");
            if (msgNode != null && !msgNode.asText().isEmpty()) {
                throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR,
                        "来自森空岛的报错：" + msgNode.asText());
            }
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
            // 提取森空岛返回的 msg 透传给前端（如"需要进行设备验证"）
            JsonNode msgNode = resp.get("msg");
            if (msgNode != null && !msgNode.asText().isEmpty()) {
                throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR,
                        "来自森空岛的报错：" + msgNode.asText());
            }
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

    /**
     * 发送 GET 请求并解析响应
     *
     * @param url     请求地址（含 query）
     * @param headers 请求头
     * @return 解析后的响应 JSON
     */
    private JsonNode getJson(String url, Map<String, String> headers) {
        String responseText = HttpRequestUtil.get(url, headers);
        if (responseText == null) {
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        return JsonMapper.parseJSONObject(responseText);
    }

    /**
     * 构建扫码接口通用请求头（参考官方前端 user.hypergryph.com）
     *
     * @return 请求头
     */
    private Map<String, String> buildScanHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Origin", "https://user.hypergryph.com");
        headers.put("Referer", "https://user.hypergryph.com/");
        return headers;
    }
}
