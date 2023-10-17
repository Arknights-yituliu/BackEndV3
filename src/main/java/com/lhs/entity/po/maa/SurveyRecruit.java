package com.lhs.entity.po.maa;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SurveyRecruit {

    @TableId
    private Long id;
    private String uid;  //企鹅物流id
    private String tag;  //tag组合，是个集合
    private Integer level;  //本次招募的最高星级
    private Long createTime;  //创建时间
    private String server;  //地区 一般为CN
    private String source; //来源
    private String version;  //maa版本号

}
