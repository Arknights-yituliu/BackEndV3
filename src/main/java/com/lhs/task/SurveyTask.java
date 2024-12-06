package com.lhs.task;

import com.lhs.service.survey.OperatorStatisticsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SurveyTask {



    private final OperatorStatisticsService operatorStatisticsService;

    public SurveyTask(OperatorStatisticsService operatorStatisticsService) {
        this.operatorStatisticsService = operatorStatisticsService;
    }




}
