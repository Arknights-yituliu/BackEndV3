package com.lhs.entity.po.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * OAuth2 用户中心资料缓存实体
 * <p>
 * 用户中心迁移后本地不再自建账号，本表仅缓存 UC 用户资料副本，id 即 UC uid，
 * 不保存任何身份/密码信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("oauth_user_info")
public class OAuthUserInfo {

    /** UC uid，资料缓存表主键 */
    @TableId
    private Long id;

    /** 昵称（UC 扩展字段，可空） */
    private String nickname;

    /** 用户头像 */
    private String avatar;

    /** 邮箱 */
    private String email;

    /** 用户状态，1正常，0封禁 */
    private Integer status;

    /** 首次 UC 登录时间 */
    private Date createTime;

    /** 资料更新时间 */
    private Date updateTime;

    /** 删除标记 */
    private Boolean deleteFlag;
}
