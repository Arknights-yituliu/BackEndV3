package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName
public class SurveyUser {
    @TableId
    private Long id;   //唯一id （自增
    private String userName;  //用户名称
    private String password; //密码
    private String email; //邮箱
    private Date createTime;  //创建时间
    private Date updateTime;  //最后一次上传数据时间
    private String ip;   //ip地址
    private Integer status;  //用户状态，1正常，0封禁
    private String avatar; //用户头像
    private Boolean deleteFlag; //删除标记
}
