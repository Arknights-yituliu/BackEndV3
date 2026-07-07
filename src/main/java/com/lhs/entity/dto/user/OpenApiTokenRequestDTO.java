package com.lhs.entity.dto.user;

import lombok.Data;

import java.util.List;

/**
 * 第三方 API Token 生成请求 DTO
 */
@Data
public class OpenApiTokenRequestDTO {

    /**
     * 权限 code 数组，如 [10001] 或 [10001, 10002]
     */
    private List<Integer> scope;

    /**
     * 备注
     */
    private String remark;
}
