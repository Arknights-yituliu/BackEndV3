package com.lhs.util;

import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.dto.material.StageDropDetailDTO;
import com.lhs.entity.po.material.StageDrop;
import com.lhs.entity.po.material.StageDropDetail;
import com.lhs.entity.po.material.StageDropV2;
import com.lhs.mapper.material.StageDropMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
public class MoveStageDrop1Test {

    @Resource
    private StageDropMapper stageDropMapper;

    private final  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");



    @Test
    void idTest() throws InterruptedException {
        IdGenerator idGenerator = new IdGenerator(1L);
        Long start = idGenerator.nextId();
        // 等待5秒
        Thread.sleep(5000);
        Long end = idGenerator.nextId();
        System.out.println(end-start);
    }

    @Test
    void move() {
        IdGenerator idGenerator = new IdGenerator(1L);
        long start = 1713844800000L;

        for (int i = 0; i < 10000; i++) {
            long end = start + 60 * 60 * 3 * 1000;
            List<StageDrop> stageDropList = stageDropMapper.selectStageDropByDate(start, end);
            if (stageDropList.isEmpty()) {
                System.out.println("<——————————没有数据——————————>");
                start = end;
                continue;
            }
            Long startId = stageDropList.get(0).getId()-50000000;
            Long endId = stageDropList.get(stageDropList.size()-1).getId()+50000000;
            List<StageDropDetail> stageDropDetailList = stageDropMapper.selectStageDropDetail(startId,endId);
            Map<Long, List<StageDropDetail>> collect = stageDropDetailList.stream().collect(Collectors.groupingBy(StageDropDetail::getChildId));
            System.out.println("本次导出数据数量：" + stageDropList.size()+"——"+stageDropDetailList.size());
            System.out.println("开始——" + sdf.format(new Date(stageDropList.get(0).getCreateTime())));
            System.out.println("结束——" + sdf.format(new Date(stageDropList.get(stageDropList.size() - 1).getCreateTime())));
            List<StageDropV2> stageDropV2List = new ArrayList<>();
            for (StageDrop stageDrop : stageDropList) {

                if (stageDrop.getUid().length() > 49) {
                    System.out.println("错误uid：" + stageDrop.getUid());
                    continue;
                }

                if (stageDrop.getTimes() > 1) {
                    continue;
                }

                StageDropV2 stageDropV2 = new StageDropV2();
                stageDropV2.setId(idGenerator.nextId());
                stageDropV2.setUid(stageDrop.getUid());
                stageDropV2.setStageId(stageDrop.getStageId());
                stageDropV2.setTimes(stageDrop.getTimes());
                stageDropV2.setCreateTime(new Date(stageDrop.getCreateTime()));
                stageDropV2.setServer(stageDrop.getServer());
                stageDropV2.setSource(stageDrop.getSource());
                stageDropV2.setVersion(stageDrop.getVersion());

                List<StageDropDetail> stageDropDetailListById = collect.get(stageDrop.getId());
                List<StageDropDetailDTO> drop1 = new ArrayList<>();
                stageDropV2.setDrops(JsonMapper.toJSONString(drop1));
                if (stageDropDetailListById != null) {
                    List<StageDropDetailDTO> dtoList = new ArrayList<>();
                    for (StageDropDetail po : stageDropDetailListById) {
                        StageDropDetailDTO dto = new StageDropDetailDTO();
                        dto.setItemId(po.getItemId());
                        dto.setQuantity(po.getQuantity());
                        dto.setDropType(po.getDropType());
                        dtoList.add(dto);
                    }
                    stageDropV2.setDrops(JsonMapper.toJSONString(dtoList));
                }


                stageDropV2List.add(stageDropV2);
            }

            start = end;


            List<StageDropV2> insert = new ArrayList<>();
            for (StageDropV2 stageDropV2 : stageDropV2List) {
                insert.add(stageDropV2);
                if (insert.size() == 2000) {
                    stageDropMapper.insertBatch("stage_drop_8",insert);
                    insert.clear();
                }
            }

            if (!insert.isEmpty()) {
                stageDropMapper.insertBatch("stage_drop_8",insert);
            }
            System.out.println("本次插入数据数量：" + stageDropV2List.size());

        }
    }


    @Test
    void move2(){
        long start = 1722009600000L;
        long end = 1722182400000L;
        List<StageDropV2> stageDropV2List = stageDropMapper.selectStageDropV2ByDate("stage_drop_8",new Date(start), new Date(end));
        System.out.println(sdf.format(stageDropV2List.get(0).getCreateTime())+"{}"+sdf.format(stageDropV2List.get(stageDropV2List.size()-1).getCreateTime()));
        List<StageDropV2> insert = new ArrayList<>();
        IdGenerator idGenerator = new IdGenerator(1L);
        for (StageDropV2 stageDropV2 : stageDropV2List) {
            stageDropV2.setId(idGenerator.nextId());
            insert.add(stageDropV2);
            if (insert.size() == 2000) {
                stageDropMapper.insertBatch("stage_drop",insert);
                insert.clear();
            }
        }

        if (!insert.isEmpty()) {
            stageDropMapper.insertBatch("stage_drop",insert);
        }
        System.out.println("本次插入数据数量：" + stageDropV2List.size());
    }


    @Test
    void select1() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        String stageId1 = "tough_12-15";
        String mainId1 = "31053";
        String stageId2 = "main_08-13";
        String mainId2 = "31033";

        String stageId3 = "tough_14-13";
        String mainId3 = "31053";


        List<Map<String, Object>> resultList = new ArrayList<>();
        List<StageDropV2> stageDropV2List = stageDropMapper.selectStageDropV2ByStageId(stageId3, new Date(1722182400000L), new Date());
        long start = 1722182400000L;
        long quantity = 0;
        long times = 0;
        for (StageDropV2 dropV2 : stageDropV2List) {
            if (dropV2.getCreateTime().getTime() >= start) {
                Map<String, Object> result = new HashMap<>();
                result.put("time", sdf.format(new Date(start)));
                result.put("quantity", quantity);
                result.put("times", times);
                result.put("knockRating", (double) quantity / (double) times);
                resultList.add(result);
                start = start + 60 * 60*24*7 * 1000;
                quantity = 0;
                times = 0;
            }
            times++;
            if (dropV2.getDrops().contains(mainId3)) {
                quantity++;
            }
        }
        System.out.println(JsonMapper.toJSONString(resultList));

    }



}
