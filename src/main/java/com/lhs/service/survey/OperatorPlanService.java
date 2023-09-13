package com.lhs.service.survey;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Result;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.survey.OperatorPlan;
import com.lhs.entity.survey.OperatorTable;
import com.lhs.entity.survey.SurveyUser;
import com.lhs.mapper.survey.OperatorTableMapper;
import com.lhs.service.dev.OSSService;
import com.lhs.vo.survey.OperatorPlanVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OperatorPlanService {


    private final OSSService ossService;
    private final SurveyUserService surveyUserService;
    private final OperatorTableMapper operatorTableMapper;

    public OperatorPlanService(OSSService ossService, SurveyUserService surveyUserService, OperatorTableMapper operatorTableMapper) {
        this.ossService = ossService;
        this.surveyUserService = surveyUserService;
        this.operatorTableMapper = operatorTableMapper;
    }

    public Result<Object> savePlan(OperatorPlanVo operatorPlanVo){

        SurveyUser surveyUserByToken = surveyUserService.getSurveyUserByToken(operatorPlanVo.getToken());
        String userName = surveyUserByToken.getUserName();

        List<OperatorPlan> operatorPlanList = operatorPlanVo.getOperatorPlanList();

        List<OperatorTable> operatorTable = operatorTableMapper.selectList(null);
        Map<String, String> collect = operatorTable.stream().
                collect(Collectors.toMap(OperatorTable::getCharId, OperatorTable::getName));

        for(OperatorPlan operatorPlan:operatorPlanList){
            String name = collect.get(operatorPlan.getCharId())==null?collect.get(operatorPlan.getCharId()): "未更新干员";
            operatorPlan.setName(name);
        }

        String result = JsonMapper.toJSONString(operatorPlanList);


        Boolean upload = ossService.upload(result, "survey/operator/plan/" + userName + ".json");

        if(upload){
            return Result.success("保存成功");
        }else{
            return Result.failure(ResultCode.OSS_UPLOAD_ERROR);
        }
    }

    public Result<List<OperatorPlan>> getPlan(OperatorPlanVo operatorPlanVo){
        String token = operatorPlanVo.getToken();
        SurveyUser surveyUserByToken = surveyUserService.getSurveyUserByToken(token);
        String userName = surveyUserByToken.getUserName();
        String read = ossService.read("survey/operator/plan/" + userName + ".json");
        List<OperatorPlan> operatorPlans = JsonMapper.parseJSONArray(read, new TypeReference<List<OperatorPlan>>() {
        });
        return Result.success(operatorPlans);

    }

}
