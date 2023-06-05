package com.lhs;

import com.alibaba.fastjson.JSONObject;
import com.lhs.common.util.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.entity.survey.SurveyScore;
import com.lhs.service.SurveyScoreService;
import com.lhs.service.SurveyUserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


@SpringBootTest
public class SurveyScoreTest {

    @Resource
    private SurveyScoreService surveyScoreService;

    @Resource
    private SurveyUserService surveyUserService;

    @Test
    void upload(){
        String charTable = FileUtil.read(ConfigUtil.Item + "character_table.json");

        for (int i = 0; i < 10000; i++) {

            HashMap<Object, Object> register = surveyUserService.register("ipAddress" + i, "山桜" + i);
            String userName = (String) register.get("userName");
            JSONObject jsonObject = JSONObject.parseObject(charTable);
            List<SurveyScore> surveyScoreList = new ArrayList<>();
            jsonObject.forEach((k,v)->{
                int rarity = Integer.parseInt(JSONObject.parseObject(String.valueOf(v)).getString("rarity"));
                SurveyScore surveyScore = SurveyScore.builder()
                        .charId(k)
                        .rarity(rarity+1)
                        .daily(getRandom())
                        .rogue(getRandom())
                        .securityService(getRandom())
                        .hard(getRandom())
                        .universal(getRandom())
                        .countermeasures(getRandom()).build();
                if (rarity==5){
                    surveyScoreList.add(surveyScore);
                }

            });

            HashMap<Object, Object> hashMap = surveyScoreService.uploadScoreForm(userName, surveyScoreList);
        }


    }

    @Test
    void isV(){
        SurveyScore newData = SurveyScore.builder().daily(0).hard(1).rogue(1).securityService(1).universal(1).countermeasures(1).build();

        Boolean isInvalid = (newData.getDaily() < 1 || newData.getDaily() > 10) ||
                (newData.getRogue() < 1 || newData.getRogue() > 10) ||
                (newData.getHard() < 1 || newData.getHard() > 10) ||
                (newData.getSecurityService() < 1 || newData.getSecurityService() > 10)||
                (newData.getUniversal() < 1 || newData.getUniversal() > 10)||
                (newData.getCountermeasures()< 1 || newData.getCountermeasures() > 10);
        
        System.out.println(!isInvalid);
    }

    private Integer getRandom(){
         return new Random().nextInt(10)-1;
    }

    @Test
    void getTableName(){
        System.out.println(surveyUserService.getTableIndex(20001L));
    }


}
