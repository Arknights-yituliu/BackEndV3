package com.lhs.vo.survey;

import lombok.Data;

@Data
public class SurveyUserVo {
    private String userName;
    private String passWord;
    private String oldPassWord;
    private String cred;
    private String token;
}
