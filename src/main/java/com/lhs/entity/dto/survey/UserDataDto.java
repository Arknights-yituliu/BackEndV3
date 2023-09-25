package com.lhs.entity.dto.survey;

import lombok.Data;

@Data
public class UserDataDto {

    private String userName;//用户名
    private String email; //邮箱
    private String accountType;
    private String passWord;//密码
    private String emailCode; //邮箱验证码
}
