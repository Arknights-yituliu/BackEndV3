package com.lhs.entity.vo.survey;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long uid;
    private String nickname;

    /** 邮箱（仅内部逻辑使用，不对外返回） */
    @JsonIgnore
    private String email;
    private Integer status;
    private String token;
    private String avatar;

    /** 方舟 uid（仅内部逻辑使用，不对外返回） */
    @JsonIgnore
    private String akUid;

    /** 是否有邮箱（仅内部逻辑使用，不对外返回） */
    @JsonIgnore
    private Boolean hasEmail = false;
}
