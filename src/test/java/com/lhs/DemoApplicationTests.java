package com.lhs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.FileConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.entity.OperatorStatistics;
import com.lhs.entity.OperatorDataVo;
import com.lhs.entity.OperatorStatisticsConfig;
import com.lhs.mapper.OperatorDataMapper;
import com.lhs.mapper.MaaUserMapper;
import com.lhs.service.*;
import com.lhs.service.dto.MaaOperBoxVo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
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
    private MaaService maaService;

    @Test
    void readGameData() {
        stageService.readGameData_stageFile();
    }

    private final String read = FileUtil.read("C:\\Users\\李会山\\Desktop\\maa.json");
    private final MaaOperBoxVo maaOperBoxVo = JSONObject.parseObject(read, MaaOperBoxVo.class);

    @Test
    void uploadData() {
        for (int i = 0; i < 20000; i++) {
            maaService.saveMaaOperatorBoxData(maaOperBoxVo, "1321e121e21");
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
    void selectByTimeStamp() {

        //获取上次统计的最后时间戳
        OperatorStatisticsConfig config = operatorDataMapper.selectConfigByKey("lastTime");
        long startTime = Long.parseLong(config.getConfigValue());
        //当前时间戳
        long endTime = new Date().getTime();
        //以上次时间戳做start，当前时间戳作end，查询这个时间段的时间
        List<OperatorDataVo> operator_box_data =
                operatorDataMapper.selectByTimeStamp("operator_data_1", startTime, endTime);

        //根据干员名称分组
        Map<String, List<OperatorDataVo>> collectByCharName = operator_box_data.stream()
                .collect(Collectors.groupingBy(OperatorDataVo::getCharName));

        //如果这个干员没有被统计，需要插入的新统计数据
        List<OperatorStatistics> operatorStatisticsList = new ArrayList<>();

        //循环这个分组后的map
        collectByCharName.forEach((k, list) -> {

            long holdings = list.size();
            //根据精英化等级分类
            Map<Integer, Long> collectByPhases = list.stream()
                    .collect(Collectors.groupingBy(OperatorDataVo::getElite, Collectors.counting()));
            //该干员精英等级一的数量
            long phases1 = collectByPhases.get(1) == null ? 0 : collectByPhases.get(1).intValue();
            //该干员精英等级二的数量
            long phases2 = collectByPhases.get(2) == null ? 0 : collectByPhases.get(2).intValue();
            //根据该干员的潜能等级分组统计  map(潜能等级,该等级的数量)
            Map<Integer, Long> collectByPotential = list.stream().collect(Collectors.groupingBy(OperatorDataVo::getPotential, Collectors.counting()));
            //json化方便存储
            String potentialStr = JSON.toJSONString(collectByPotential);
            //查询是否有这条记录
            OperatorStatistics operatorStatistics = operatorDataMapper.selectStatisticsByCharName(k);

            if (operatorStatistics == null) {
                OperatorStatistics statistics = OperatorStatistics.builder()
                        .charName(k)
                        .holdings(holdings)
                        .phases1(phases1)
                        .phases2(phases2 + 100)
                        .potentialRanks(potentialStr)
                        .build();
                //没有的话塞入集合准备新增
                operatorStatisticsList.add(statistics);
            } else {
                //有记录的话直接更新
                holdings += operatorStatistics.getHoldings();
                phases1 += operatorStatistics.getPhases1();
                phases2 += operatorStatistics.getPhases2();

                //合并该干员上次和本次潜能统计的数据
                JSONObject jsonObject =JSONObject.parseObject(operatorStatistics.getPotentialRanks());
                Map<Integer, Long> potentialRanksNew = new HashMap<>();
                jsonObject.forEach((p,v)->potentialRanksNew.put(Integer.parseInt(p),Long.parseLong(String.valueOf(v))));
                collectByPotential.forEach((potential,v)->potentialRanksNew.merge(potential, v, Long::sum));
                potentialStr = JSON.toJSONString(potentialRanksNew);

                OperatorStatistics statistics = OperatorStatistics.builder()
                        .charName(k)
                        .holdings(holdings)
                        .phases1(phases1)
                        .phases2(phases2 + 100)
                        .potentialRanks(potentialStr)
                        .build();
                operatorDataMapper.updateStatisticsByCharName(statistics);
            }

        });

        //保存本次时间段统计的最后时间
        operatorDataMapper.updateConfigByKey("lastTime", String.valueOf(endTime));

        //如果有没统计的干员就新增
        if (operatorStatisticsList.size() > 0) operatorDataMapper.insertStatisticsBatch(operatorStatisticsList);

    }

    @Test
    void statisticsTest() {


    }


    @Test
    void secretTest() {
        String SECRET = FileConfig.Secret;
        System.out.println(SECRET);
        String ip = "127.0.0.1";
        System.out.println(AES.encrypt(ip, SECRET));
        System.out.println(AES.encrypt(ip, SECRET));
    }


}
