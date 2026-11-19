package com.lhs.entity.dto.user;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** 本站 PAT 签发入参，refresh_token 只由服务端缓存提供。 */
@Data
public class PatIssueRequestDTO {
    private String name;

    @JsonProperty("expires_in")
    @JsonAlias("expiresIn")
    private Long expiresIn;

    private String scope;
    private PatPayloadDTO payload;
}
