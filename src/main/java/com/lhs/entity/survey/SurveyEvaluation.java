package com.lhs.entity.survey;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyEvaluation {
    private String id;
    @ApiModelProperty(value = "用户id")
    private Long uid;

    @ApiModelProperty(value = "干员id")
    private String  charId;

    @ApiModelProperty(value = "干员星级")
    private Integer rarity;

    @ApiModelProperty(value = "日常")
    private Integer daily;

    @ApiModelProperty(value = "肉鸽")
    private Integer rogue;

    @ApiModelProperty(value = "保全")
    private Integer securityService;

    @ApiModelProperty(value = "高难度")
    private Integer hard;

    @ApiModelProperty(value = "泛用")
    private Integer universal;

    @ApiModelProperty(value = "对策")
    private Integer countermeasures;


}
