package com.lhs.entity.vo.survey;

import lombok.Data;

/**
 * 森空岛 cred 与临时 token 响应 VO
 * <p>
 * 通过鹰角官网 hg token 换取，供前端后续调用森空岛 API 使用
 */
@Data
public class SklandCredTokenVO {

    /** 森空岛凭证 */
    private String cred;

    /** 森空岛临时 token */
    private String token;
}
