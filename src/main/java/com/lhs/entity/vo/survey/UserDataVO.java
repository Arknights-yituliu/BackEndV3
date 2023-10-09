package com.lhs.entity.vo.survey;

import com.lhs.common.annotation.Sensitive;
import com.lhs.entity.po.survey.SurveyUser;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userName;
    private String nickName;
    @Sensitive(prefixNoMaskLen = 4,suffixNoMaskLen = 6)
    private String email;
    private String uid;
    private Integer status;
    private String token;
    private String avatar;


}
