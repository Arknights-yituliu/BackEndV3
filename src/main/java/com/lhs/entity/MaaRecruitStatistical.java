package com.lhs.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("maa_recruit_statistical")//maa公招统计结果
public class MaaRecruitStatistical {

    @TableId
    private  Long id;
    private  Integer topOperator ;  //高级资深总数
    private  Integer seniorOperator; //资深总数
    private  Integer topAndSeniorOperator; //高级资深含有资深总数
    private  Integer seniorOperatorCount;  //五星TAG总数
    private  Integer rareOperatorCount;   //四星TAG总数
    private  Integer commonOperatorCount; //三星TAG总数
    private  Integer robot;                //小车TAG总数
    private  Integer robotChoice;       //小车和其他组合共同出现次数
    private  Integer vulcan;             //火神出现次数
    private  Integer gravel;         //砾出现次数
    private  Integer jessica;    //杰西卡次数
    private  Integer maaRecruitDataCount; //总数据量
    private Date lastTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;


}
