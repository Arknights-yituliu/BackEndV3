package com.lhs.entity.dto.user;

import java.util.List;

/**
 * 第三方API Token Redis存储数据结构
 */
public class OpenApiTokenDataDTO {

    private Long uid;
    private List<Integer> scope;

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public List<Integer> getScope() {
        return scope;
    }

    public void setScope(List<Integer> scope) {
        this.scope = scope;
    }
}
