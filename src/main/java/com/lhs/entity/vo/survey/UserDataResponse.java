package com.lhs.entity.vo.survey;

import lombok.Data;

@Data
public class UserDataResponse {
    private String userName;
    private String nickName;
    private String email;
    private String uid;
    private Integer status;
    private String token;
}
