package com.lhs.service.survey;


import jakarta.servlet.http.HttpServletRequest;

public interface QuestionnaireService {
     void uploadQuestionnaireResult(HttpServletRequest httpServletRequest, String requestContent);
}
