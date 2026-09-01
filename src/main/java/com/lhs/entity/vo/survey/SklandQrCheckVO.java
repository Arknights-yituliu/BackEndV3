package com.lhs.entity.vo.survey;

import lombok.Data;

/**
 * 森空岛扫码登录 - 扫码状态查询 VO
 * <p>
 * status 复用上游语义：100 未扫码 / 101 已扫待确认 / 102 已过期 / 0 完成（此时返回凭证）
 */
@Data
public class SklandQrCheckVO {

    /** 扫码状态（上游 status） */
    private Integer status;

    /** 状态说明（上游 msg） */
    private String msg;

    /** 森空岛 cred（status=0 时有效） */
    private String cred;

    /** 森空岛临时 token（status=0 时有效） */
    private String token;
}
