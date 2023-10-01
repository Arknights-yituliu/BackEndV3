package com.lhs.entity.vo.survey;

import com.lhs.entity.po.survey.OperatorPlan;
import lombok.Data;

import java.util.List;

@Data
public class OperatorPlanVo {
    private String token;
    private List<OperatorPlan>  operatorPlanList;

}
