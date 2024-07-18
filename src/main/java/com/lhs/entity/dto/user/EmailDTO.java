package com.lhs.entity.dto.user;

import lombok.Data;

@Data
public class EmailDTO {
    private String emailToAddress;
    private String subject;
    private String context;
}
