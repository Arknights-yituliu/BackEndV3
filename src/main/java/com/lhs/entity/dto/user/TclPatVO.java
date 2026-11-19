package com.lhs.entity.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** TCL PAT 签发结果；PAT 与 OAuth access_token 是不同的凭证。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TclPatVO {
    private Long id;
    private String token;
    private String tokenPrefix;
    private String name;
    private String clientId;
    private String scope;
    private Long expiresIn;
    private String expireTime;
    private PatPayloadDTO payload;
    private String createdAt;
}
