package com.lhs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lhs.common.util.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.entity.survey.SurveyEvaluation;
import com.lhs.service.SurveyEvaluationService;
import com.lhs.service.SurveyUserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


@SpringBootTest
public class SurveyEvaluationTest {

    @Resource
    private SurveyEvaluationService surveyEvaluationService;

    @Resource
    private SurveyUserService surveyUserService;

    @Test
    void upload(){
        String charTable = FileUtil.read(ConfigUtil.Item + "character_table.json");

        for (int i = 0; i < 10000; i++) {

            HashMap<Object, Object> register = surveyUserService.register("ipAddress" + i, "山桜" + i);
            String userName = (String) register.get("userName");
            JSONObject jsonObject = JSONObject.parseObject(charTable);
            List<SurveyEvaluation> surveyEvaluationList = new ArrayList<>();
            jsonObject.forEach((k,v)->{
                int rarity = Integer.parseInt(JSONObject.parseObject(String.valueOf(v)).getString("rarity"));
                SurveyEvaluation surveyEvaluation = SurveyEvaluation.builder()
                        .charId(k)
                        .rarity(rarity+1)
                        .daily(getRandom())
                        .rogue(getRandom())
                        .securityService(getRandom())
                        .hard(getRandom())
                        .universal(getRandom())
                        .countermeasures(getRandom()).build();
                if (rarity==5){
                    surveyEvaluationList.add(surveyEvaluation);
                }

            });

            HashMap<Object, Object> hashMap = surveyEvaluationService.uploadEvaluationForm(userName, surveyEvaluationList);
        }


    }

    private Integer getRandom(){
         return new Random().nextInt(10)-1;
    }

    @Test
    void getTableName(){
        System.out.println(surveyUserService.getTableIndex(20001L));
    }


}
