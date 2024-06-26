package com.lhs.service.admin;


import com.lhs.entity.vo.dev.LoginVo;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

public interface AdminService {

    Boolean developerLevel(HttpServletRequest request);

    void emailSendCode(LoginVo loginVo);

    Map<String,Object> login(LoginVo loginVo);

    Boolean checkToken(String token);

    HashMap<String,Object> getDeveloperInfo(String token);

}
