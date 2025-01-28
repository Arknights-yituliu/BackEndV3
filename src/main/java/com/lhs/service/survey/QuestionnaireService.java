package com.lhs.service.survey;


import com.lhs.entity.dto.survey.QuestionnaireSubmitInfoDTO;
import com.lhs.entity.vo.survey.OperatorCarryRateStatisticsVO;
import com.lhs.entity.vo.survey.SurveySubmitterVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface QuestionnaireService {
     void uploadQuestionnaireResult(HttpServletRequest httpServletRequest, QuestionnaireSubmitInfoDTO questionnaireSubmitInfoDTO);

     void statisticsQuestionnaireResult(int questionnaireId);

     List<OperatorCarryRateStatisticsVO> getQuestionnaireResultByType(Integer questionnaireType);
}
