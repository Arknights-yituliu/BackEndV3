package com.lhs.entity.survey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SurveyUser {
    private Long id;   //唯一id （自增
    private String passWord;
    private String uid;
    private String userName;  //用户id(用户填写的昵称后加#xxxx
    private Date createTime;  //创建时间
    private Date updateTime;  //最后一次上传数据时间
    private String ip;   //ip地址
    private Integer status;  //用户状态，1正常，0封禁
}
