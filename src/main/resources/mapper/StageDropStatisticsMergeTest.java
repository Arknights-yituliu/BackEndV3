package com.lhs.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.enums.TimeGranularity;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.dto.material.PenguinMatrixDTO;
import com.lhs.entity.dto.material.StageDropDetailDTO;
import com.lhs.entity.po.material.StageDropStatistics;
import com.lhs.entity.po.material.StageDropV2;
import com.lhs.entity.vo.material.StageDropStatisticsVO;
import com.lhs.mapper.material.StageDropMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
public class StageDropStatisticsMergeTest {

    @Resource
    private StageDropMapper stageDropMapper;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private Map<String, Date> getStartAndEndDate(Integer year, Integer month) {
        // 获取指定月份的第一天
        // 获取指定月份的第一天
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        Date start = Date.from(firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());


        int endMonth = month+1;
        int endYear = year;
        if(endMonth>12){
            endYear++;
            endMonth=1;
        }

        // 获取指定月份的第一天
        // 获取指定月份的第一天
        LocalDate firstDayOfNextMonth = LocalDate.of(endYear, endMonth, 1);
        Date end = Date.from(firstDayOfNextMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 获取指定月份的最后一天
//        LocalDate lastDayOfMonth = firstDayOfMonth.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
//        Date lastDayDate = Date.from(lastDayOfMonth.atTime(23, 59, 59, 999999999).atZone(ZoneId.systemDefault()).toInstant());


        Map<String, Date> result = new HashMap<>();
        result.put("start", start);
        result.put("end", end);

        return result;
    }


    @Test
    void stageDropStatisticsByMonth() {

        int year = 2024;
        int month = 7;
        for (int i = 0; i < 150; i++) {

            if (month > 12) {
                month = 1;
                year++;
            }

            Map<String, Date> startAndEndDate = getStartAndEndDate(year, month);
            Date start = startAndEndDate.get("start");
            Date end = startAndEndDate.get("end");


            List<StageDropStatistics> stageDropStatisticsList = stageDropMapper.
                    listStageDropStatisticsByDate(TimeGranularity.HOUR.code(), start, end);

            if (stageDropStatisticsList.isEmpty()) {
                System.out.println("<——————————没有数据——————————>");
                month++;
                continue;
            }

            Date startDate = stageDropStatisticsList.get(0).getEnd();
            Date endDate = stageDropStatisticsList.get(stageDropStatisticsList.size() - 1).getEnd();
            System.out.println("统计区间：" + sdf.format(startDate) + "——" + sdf.format(endDate));
            System.out.println("本次统计总数据量为：" + stageDropStatisticsList.size());

            mergeStageDropStatistics(stageDropStatisticsList, TimeGranularity.MONTH.code(), start,end);

            month++;
        }
    }

    @Test
    void stageDropStatistics() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        long start = 1720972800000L;
        for (int i = 0; i < 100000; i++) {
            long end = start + 60 * 60 * 24 * 1000L;

            if(end>1722009600000L){
                return;
            }

            List<StageDropStatistics> stageDropStatisticsList = stageDropMapper.
                    listStageDropStatisticsByDate(TimeGranularity.HOUR.code(), new Date(start), new Date(end));

            if (stageDropStatisticsList.isEmpty()) {
                System.out.println("<——————————没有数据——————————>");
                start = end;
                continue;
            }

            Date startDate = stageDropStatisticsList.get(0).getEnd();
            Date endDate = stageDropStatisticsList.get(stageDropStatisticsList.size() - 1).getEnd();
            System.out.println("统计区间：" + sdf.format(startDate) + "——" + sdf.format(endDate));
            System.out.println("本次统计总数据量为：" + stageDropStatisticsList.size());

            mergeStageDropStatistics(stageDropStatisticsList, TimeGranularity.DAY.code(), new Date(start),new Date(end));

            start = end;
        }
    }

    private void mergeStageDropStatistics(List<StageDropStatistics> stageDropStatisticsList, Integer timeGranularity, Date start,Date end) {

        IdGenerator idGenerator = new IdGenerator(1L);
        Map<String, List<StageDropStatistics>> collect = stageDropStatisticsList.stream().collect(Collectors.groupingBy(StageDropStatistics::getStageId));
        List<StageDropStatistics> insertList = new ArrayList<>();
        for (List<StageDropStatistics> byId : collect.values()) {
            HashMap<String, StageDropStatistics> dropQuantity = new HashMap<>();
            byId.sort(Comparator.comparing(StageDropStatistics::getEnd));
            int times = 0;
            long tmp = 0;
            for (StageDropStatistics drop : byId) {
                if ((drop.getEnd().getTime() - tmp) > 60 * 5 * 1000) {
                    times += drop.getTimes();
                    tmp = drop.getEnd().getTime();
                }
                if (dropQuantity.get(drop.getItemId()) != null) {
                    dropQuantity.get(drop.getItemId()).addQuantity(drop.getQuantity());
                } else {
                    dropQuantity.put(drop.getItemId(), drop);
                }
            }

            for (String key : dropQuantity.keySet()) {
                StageDropStatistics stageDropStatistics = dropQuantity.get(key);
                stageDropStatistics.setTimes(times);
                stageDropStatistics.setId(idGenerator.nextId());
                stageDropStatistics.setTimeGranularity(timeGranularity);
                stageDropStatistics.setStart(start);
                stageDropStatistics.setEnd(end);
                insertList.add(stageDropStatistics);
            }
        }
        System.out.println(sdf.format(new Date()) + "——本次插入统计结果条数：" + insertList.size());
        stageDropMapper.insertBatchStageDropStatistics(insertList);
    }

    @Test
    void stageDropCountStatistics() {
        List<StageDropStatistics> stageDropStatisticsList = stageDropMapper.listStageDropStatisticsByDate(TimeGranularity.HOUR.code(), new Date(1715702400000L), new Date());
        List<StageDropStatisticsVO> insertList = new ArrayList<>();

        Map<String, List<StageDropStatistics>> collect = stageDropStatisticsList.stream().collect(Collectors.groupingBy(StageDropStatistics::getStageId));

        for (List<StageDropStatistics> byId : collect.values()) {
            HashMap<String, StageDropStatistics> dropQuantity = new HashMap<>();
            byId.sort(Comparator.comparing(StageDropStatistics::getEnd));
            int times = 0;
            long tmp = 0;
            for (StageDropStatistics drop : byId) {
                if ((drop.getEnd().getTime() - tmp) > 60 * 5 * 1000) {
                    times += drop.getTimes();
                    tmp = drop.getEnd().getTime();
                }
                if (dropQuantity.get(drop.getItemId()) != null) {
                    dropQuantity.get(drop.getItemId()).addQuantity(drop.getQuantity());
                } else {
                    dropQuantity.put(drop.getItemId(), drop);
                }
            }

            for (String key : dropQuantity.keySet()) {
                StageDropStatistics po = dropQuantity.get(key);
                po.setTimes(times);

                StageDropStatisticsVO stageDropStatisticsVO = new StageDropStatisticsVO();
                stageDropStatisticsVO.setStageId(po.getStageId());
                stageDropStatisticsVO.setItemId(po.getItemId());
                stageDropStatisticsVO.setTimes(po.getTimes());
                stageDropStatisticsVO.setQuantity(po.getQuantity());
                stageDropStatisticsVO.setStart(po.getStart().getTime());
                stageDropStatisticsVO.setEnd(po.getEnd().getTime());
                insertList.add(stageDropStatisticsVO);
            }
        }


        String penguinStageDataText = FileUtil.read(ConfigUtil.Penguin + "penguin.json");
        String matrixText = JsonMapper.parseJSONObject(penguinStageDataText).get("matrix").toPrettyString();
        List<PenguinMatrixDTO> penguinMatrixDTOList = JsonMapper.parseJSONArray(matrixText, new TypeReference<>() {
        });

        List<PenguinMatrixDTO> penguinMatrixDTOS = penguinMatrixDTOList.stream().filter(e -> e.getStageId().equals("main_01-07")).toList();
        for(PenguinMatrixDTO dto:penguinMatrixDTOS){
            StageDropStatisticsVO stageDropStatisticsVO = new StageDropStatisticsVO();
            stageDropStatisticsVO.setStageId(dto.getStageId());
            stageDropStatisticsVO.setItemId(dto.getItemId());
            stageDropStatisticsVO.setTimes(dto.getTimes());
            stageDropStatisticsVO.setQuantity(dto.getQuantity());
            stageDropStatisticsVO.setStart(dto.getStart());
            stageDropStatisticsVO.setEnd(dto.getEnd());
            insertList.add(stageDropStatisticsVO);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("matrix", insertList);
        String jsonString = JsonMapper.toJSONString(result);

        FileUtil.save(ConfigUtil.Penguin, "yituliu.json", jsonString);
    }


    @Test
    void getStageData() {


    }


    private void dataStatistics(List<StageDropV2> stageDropV2List, Integer timeGranularity) {
        if (stageDropV2List.isEmpty()) {
            return;
        }
        IdGenerator idGenerator = new IdGenerator(1L);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date start = stageDropV2List.get(0).getCreateTime();
        Date end = stageDropV2List.get(stageDropV2List.size() - 1).getCreateTime();

        System.out.println("统计区间：" + sdf.format(start) + "——" + sdf.format(end));
        System.out.println("统计区间数据数量：" + stageDropV2List.size());

        HashMap<String, Integer> timesMap = new HashMap<>();
        HashMap<String, StageDropStatistics> dropQuantity = new HashMap<>();
        Date date = new Date();
        for (StageDropV2 stageDropV2 : stageDropV2List) {

            if (!"CN".equals(stageDropV2.getServer())) {
                continue;
            }

            String stageId = stageDropV2.getStageId();
            timesMap.merge(stageId, 1, Integer::sum);

            if (stageDropV2.getDrops().length() > 5) {
                List<StageDropDetailDTO> stageDropDetailDTOList = JsonMapper.parseJSONArray(stageDropV2.getDrops(),
                        new TypeReference<>() {
                        });
                for (StageDropDetailDTO dropDetail : stageDropDetailDTOList) {
                    String itemId = dropDetail.getItemId();
                    String key = stageId + "&" + itemId;
                    Integer quantity = dropDetail.getQuantity();
                    if (dropQuantity.get(key) != null) {
                        dropQuantity.get(key).addQuantity(quantity);
                    } else {
                        StageDropStatistics stageDropStatistics = new StageDropStatistics();

                        stageDropStatistics.setStageId(stageId);
                        stageDropStatistics.setItemId(itemId);
                        stageDropStatistics.setQuantity(quantity);
                        stageDropStatistics.setEnd(end);
                        stageDropStatistics.setTimeGranularity(timeGranularity);
                        stageDropStatistics.setCreateTime(date);
                        dropQuantity.put(key, stageDropStatistics);
                    }
                }
            }
        }

        List<StageDropStatistics> insertList = new ArrayList<>();
        for (String key : dropQuantity.keySet()) {
            StageDropStatistics stageDropStatistics = dropQuantity.get(key);
            stageDropStatistics.setId(idGenerator.nextId());
            String stageId = stageDropStatistics.getStageId();
            stageDropStatistics.setTimes(timesMap.get(stageId));
            insertList.add(stageDropStatistics);
        }

        System.out.println(sdf.format(new Date()) + "——本次插入统计结果条数：" + insertList.size());
        if (insertList.isEmpty()) {
            return;
        }
        stageDropMapper.insertBatchStageDropStatistics(insertList);
    }
}
