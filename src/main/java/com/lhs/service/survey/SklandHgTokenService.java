package com.lhs.service.survey;

import com.lhs.entity.vo.survey.SklandCredTokenVO;

/**
 * 鹰角官网 token 换取森空岛 cred/token 的服务
 * <p>
 * 流程：
 * 1. 调 as.hypergryph.com/user/oauth2/v2/grant，用 hg token 换取一次性 code
 * 2. 调 zonai.skland.com/web/v1/user/auth/generate_cred_by_code，用 code 换取 cred 和 token
 */
public interface SklandHgTokenService {

    /**
     * 通过鹰角官网 hg token 换取森空岛 cred 和临时 token
     *
     * @param hgToken 鹰角官网 token（account/info/hg 接口返回的 data.content）
     * @return 森空岛 cred 和 token
     */
    SklandCredTokenVO getCredAndTokenByHgToken(String hgToken);
}
