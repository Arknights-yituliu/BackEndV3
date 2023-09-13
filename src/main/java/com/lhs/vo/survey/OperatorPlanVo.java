package com.lhs.vo.survey;

import com.lhs.entity.survey.OperatorPlan;
import lombok.Data;

import java.util.List;

@Data
public class OperatorPlanVo {
    private String token;
    private List<OperatorPlan>  operatorPlanList;

}
