package com.lhs.entity.survey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class SurveyStatisticsUser {

    @Id
    private Long id;   //唯一id （自增
    private String passWord;
    private String uid;
    private String userName;  //用户id(用户填写的昵称后加#xxxx
    private Date createTime;  //创建时间
    private Integer status;
    private String email;
    private int operatorCount;
}
