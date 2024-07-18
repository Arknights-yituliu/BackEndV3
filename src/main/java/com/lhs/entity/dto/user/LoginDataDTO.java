package com.lhs.entity.dto.user;

import lombok.Data;

@Data
public class LoginDataDTO {

    private String userName;//用户名
    private String email; //邮箱
    private String accountType;
    private String password;//密码
    private String emailCode; //邮箱验证码
    private String verificationCode; //邮箱验证码
    private String token;
    private String hgToken;
    private String sklandCred;
}
