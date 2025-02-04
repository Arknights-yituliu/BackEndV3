package com.lhs.entity.vo.survey;

import com.lhs.common.annotation.Sensitive;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
public class UserInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long uid;
    private String userName;
    private String nickName;
    @Sensitive(prefixNoMaskLen = 4,suffixNoMaskLen = 6)
    private String email;
    private Integer status;
    private String token;
    private String avatar;
    private String akNickName;
    private String akUid;
    private Map<String,Object> config;
    private Boolean hasEmail = false;
    private Boolean hasPassword = false;
}
