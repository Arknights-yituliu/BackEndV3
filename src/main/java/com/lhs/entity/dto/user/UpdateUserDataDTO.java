package com.lhs.entity.dto.user;

import lombok.Data;

@Data
public class UpdateUserDataDTO {
    private String property;
    private String token;
    private String userName;
    private String email;
    private String emailCode;
    private String newPassWord;
    private String oldPassWord;
    private String avatar;
}
