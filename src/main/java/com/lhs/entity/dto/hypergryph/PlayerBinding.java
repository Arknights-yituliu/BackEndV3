package com.lhs.entity.dto.hypergryph;

import lombok.Data;

@Data
public class PlayerBinding {

    private String uid;
    private String channelMasterId;
    private String channelName;
    private String nickName;
    private Boolean defaultFlag;
}
