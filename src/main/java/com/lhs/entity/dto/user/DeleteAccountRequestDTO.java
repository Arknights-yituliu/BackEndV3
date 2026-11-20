package com.lhs.entity.dto.user;

import lombok.Getter;
import lombok.Setter;

/** 注销验证码；用户身份与 refresh_token 均由服务端确定。 */
@Getter
@Setter
public class DeleteAccountRequestDTO {
    private String code;
}
