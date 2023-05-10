package com.lhs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.FileConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.entity.OperatorStatistics;
import com.lhs.entity.OperatorStatisticsConfig;
import com.lhs.mapper.OperatorDataMapper;
import com.lhs.mapper.MaaUserMapper;
import com.lhs.service.*;
import com.lhs.service.dto.MaaOperBoxVo;
import com.lhs.service.dto.OperBox;
import com.lhs.service.vo.OperatorStatisticsVo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import javax.xml.crypto.Data;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@SpringBootTest
class DemoApplicationTests {


    @Resource
    private StageService stageService;
    @Resource
    private OperatorDataMapper operatorDataMapper;
    @Resource
    private MaaUserMapper maaUserMapper;
    @Resource
    private OperatorSurveyService operatorSurveyService;

    @Test
    void readGameData() {
        stageService.readGameData_stageFile();
    }

    private final String read = FileUtil.read("C:\\Users\\李会山\\Desktop\\maa.json");
    private final MaaOperBoxVo maaOperBoxVo = JSONObject.parseObject(read, MaaOperBoxVo.class);

    @Test
    void uploadData() {
        for (int i = 0; i < 20000; i++) {
            operatorSurveyService.saveMaaOperatorBoxData(maaOperBoxVo, "1k2j8x4g1n"+i);
        }
    }



    @Test
    void testMaaJson() {
        String read = FileUtil.read("src/main/resources/operBox.json");
        JSONArray own_opers = JSONArray.parseArray(JSONObject.parseObject(read).getString("own_opers"));

        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("uuid", "11131231");
        hashMap.put("operBox", own_opers);
        hashMap.put("server", "server");
        hashMap.put("source", "source");
        hashMap.put("version", "version");
        System.out.println(JSON.toJSONString(hashMap));
    }


    @Test
    void endTime(){
        long end =  1682679361900L;

        for (int i = 0; i < 100; i++) {
            end+=100;
            System.out.println(end);
        }
    }



    @Test
    void statisticsTest() {

        List<OperatorStatistics> operatorStatisticsList = operatorDataMapper.selectStatisticsList();
        OperatorStatisticsConfig config1 = operatorDataMapper.selectConfigByKey("user_count");
        double userCount = Double.parseDouble(config1.getConfigValue());

        List<OperatorStatisticsVo> statisticsVoResultList = new ArrayList<>();

        DecimalFormat df=new DecimalFormat("0.00");

        for(OperatorStatistics statistics:operatorStatisticsList){
            int i = new Random().nextInt(5000);

            JSONObject jsonObject = JSONObject.parseObject(statistics.getPotentialRanks());
            Map<Integer, Long> potentialRanks = new HashMap<>();
            jsonObject.forEach((p, v) -> potentialRanks.put(Integer.parseInt(p), Long.parseLong(String.valueOf(v))));

            Map<Integer, Double> potentialRanksResult = new HashMap<>();
            potentialRanks.forEach((k,v)->{
                potentialRanksResult.put(k,Double.parseDouble(df.format((v/userCount*100))));
            });

            JSONObject result = JSONObject.parseObject(JSON.toJSONString(potentialRanksResult));

            OperatorStatisticsVo operatorStatisticsVo = OperatorStatisticsVo.builder()
                    .charId(statistics.getCharId())
                    .charName(statistics.getCharName())
                    .rarity(statistics.getRarity())
                    .owningRate(Double.parseDouble(df.format((statistics.getHoldings()-i)/userCount*100)))
                    .phases1Rate(Double.parseDouble(df.format(statistics.getPhases1()/userCount*100)))
                    .phases2Rate(Double.parseDouble(df.format(statistics.getPhases2()/userCount*100)))
                    .potentialRanks(result)
                    .build();
              statisticsVoResultList.add(operatorStatisticsVo);
        }

        statisticsVoResultList = statisticsVoResultList.stream().filter(e->e.getRarity()>5).collect(Collectors.toList());

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
    void equalsTest(){
        OperBox operBox1 = new OperBox();
        operBox1.setElite(2);
        operBox1.setLevel(30);
        operBox1.setPotential(1);
        OperBox operBox2 = new OperBox();
        operBox2.setElite(2);
        operBox2.setLevel(30);
        operBox2.setPotential(1);

        System.out.println(Objects.equals(operBox1.getElite(), operBox2.getElite()));
        System.out.println(Objects.equals(operBox1.getLevel(), operBox2.getLevel()));
        System.out.println(Objects.equals(operBox1.getPotential(), operBox2.getPotential()));
    }

    @Test
    void group(){
        List<Long> userIds = operatorDataMapper.selectIdsByPage();

        List<List<Long>> userIdsGroup = new ArrayList<>();

        int length = userIds.size();
        // 计算可以分成多少组
        int num = length/400+1;
        int start = 0;
        int end = 400;
        for (int i = 0; i < num; i++) {
            end = Math.min(end, userIds.size());
            System.out.println("start:"+start+"---end:"+end);
            userIdsGroup.add(userIds.subList(start,end));
            start+=400;
            end+=400;
        }

        for(List<Long> list :userIdsGroup){
            System.out.println(list.get(0));
            System.out.println(list.get(list.size()-1));
        }

    }

    @Test
    void idTest(){
        Date date = new Date();
        HashMap<Long, Long> hashMap = new HashMap<>();
        int count = 0;
        for (int i = 0; i < 100; i++) {


            int end1 = new Random().nextInt(999);
            long time = new Date().getTime();
            long id = time*1000+end1;
            if(hashMap.get(id)!=null) count++;
            System.out.println(id);
            hashMap.put(id,id);
        }
        System.out.println(count);
    }


    @Test
    void readCharId(){
        String read1 = FileUtil.read("E:\\BOT_img\\botResource\\Arknights-Bot-Resource\\gamedata\\excel\\character_table.json");
        JSONObject jsonObject = JSONObject.parseObject(read1);
        HashMap<Object, Object> hashMap = new HashMap<>();
        jsonObject.forEach((k,v)->{
            if(k.startsWith("char")){
            HashMap<Object, Object> character = new HashMap<>();
            JSONObject characterJson = JSONObject.parseObject(String.valueOf(v));
            character.put("name",characterJson.getString("name"));
            character.put("rarity",characterJson.getString("rarity"));
            hashMap.put(k,character);
            }
        });
        FileUtil.save(FileConfig.Item,"character_table.json",JSON.toJSONString(hashMap));
    }


}
