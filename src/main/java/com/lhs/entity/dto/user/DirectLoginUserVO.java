package com.lhs.entity.dto.user;

import lombok.Data;

/**
 * 直连登录-兑换用户信息响应 VO
 * <p>
 * 对应 UC /oauth2/direct-user 接口 data 部分：uid、昵称、头像、脱敏邮箱、状态
 */
@Data
public class DirectLoginUserVO {

    /** 用户 uid（本地资料缓存表主键/账号打通 key） */
    private Long uid;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 邮箱（脱敏，仅供展示） */
    private String email;

    /** 状态：1=正常；-1=封禁 */
    private Integer status;
}
