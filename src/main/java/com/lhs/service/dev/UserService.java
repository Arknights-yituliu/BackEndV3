package com.lhs.service.dev;


import com.lhs.entity.vo.dev.LoginVo;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService {

    Boolean developerLevel(HttpServletRequest request);

    void emailSendCode(LoginVo loginVo);

    String login(LoginVo loginVo);

    Boolean loginAndCheckToken(String token);

}
