package com.lhs.service.survey;

import com.lhs.common.util.Result;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.survey.SurveyUser;
import com.lhs.service.dev.OSSService;
import org.springframework.stereotype.Service;

@Service
public class OperatorPlanService {


    private final OSSService ossService;
    private final SurveyUserService surveyUserService;

    public OperatorPlanService(OSSService ossService, SurveyUserService surveyUserService) {
        this.ossService = ossService;
        this.surveyUserService = surveyUserService;
    }

    public Result<Object> savePlan(String token, String planText){
        SurveyUser surveyUserByToken = surveyUserService.getSurveyUserByToken(token);
        Long id = surveyUserByToken.getId();

        Boolean upload = ossService.upload(planText, "survey/operator/plan/" + id + ".json");

        if(upload){
            return Result.success("保存成功");
        }else{
            return Result.failure(ResultCode.OSS_UPLOAD_ERROR);
        }

    }

}
