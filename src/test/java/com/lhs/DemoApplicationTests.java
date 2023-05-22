package com.lhs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.util.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.entity.survey.SurveyDataChar;
import com.lhs.entity.survey.SurveyDataCharVo;
import com.lhs.entity.survey.SurveyStatisticsChar;
import com.lhs.mapper.SurveyMapper;
import com.lhs.service.*;
import com.lhs.service.dto.MaaOperBoxVo;
import com.lhs.service.dto.SurveyStatisticsCharVo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;


@SpringBootTest
class DemoApplicationTests {


    @Resource
    private StageService stageService;

    @Resource
    private SurveyService surveyService;

    @Resource
    private SurveyMapper surveyMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    private final String read = FileUtil.read("C:\\Users\\李会山\\Desktop\\maa.json");
    private final MaaOperBoxVo maaOperBoxVo = JSONObject.parseObject(read, MaaOperBoxVo.class);





    @Test
    void secretTest() {
        String SECRET = ConfigUtil.Secret;
        String ip = "117.11.47.234";
        System.out.println(SECRET.length());
        System.out.println(AES.encrypt(ip, SECRET));
        System.out.println( AES.decrypt("FiMY9b2S+RPcNLEt6zM+uhnLR0aZ6YRtT5xfQbv6/xxLUHWZc7J5OZ3PthmjR2tq",SECRET));

    }





    @Test
    void readCharId() {
        String character_tableStr = FileUtil.read("E:\\BOT_img\\botResource\\Arknights-Bot-Resource\\gamedata\\excel\\character_table.json");
        String uniequip_tableStr = FileUtil.read("E:\\BOT_img\\botResource\\Arknights-Bot-Resource\\gamedata\\excel\\uniequip_table.json");

        JSONObject character_table = JSONObject.parseObject(character_tableStr);
        JSONObject uniequip_table = JSONObject.parseObject(uniequip_tableStr);
        JSONObject equipDict = JSONObject.parseObject(uniequip_table.getString("equipDict"));
        HashMap<String, HashMap<String, Boolean>> modTable = new HashMap<>();

        equipDict.forEach((k, v) -> {

            JSONObject modJson = JSONObject.parseObject(String.valueOf(v));
            String typeName1 = modJson.getString("typeName1");
            if (!typeName1.equals("ORIGINAL")) {
                String typeName2 = modJson.getString("typeName2");

                String charId = modJson.getString("charId");
                if (modTable.get(charId) != null) {
                    HashMap<String, Boolean> hashMap = modTable.get(charId);
                    hashMap.put("mod" + typeName2, true);
                    modTable.put(charId,hashMap);
                } else {
                    HashMap<String, Boolean> hashMap = new HashMap<>();
                    hashMap.put("mod" + typeName2, true);
                    modTable.put(charId,hashMap);
                }
            }
        });

        HashMap<Object, Object> hashMap = new HashMap<>();
        character_table.forEach((k, v) -> {
            if (k.startsWith("char")) {
                HashMap<Object, Object> character = new HashMap<>();
                JSONObject characterJson = JSONObject.parseObject(String.valueOf(v));
                character.put("name", characterJson.getString("name"));
                character.put("rarity", Integer.parseInt(characterJson.getString("rarity")) + 1);
                character.put("mod",modTable.get(k));
                hashMap.put(k, character);
            }
        });


        FileUtil.save(ConfigUtil.Item, "character_table.json", JSON.toJSONString(hashMap));
        FileUtil.save("E:\\VCProject\\frontend-v2-plus\\src\\static\\json\\survey\\", "character_table.json", JSON.toJSONString(hashMap));
    }

    @Test
    void uploadTest() {
        String read1 = FileUtil.read(ConfigUtil.Item + "character_table.json");
        JSONObject jsonObject = JSONObject.parseObject(read1);

        for (int i = 0; i < 1; i++) {
            String ip = "127.0.0.1" + i;
            String name = "山桜X" + i;
            HashMap<Object, Object> register = surveyService.register(ip, name);
            Object uid = register.get("id");
            List<SurveyDataChar> surveyDataCharList = new ArrayList<>();
            jsonObject.forEach((k, v) -> {

                JSONObject charInfo = JSONObject.parseObject(String.valueOf(v));
                int random = new Random().nextInt(11);
                SurveyDataChar surveyDataChar = SurveyDataChar.builder()
                        .charId(k)
                        .rarity(Integer.parseInt(charInfo.getString("rarity")))
                        .phase(random < 4 ? 1 : 2)
                        .potential(1)
                        .skill1(random)
                        .skill2(random)
                        .skill3(random)
                        .modX(0)
                        .modY(0)
                        .uid(Long.parseLong(String.valueOf(uid)))
                        .build();
                surveyDataCharList.add(surveyDataChar);
            });
            surveyService.uploadCharForm(name, surveyDataCharList);
        }
    }

    @Test
    void survey11() {
        List<Long> userIds = surveyMapper.selectSurveyUserIds();
        List<List<Long>> userIdsGroup = new ArrayList<>();

        int length = userIds.size();

        // 计算可以分成多少组
        int num = length / 500 + 1;
        int fromIndex = 0;
        int toIndex = 500;
        for (int i = 0; i < num; i++) {
            toIndex = Math.min(toIndex, userIds.size());
//            System.out.println("fromIndex:"+fromIndex+"---toIndex:"+toIndex);
            userIdsGroup.add(userIds.subList(fromIndex, toIndex));
            fromIndex += 500;
            toIndex += 500;
        }


        surveyMapper.truncateCharStatisticsTable();


        HashMap<String, SurveyStatisticsCharVo> hashMap = new HashMap<>();
        List<SurveyStatisticsChar> surveyDataCharList = new ArrayList<>();

        for (int i = 0; i < userIdsGroup.size(); i++) {
            List<SurveyDataCharVo> surveyDataCharList_DB =
                    surveyMapper.selectSurveyDataCharVoByUidList("survey_data_char_1", userIdsGroup.get(i));

//            log.info("本次统计数量：" + surveyDataCharList_DB.size());
            System.out.println("本次统计数量：" + surveyDataCharList_DB.size());

            Map<String, List<SurveyDataCharVo>> collectByCharId = surveyDataCharList_DB.stream()
                    .collect(Collectors.groupingBy(SurveyDataCharVo::getCharId));
            collectByCharId.forEach((k, list) -> {
                int own = list.size();
                String charId = list.get(0).getCharId();
                int rarity = list.get(0).getRarity();

                Map<Integer, Long> collectByPhases = list.stream()
                        .collect(Collectors.groupingBy(SurveyDataCharVo::getPhase, Collectors.counting()));
                //该干员精英等级一的数量
                int phases1 = collectByPhases.get(1) == null ? 0 : collectByPhases.get(1).intValue();
                //该干员精英等级二的数量
                int phases2 = collectByPhases.get(2) == null ? 0 : collectByPhases.get(2).intValue();

                //根据该干员的潜能等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectByPotential = list.stream()
                        .collect(Collectors.groupingBy(SurveyDataCharVo::getPotential, Collectors.counting()));
                Map<Integer, Long> collectBySkill1 = list.stream()
                        .collect(Collectors.groupingBy(SurveyDataCharVo::getSkill1, Collectors.counting()));
                Map<Integer, Long> collectBySkill2 = list.stream()
                        .collect(Collectors.groupingBy(SurveyDataCharVo::getSkill2, Collectors.counting()));
                Map<Integer, Long> collectBySkill3 = list.stream()
                        .collect(Collectors.groupingBy(SurveyDataCharVo::getSkill3, Collectors.counting()));
                Map<Integer, Long> collectByModX = list.stream()
                        .collect(Collectors.groupingBy(SurveyDataCharVo::getModX, Collectors.counting()));
                Map<Integer, Long> collectByModY = list.stream()
                        .collect(Collectors.groupingBy(SurveyDataCharVo::getModY, Collectors.counting()));

                if (hashMap.get(charId) != null) {
                    SurveyStatisticsCharVo lastData = hashMap.get(charId);

                    own += lastData.getOwn();
                    phases2 += lastData.getPhase2();

                    lastData.getPotential()
                            .forEach((potential, v) -> collectByPotential.merge(potential, v, Long::sum));
                    lastData.getSkill1()
                            .forEach((potential, v) -> collectBySkill1.merge(potential, v, Long::sum));
                    lastData.getSkill2()
                            .forEach((potential, v) -> collectBySkill2.merge(potential, v, Long::sum));
                    lastData.getSkill3()
                            .forEach((potential, v) -> collectBySkill3.merge(potential, v, Long::sum));
                    lastData.getModX()
                            .forEach((potential, v) -> collectByModX.merge(potential, v, Long::sum));
                    lastData.getModY()
                            .forEach((potential, v) -> collectByModY.merge(potential, v, Long::sum));


                }

                SurveyStatisticsCharVo build = SurveyStatisticsCharVo.builder()
                        .charId(charId)
                        .own(own)
                        .phase2(phases2)
                        .rarity(rarity)
                        .skill1(collectBySkill1)
                        .skill2(collectBySkill2)
                        .skill3(collectBySkill3)
                        .potential(collectByPotential)
                        .modX(collectByModX)
                        .modY(collectByModY)
                        .build();

                hashMap.put(charId, build);
            });
        }

        hashMap.forEach((k, v) -> {
            SurveyStatisticsChar build = SurveyStatisticsChar.builder()
                    .charId(v.getCharId())
                    .rarity(v.getRarity())
                    .own(v.getOwn())
                    .phase2(v.getPhase2())
                    .skill1(JSON.toJSONString(v.getPotential()))
                    .skill2(JSON.toJSONString(v.getSkill2()))
                    .skill3(JSON.toJSONString(v.getSkill3()))
                    .modX(JSON.toJSONString(v.getModX()))
                    .modY(JSON.toJSONString(v.getModY()))
                    .potential(JSON.toJSONString(v.getPotential()))
                    .build();
            surveyDataCharList.add(build);
        });
        surveyMapper.insertBatchCharStatistics(surveyDataCharList);
    }


    @Test
    void randomId() {
        for (int i = 0; i < 9999; i++) {
            int random = new Random().nextInt(9999);
            String end4 = String.format("%04d", random);
            System.out.println(end4);
        }
    }


    @Test
    void getToken(){
        Object o = redisTemplate.opsForValue().get("KZkR3/roGVuCdbbHD3gGLGtH7BmO29IZd7yEc+uEmjY=");
        System.out.println(o);
    }

}
