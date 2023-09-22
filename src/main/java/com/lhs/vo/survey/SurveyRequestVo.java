package com.lhs.vo.survey;

import lombok.Data;


@Data
public class SurveyRequestVo {

    private String userName;//用户名
    private String passWord;//密码
    private String oldPassWord;//密码
    private String newPassWord;//密码
    private String uid;//游戏内的玩家uid
    private String cred;//森空岛cred
    private String token;//用户凭证（校验用户权限身份）
    private String sklandData;//森空岛数据
    private String email; //邮箱
    private String emailCode; //邮箱验证码
    private String nickName; //游戏内的玩家昵称
}
