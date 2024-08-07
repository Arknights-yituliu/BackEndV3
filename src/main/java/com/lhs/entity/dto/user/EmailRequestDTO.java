package com.lhs.entity.dto.user;

import lombok.Data;


@Data
public class EmailRequestDTO {
    private String mailUsage; //邮件用途
    private String token; //用户凭证
    private String email; //邮箱
}
