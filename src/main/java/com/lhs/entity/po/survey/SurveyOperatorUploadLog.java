package com.lhs.entity.po.survey;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@TableName
@Table
@Entity
public class SurveyOperatorUploadLog {

    @Id
    @TableId
    private Long id;
    private String userName;
    private String uid;
    private String ip;   //ip地址
    private Long lastTime;  //创建时间
    private boolean deleteFlag;

}
