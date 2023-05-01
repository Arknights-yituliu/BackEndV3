package com.lhs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.config.FileConfig;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.*;
import com.lhs.mapper.*;
import com.lhs.service.dto.MaaOperBoxVo;
import com.lhs.service.dto.OperBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OperatorSurveyService {


    @Resource
    private ScheduleMapper scheduleMapper;

    @Resource
    private MaaUserMapper maaUserMapper;

    @Resource
    private OperatorDataMapper operatorDataMapper;




    public HashMap<String, Long> saveMaaOperatorBoxData(MaaOperBoxVo maaOperBoxVo, String ipAddress) {

        List<OperBox> operatorBox = JSONArray.parseArray(JSON.toJSONString(maaOperBoxVo.getOperBox()), OperBox.class);//maa返回的干员box信息
        int operatorTotal = operatorBox.size();      //干员box的干员总数

//       ipAddress = ipAddress + new Random().nextInt(9999999);   //测试ip+随机数

        QueryWrapper<MaaUser> queryWrapperUser = new QueryWrapper<>();
        queryWrapperUser.eq("ip", ipAddress);  //根据ip查找

        MaaUser maaUser = maaUserMapper.selectOne(queryWrapperUser);  //查询是否有这个用户
        Date date = new Date();  //获取当前时间
        long timeStamp = date.getTime();


        long millis = System.currentTimeMillis();  //到毫秒的时间戳
        int end3 = new Random().nextInt(999);  //随机4位数
        long id = millis * 100 + end3;  //分配的id

        OperatorStatisticsConfig config = operatorDataMapper.selectConfigByKey("tableName");

        String tableName = config.getConfigValue();  //存入的数据表的表名

        long rowsAffected = 0;

        if(operatorBox.size()<2) {
             throw new ServiceException(ResultCode.PARAM_IS_BLANK);   //干员过少不收录
        }

        if (maaUser == null) {        //用户不存在新建
            maaUser = MaaUser.builder()
                    .id(id)
                    .penguinId(maaOperBoxVo.getUuid())
                    .server(maaOperBoxVo.getServer())
                    .source(maaOperBoxVo.getSource())
                    .version(maaOperBoxVo.getVersion())
                    .operatorTotal(operatorTotal)
                    .tableName(tableName)
                    .ip(ipAddress)
                    .createTime(date)
                    .updateTime(date)
                    .build();
            int insert = maaUserMapper.insert(maaUser);
        } else {
            //用户存在更新各种信息
            tableName = maaUser.getTableName();
            log.info("ip: " + ipAddress+"----用户已经存在");
            maaUser.setOperatorTotal(operatorTotal);
            maaUser.setUpdateTime(date);
            maaUser.setServer(maaOperBoxVo.getServer());
            maaUser.setSource(maaOperBoxVo.getSource());
            maaUser.setVersion(maaOperBoxVo.getVersion());
            maaUser.setPenguinId(maaOperBoxVo.getUuid());
            id = maaUser.getId();
            int insert = maaUserMapper.updateById(maaUser);
        }

        List<OperatorData> operatorDataList = new ArrayList<>();




        for (OperBox operator : operatorBox) {

            if("".equals(operator.getId())||operator.getId()==null) continue;
            OperatorData operatorData_DB =
                    operatorDataMapper.selectOperatorDataById(maaUser.getTableName(), id + "_" + operator.getId());

            if (operatorData_DB != null) {
                if (Objects.equals(operator.getElite(), operatorData_DB.getElite()) &&
                        Objects.equals(operator.getLevel(), operatorData_DB.getLevel()) &&
                        Objects.equals(operator.getPotential(), operatorData_DB.getPotential())) {
                } else {
                    operatorData_DB.setElite(operator.getElite());
                    operatorData_DB.setLevel(operator.getLevel());
                    operatorData_DB.setPotential(operator.getPotential());
                    operatorData_DB.setUpdateTimeStamp(timeStamp);

                    rowsAffected++;
                    operatorDataMapper.updateOperatorDataById(tableName, operatorData_DB);
                }
                continue;
            }

            OperatorData build = OperatorData.builder().id(id + "_" + operator.getId())
                    .userId(id)
                    .charId(operator.getId())
                    .charName(operator.getName())
                    .elite(operator.getElite())
                    .rarity(operator.getRarity())
                    .level(operator.getLevel())
                    .own(operator.getOwn())
                    .potential(operator.getPotential())
                    .createTimeStamp(timeStamp)
                    .updateTimeStamp(timeStamp)
                    .build();

            rowsAffected++;
            operatorDataList.add(build);
        }

        if (operatorDataList.size() > 0) {
            Integer integer = operatorDataMapper.insertOperatorDataBatch(maaUser.getTableName(), operatorDataList);
        }

        HashMap<String, Long> hashMap = new HashMap<>();
        hashMap.put("rowsAffected", rowsAffected);
        hashMap.put("uid", id);
        System.out.println(hashMap);
        return hashMap;
    }


    public void OperatorStatistics() {

        Long userCount = maaUserMapper.selectCount(null); //统计上传者数量

        if (userCount > 25000) {
            operatorDataMapper.updateConfigByKey("tableName", "operator_data_2"); //切换存储表
        }
        if (userCount > 50000) {
            operatorDataMapper.updateConfigByKey("tableName", "operator_data_3");
        }

        operatorDataMapper.updateConfigByKey("user_count ", String.valueOf(userCount));

        List<Long> userIds = operatorDataMapper.selectIdsByPage();

        List<List<Long>> userIdsGroup = new ArrayList<>();

        int length = userIds.size();

        // 计算可以分成多少组
        int num = length/500+1;
        int fromIndex = 0;
        int toIndex = 500;
        for (int i = 0; i < num; i++) {
            toIndex = Math.min(toIndex, userIds.size());
            System.out.println("fromIndex:"+fromIndex+"---toIndex:"+toIndex);
            userIdsGroup.add(userIds.subList(fromIndex,toIndex));
            fromIndex+=500;
            toIndex+=500;
        }

        operatorDataMapper.truncateOperatorStatisticsTable();

        for (int i = 0; i < userIdsGroup.size(); i++) {

            //以上次时间戳做start，当前时间戳作end，查询这个时间段的时间
            List<OperatorDataVo> operator_data_DB =
                    operatorDataMapper.selectOperatorDataByUserId("operator_data_1", userIdsGroup.get(i));

            log.info("本次统计数量：" + operator_data_DB.size());
            //根据干员名称分组
            Map<String, List<OperatorDataVo>> collectByCharName = operator_data_DB.stream()
                    .collect(Collectors.groupingBy(OperatorDataVo::getCharName));

            //如果这个干员没有被统计，需要插入的新统计数据
            List<OperatorStatistics> operatorStatisticsList = new ArrayList<>();

            //循环这个分组后的map
            collectByCharName.forEach((k, list) -> {

                int holdings = list.size();

                String charId = list.get(0).getCharId();
                int rarity = list.get(0).getRarity();

                //根据精英化等级分类
                Map<Integer, Long> collectByPhases = list.stream()
                        .collect(Collectors.groupingBy(OperatorDataVo::getElite, Collectors.counting()));
                //该干员精英等级一的数量
                int phases1 = collectByPhases.get(1) == null ? 0 : collectByPhases.get(1).intValue();
                //该干员精英等级二的数量
                int phases2 = collectByPhases.get(2) == null ? 0 : collectByPhases.get(2).intValue();

                //根据该干员的潜能等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectByPotential = list.stream().collect(Collectors.groupingBy(OperatorDataVo::getPotential, Collectors.counting()));
                //json字符串化
                String potentialStr = JSON.toJSONString(collectByPotential);
                //查询是否有这条记录
                OperatorStatistics operatorStatistics = operatorDataMapper.selectStatisticsByCharName(k);

                if (operatorStatistics == null) {
                    operatorStatistics = OperatorStatistics.builder()
                            .charId(charId)
                            .charName(k)
                            .rarity(rarity)
                            .holdings(holdings)
                            .phases1(phases1)
                            .phases2(phases2)
                            .potentialRanks(potentialStr)
                            .build();
                    //没有的话塞入集合准备新增
                    operatorStatisticsList.add(operatorStatistics);
                } else {
                    //有记录的话直接更新
                    holdings += operatorStatistics.getHoldings();
                    phases1 += operatorStatistics.getPhases1();
                    phases2 += operatorStatistics.getPhases2();

                    //合并该干员上次和本次潜能统计的数据
                    JSONObject jsonObject = JSONObject.parseObject(operatorStatistics.getPotentialRanks());
                    Map<Integer, Long> potentialRanksNew = new HashMap<>();
                    jsonObject.forEach((p, v) -> potentialRanksNew.put(Integer.parseInt(p), Long.parseLong(String.valueOf(v))));

                    collectByPotential.forEach((potential, v) -> potentialRanksNew.merge(potential, v, Long::sum));
                    potentialStr = JSON.toJSONString(potentialRanksNew);

                    operatorStatistics = OperatorStatistics.builder()
                            .charId(charId)
                            .charName(k)
                            .holdings(holdings)
                            .phases1(phases1)
                            .phases2(phases2 )
                            .potentialRanks(potentialStr)
                            .build();
                    operatorDataMapper.updateStatisticsByCharName(operatorStatistics);
                }

            });

            operator_data_DB =null;

            //如果有没统计的干员就新增
            if (operatorStatisticsList.size() > 0) operatorDataMapper.insertStatisticsBatch(operatorStatisticsList);

        }
    }


    public void saveScheduleJson(String scheduleJson, Long scheduleId) {
        Schedule schedule = new Schedule();
        schedule.setUid(scheduleId);
        schedule.setScheduleId(scheduleId);
        JSONObject jsonObject = JSONObject.parseObject(scheduleJson);
        jsonObject.put("id", scheduleId);
        schedule.setSchedule(JSON.toJSONString(jsonObject));
        schedule.setCreateTime(new Date());
        scheduleMapper.insert(schedule);
    }


    public void exportScheduleFile(HttpServletResponse response, Long scheduleId) {

        Schedule schedule = scheduleMapper.selectOne(new QueryWrapper<Schedule>().eq("schedule_id", scheduleId));
        String jsonForMat = JSON.toJSONString(JSONObject.parseObject(schedule.getSchedule()), SerializerFeature.PrettyFormat,
                SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullListAsEmpty);
//        System.out.println(FileConfig.Schedule);

        FileUtil.save(response, FileConfig.Schedule, scheduleId.toString()+".json", jsonForMat);
    }


    public String retrieveScheduleJson(Long scheduleId) {
        Schedule schedule = scheduleMapper.selectOne(new QueryWrapper<Schedule>().eq("schedule_id",scheduleId));

        if(schedule==null){
            String read = FileUtil.read(FileConfig.Schedule + scheduleId + ".json");
            if (read == null) throw new ServiceException(ResultCode.DATA_NONE);
            return read;
        }

        return schedule.getSchedule();
    }



}
