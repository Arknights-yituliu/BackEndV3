package com.lhs.entity.vo.survey;

import com.lhs.common.annotation.Sensitive;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userName;
    private String nickName;
    @Sensitive(prefixNoMaskLen = 4,suffixNoMaskLen = 6)
    private String email;
    @Sensitive(prefixNoMaskLen = 1,suffixNoMaskLen = 1)
    private String uid;
    private Integer status;
    private String token;
    private String avatar;
}
