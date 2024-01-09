package com.lhs.entity.dto.util;

import lombok.Data;

@Data
public class EmailFormDTO {
    private String from;
    private String to;
    private String cc;
    private String subject;
    private String text;

}
