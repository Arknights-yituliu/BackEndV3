package com.lhs.entity.dto.survey;

import lombok.Data;

@Data
public class UpdateUserDataDto {
    private String property;
    private String token;
    private String userName;
    private String email;
    private String emailCode;
    private String newPassWord;
    private String oldPassWord;
}
