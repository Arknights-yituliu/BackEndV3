package com.lhs.service.survey;


import com.lhs.entity.dto.survey.QuestionnaireSubmitInfoDTO;
import com.lhs.entity.vo.survey.SurveySubmitterVO;
import jakarta.servlet.http.HttpServletRequest;

public interface QuestionnaireService {
     SurveySubmitterVO uploadQuestionnaireResult(HttpServletRequest httpServletRequest, QuestionnaireSubmitInfoDTO questionnaireSubmitInfoDTO);

     void statisticsQuestionnaireResult(int questionnaireId);
}
