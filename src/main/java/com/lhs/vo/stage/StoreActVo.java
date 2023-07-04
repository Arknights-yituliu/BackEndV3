package com.lhs.vo.stage;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "活动商店性价比返回对象")
public class StoreActVo {
    @ApiModelProperty(value = "结束日期")
    private String actEndDate;

    @ApiModelProperty(value = "商店提示内容")
    private List<ActTagArea> actTagArea;

    @ApiModelProperty(value = "商店头图链接")
    private String actImgUrl;

    @ApiModelProperty(value = "商店性价比基准")
    private Double actPPRBase;

    @ApiModelProperty(value = "商店性价比阶梯")
    private Double actPPRStair;

    @ApiModelProperty(value = "活动名称")
    private String actName;

    @ApiModelProperty(value = "商店售卖物品")
    private List<StoreItem> actStore;
}