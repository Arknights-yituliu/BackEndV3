package com.lhs.task;

import com.lhs.service.survey.SurveyOperatorService;
import com.lhs.service.survey.SurveyStatisticsOperatorService;
import com.lhs.service.survey.SurveyUserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SurveyOperatorTask {

    private final SurveyUserService surveyUserService;

    private final SurveyStatisticsOperatorService surveyStatisticsOperatorService;

    public SurveyOperatorTask(SurveyUserService surveyUserService, SurveyStatisticsOperatorService surveyStatisticsOperatorService) {
        this.surveyUserService = surveyUserService;
        this.surveyStatisticsOperatorService = surveyStatisticsOperatorService;
    }



    @Scheduled(cron = "0 5 0/1 * * ? ")
    public void operatorStatistics(){
        surveyStatisticsOperatorService.operatorStatistics();
    }


}
