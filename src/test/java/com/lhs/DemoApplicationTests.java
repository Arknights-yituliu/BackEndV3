package com.lhs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.FileConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.entity.*;
import com.lhs.mapper.OperatorDataMapper;
import com.lhs.mapper.SurveyMapper;
import com.lhs.service.*;
import com.lhs.service.dto.MaaOperBoxVo;
import com.lhs.service.dto.SurveyStatisticsCharVo;
import com.lhs.service.vo.OperatorStatisticsVo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;


@SpringBootTest
class DemoApplicationTests {


    @Resource
    private StageService stageService;
    @Resource
    private OperatorDataMapper operatorDataMapper;
    @Resource
    private SurveyService surveyService;

    @Resource
    private SurveyMapper surveyMapper;


    @Test
    void readGameData() {
        stageService.readGameData_stageFile();
    }

    private final String read = FileUtil.read("C:\\Users\\李会山\\Desktop\\maa.json");
    private final MaaOperBoxVo maaOperBoxVo = JSONObject.parseObject(read, MaaOperBoxVo.class);


    @Test
    void statisticsTest() {

        List<OperatorStatistics> operatorStatisticsList = operatorDataMapper.selectStatisticsList();
        OperatorStatisticsConfig config1 = operatorDataMapper.selectConfigByKey("user_count");
        double userCount = Double.parseDouble(config1.getConfigValue());

        List<OperatorStatisticsVo> statisticsVoResultList = new ArrayList<>();

        DecimalFormat df = new DecimalFormat("0.00");

        for (OperatorStatistics statistics : operatorStatisticsList) {
            int i = new Random().nextInt(5000);

            JSONObject jsonObject = JSONObject.parseObject(statistics.getPotentialRanks());
            Map<Integer, Long> potentialRanks = new HashMap<>();
            jsonObject.forEach((p, v) -> potentialRanks.put(Integer.parseInt(p), Long.parseLong(String.valueOf(v))));

            Map<Integer, Double> potentialRanksResult = new HashMap<>();
            potentialRanks.forEach((k, v) -> {
                potentialRanksResult.put(k, Double.parseDouble(df.format((v / userCount * 100))));
            });

            JSONObject result = JSONObject.parseObject(JSON.toJSONString(potentialRanksResult));

            OperatorStatisticsVo operatorStatisticsVo = OperatorStatisticsVo.builder()
                    .charId(statistics.getCharId())
                    .charName(statistics.getCharName())
                    .rarity(statistics.getRarity())
                    .owningRate(Double.parseDouble(df.format((statistics.getHoldings() - i) / userCount * 100)))
                    .phases1Rate(Double.parseDouble(df.format(statistics.getPhases1() / userCount * 100)))
                    .phases2Rate(Double.parseDouble(df.format(statistics.getPhases2() / userCount * 100)))
                    .potentialRanks(result)
                    .build();
            statisticsVoResultList.add(operatorStatisticsVo);
        }

        statisticsVoResultList = statisticsVoResultList.stream().filter(e -> e.getRarity() > 5).collect(Collectors.toList());

        statisticsVoResultList.sort(Comparator.comparing(OperatorStatisticsVo::getOwningRate).reversed());
        System.out.println(JSON.toJSONString(statisticsVoResultList));

    }


    @Test
    void secretTest() {
        String SECRET = FileConfig.Secret;
        String ip = "117.11.47.234";
        System.out.println(AES.encrypt(ip, SECRET));
        System.out.println(AES.encrypt(ip, SECRET));
    }


    @Test
    void group() {
        List<Long> userIds = operatorDataMapper.selectIdsByPage();

        List<List<Long>> userIdsGroup = new ArrayList<>();

        int length = userIds.size();
        // 计算可以分成多少组
        int num = length / 400 + 1;
        int start = 0;
        int end = 400;
        for (int i = 0; i < num; i++) {
            end = Math.min(end, userIds.size());
            System.out.println("start:" + start + "---end:" + end);
            userIdsGroup.add(userIds.subList(start, end));
            start += 400;
            end += 400;
        }

        for (List<Long> list : userIdsGroup) {
            System.out.println(list.get(0));
            System.out.println(list.get(list.size() - 1));
        }

    }


    @Test
    void readCharId() {
        String read1 = FileUtil.read("E:\\BOT_img\\botResource\\Arknights-Bot-Resource\\gamedata\\excel\\character_table.json");
        JSONObject jsonObject = JSONObject.parseObject(read1);
        HashMap<Object, Object> hashMap = new HashMap<>();
        jsonObject.forEach((k, v) -> {
            if (k.startsWith("char")) {
                HashMap<Object, Object> character = new HashMap<>();
                JSONObject characterJson = JSONObject.parseObject(String.valueOf(v));
                character.put("name", characterJson.getString("name"));
                character.put("rarity", characterJson.getString("rarity"));
                hashMap.put(k, character);
            }
        });
        FileUtil.save(FileConfig.Item, "character_table.json", JSON.toJSONString(hashMap));
    }

    @Test
    void uploadTest() {
        String read1 = FileUtil.read(FileConfig.Item + "character_table.json");
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
                    surveyMapper.selectSurveyDataCharByUidList("survey_data_char_1", userIdsGroup.get(i));

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

                if(hashMap.get(charId)!=null){
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

                hashMap.put(charId,build);
            });
        }

        hashMap.forEach((k,v)->{
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

}
