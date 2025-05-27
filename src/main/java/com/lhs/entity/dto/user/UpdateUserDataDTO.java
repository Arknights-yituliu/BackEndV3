package com.lhs.entity.dto.user;

import lombok.Data;

@Data
public class UpdateUserDataDTO {
    private String property;
    private String action;
    private String token;
    private String userName;
    private String oldEmail;
    private String email;
    private String verificationCode;
    private String newPassWord;
    private String oldPassWord;
    private String avatar;
    private String cred;
}
