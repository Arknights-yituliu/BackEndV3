package com.lhs.vo.survey;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyStatisticsScoreVo {

    @ApiModelProperty(value = "干员id")
    private String  charId;

    @ApiModelProperty(value = "干员星级")
    private Integer rarity;

    @ApiModelProperty(value = "日常")
    private Double daily;

    @ApiModelProperty(value = "样本量-日常")
    private Integer sampleSizeDaily;

    @ApiModelProperty(value = "肉鸽")
    private Double rogue;

    @ApiModelProperty(value = "样本量-肉鸽")
    private Integer sampleSizeRogue;

    @ApiModelProperty(value = "保全")
    private Double securityService;

    @ApiModelProperty(value = "样本量-保全")
    private Integer sampleSizeSecurityService;

    @ApiModelProperty(value = "高难度")
    private Double hard;

    @ApiModelProperty(value = "样本量-高难度")
    private Integer sampleSizeHard;

    @ApiModelProperty(value = "泛用")
    private Double universal;

    @ApiModelProperty(value = "样本量-泛用")
    private Integer sampleSizeUniversal;

    @ApiModelProperty(value = "对策")
    private Double countermeasures;

    @ApiModelProperty(value = "样本量-对策")
    private Integer sampleSizeCountermeasures;


}
