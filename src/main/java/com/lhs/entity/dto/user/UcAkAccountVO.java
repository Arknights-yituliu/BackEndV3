package com.lhs.entity.dto.user;

/**
 * UC 已绑定游戏账号（GET /oauth2/ak-accounts 响应元素）
 *
 * <p>UC 侧按 updateTime 倒序返回，取首条即用户最近导入数据的账号，
 * 因此本类只声明业务需要的 akUid，时间字段由 JSON 解析时忽略。</p>
 *
 * @author BackEndV3
 */
public class UcAkAccountVO {

    /** 游戏账号 UID */
    private String akUid;

    public String getAkUid() {
        return akUid;
    }

    public void setAkUid(String akUid) {
        this.akUid = akUid;
    }
}
