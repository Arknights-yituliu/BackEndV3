package com.lhs.vo.survey;

import lombok.Data;

@Data
public class UserDataResponse {
    private String userName;
    private Integer status;
    private String token;
    private Boolean registered;
}
