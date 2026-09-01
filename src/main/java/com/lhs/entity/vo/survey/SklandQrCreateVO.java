package com.lhs.entity.vo.survey;

import lombok.Data;

/**
 * 森空岛扫码登录 - 二维码会话 VO
 */
@Data
public class SklandQrCreateVO {

    /** 扫码会话 ID */
    private String scanId;

    /** 二维码内容（deep link，如 hypergryph://scan_login?scanId=xxx），供前端渲染二维码 */
    private String qrContent;
}
