package com.lhs.entity.dto.survey;

import lombok.Data;

@Data
public class EmailDTO {
    private String emailToAddress;
    private String subject;
    private String context;
}
