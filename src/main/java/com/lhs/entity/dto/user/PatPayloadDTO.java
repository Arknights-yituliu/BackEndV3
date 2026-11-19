package com.lhs.entity.dto.user;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** PAT 的账号快照；source 仅作展示，不参与取数或鉴权。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatPayloadDTO {
    @JsonProperty("game_uid")
    @JsonAlias("gameUid")
    private String gameUid;

    @JsonProperty("game_name")
    @JsonAlias("gameName")
    private String gameName;

    private String source;
}
