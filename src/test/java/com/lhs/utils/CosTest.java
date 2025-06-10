package com.lhs.utils;

import com.lhs.service.survey.OperatorDataService;
import com.lhs.service.survey.QuestionnaireService;
import com.lhs.service.user.UserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CosTest {

    @Resource
    private OperatorDataService operatorDataService;

    @Resource
    private QuestionnaireService questionnaireService;

    @Resource
    private UserService userService;

    @Test
    void backupTest() {
//        operatorDataService.backupOperatorProgressionData();
//        questionnaireService.backup();
        userService.backupUserInfo();
        userService.backupUserExternalAccountBinding();
    }



}
