package com.lhs.task;

import com.lhs.service.survey.OperatorStatisticsService;
import org.springframework.stereotype.Service;

@Service
public class SurveyTask {



    private final OperatorStatisticsService operatorStatisticsService;

    public SurveyTask(OperatorStatisticsService operatorStatisticsService) {
        this.operatorStatisticsService = operatorStatisticsService;
    }

    //    @Scheduled(cron = "0 5 0/1 * * ? ")
    public void operatorStatistics(){
        operatorStatisticsService.operatorStatistics();
    }


}
