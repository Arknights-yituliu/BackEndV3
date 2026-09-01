package com.lhs.service.survey;

import com.lhs.entity.vo.survey.SklandCredTokenVO;
import com.lhs.entity.vo.survey.SklandQrCheckVO;
import com.lhs.entity.vo.survey.SklandQrCreateVO;

/**
 * 鹰角官网 token 换取森空岛 cred/token 的服务
 * <p>
 * 流程：
 * 1. 调 as.hypergryph.com/user/oauth2/v2/grant，用 hg token 换取一次性 code
 * 2. 调 zonai.skland.com/web/v1/user/auth/generate_cred_by_code，用 code 换取 cred 和 token
 * <p>
 * 同时承载森空岛扫码登录：申请二维码 → 轮询扫码状态 → 确认后换通行证 token，再走上述标准流程
 */
public interface SklandHgTokenService {

    /**
     * 通过鹰角官网 hg token 换取森空岛 cred 和临时 token
     *
     * @param hgToken 鹰角官网 token（account/info/hg 接口返回的 data.content）
     * @return 森空岛 cred 和 token
     */
    SklandCredTokenVO getCredAndTokenByHgToken(String hgToken);

    /**
     * 申请扫码登录二维码
     *
     * @return 扫码会话（scanId + 二维码内容）
     */
    SklandQrCreateVO createQrLogin();

    /**
     * 查询扫码状态；用户确认后自动完成换取，返回森空岛凭证
     *
     * @param scanId 扫码会话 ID
     * @return 扫码状态（100/101/102 时前端继续轮询，0 时携带 cred/token）
     */
    SklandQrCheckVO checkQrLogin(String scanId);
}
