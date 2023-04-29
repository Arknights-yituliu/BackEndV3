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
public class MaaService {

    @Resource
    private MaaRecruitMapper maaRecruitMapper;
    @Resource
    private MaaRecruitStatisticalMapper maaStatisticalMapper;
    @Resource
    private ResultMapper resultMapper;
    @Resource
    private ScheduleMapper scheduleMapper;

    @Resource
    private MaaUserMapper maaUserMapper;

    @Resource
    private OperatorDataMapper operatorDataMapper;


    public HashMap<String, Long> saveMaaOperatorBoxData(MaaOperBoxVo maaOperBoxVo, String ipAddress) {

        List<OperBox> operatorBox = JSONArray.parseArray(JSON.toJSONString(maaOperBoxVo.getOperBox()), OperBox.class);//maa返回的干员box信息
        int operatorTotal = operatorBox.size();      //干员box的干员总数

        ipAddress = ipAddress + new Random().nextInt(9999999);   //测试ip+随机数
        QueryWrapper<MaaUser> queryWrapperUser = new QueryWrapper<>();
        queryWrapperUser.eq("ip", ipAddress);  //根据ip查找
        log.info("ip: " + ipAddress);
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
             throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }

        if (maaUser == null) {
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
            log.info("用户已经存在");
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

        tableName = maaUser.getTableName();


        for (OperBox operator : operatorBox) {
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
        hashMap.put("rows affected", rowsAffected);
        hashMap.put("uid", id);

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

                long holdings = list.size();

                String charId = "1";

                if(list.get(0).getCharId()!=null) {
                    charId = list.get(0).getCharId();
                }
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
                    operatorStatistics = OperatorStatistics.builder()
                            .charId(charId)
                            .charName(k)
                            .holdings(holdings)
                            .phases1(phases1)
                            .phases2(phases2 + 100)
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
                            .phases2(phases2 + 100)
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

        FileUtil.save(response, FileConfig.Schedule, scheduleId.toString(), jsonForMat);
    }


    public String exportScheduleJson(Long scheduleId) {
//        Schedule schedule = scheduleMapper.selectOne(new QueryWrapper<Schedule>().eq("schedule_id",scheduleId));
        String read = FileUtil.read(FileConfig.Schedule + scheduleId + ".json");
        if (read == null) throw new ServiceException(ResultCode.DATA_NONE);
//        if(schedule.getSchedule()==null) throw new ServiceException(ResultCode.DATA_NONE);
        return read;
    }


    public String saveMaaRecruitData(MaaRecruitData maaRecruitData) {
        int insert = maaRecruitMapper.insert(maaRecruitData);
        return null;
    }


    public void maaRecruitDataCalculation() {

        List<MaaRecruitStatistical> maaRecruitStatisticalList = maaStatisticalMapper.selectList(
                new QueryWrapper<MaaRecruitStatistical>().orderByDesc("create_time"));
        Date date = new Date();
        List<MaaRecruitData> maaRecruitDataList = maaRecruitMapper.selectList(
                new QueryWrapper<MaaRecruitData>().between("create_time", maaRecruitStatisticalList.get(0).getLastTime(), date));

        int topOperator = 0;  //高级资深总数
        int seniorOperator = 0; //资深总数
        int topAndSeniorOperator = 0; //高级资深含有资深总数
        int seniorOperatorCount = 0;  //五星TAG总数
        int rareOperatorCount = 0;   //四星TAG总数
        int commonOperatorCount = 0; //三星TAG总数
        int robot = 0;                //小车TAG总数
        int robotChoice = 0;       //小车和其他组合共同出现次数
        int vulcan = 0;             //火神出现次数
        int gravel = 0;            //砾出现次数
        int jessica = 0;         //杰西卡次数
        int count = 1;
        for (MaaRecruitData maaRecruitData : maaRecruitDataList) {

            int topAndSeniorOperatorSign = 0;  //高资与资深标记
            boolean vulcanSignMain = false; //火神标记
            boolean vulcanSignItem = false; //火神标记
            boolean jessicaSignMain = false; //杰西卡标记
            boolean jessicaSignItem = false;  //杰西卡标记
            boolean gravelSign = false; //砾标记


            List<String> tags = JSONArray.parseArray(maaRecruitData.getTag(), String.class);

            for (String tag : tags) {
                if ("高级资深干员".equals(tag)) {
                    topOperator++;
                    topAndSeniorOperatorSign++;
                }
                if ("资深干员".equals(tag)) {
                    seniorOperator++;
                    topAndSeniorOperatorSign++;
                }
                if ("支援机械".equals(tag)) {
                    robot++;
                    if (maaRecruitData.getLevel() > 3) robotChoice++;
                }
                if ("生存".equals(tag)) {
                    vulcanSignMain = true;
                    jessicaSignMain = true;
                }
                if ("重装干员".equals(tag) || "防护".equals(tag)) {
                    vulcanSignItem = true;
                }
                if ("狙击干员".equals(tag) || "远程位".equals(tag)) {
                    jessicaSignItem = true;
                }
                if ("快速复活".equals(tag)) {
                    gravelSign = true;
                }

            }

            if (maaRecruitData.getLevel() == 6) {
                vulcanSignMain = false;
                gravelSign = false;
                jessicaSignMain = false;
            }
            if (maaRecruitData.getLevel() == 5) {
                seniorOperatorCount++;
                gravelSign = false;
                jessicaSignMain = false;
            }
            if (maaRecruitData.getLevel() == 4) rareOperatorCount++;
            if (maaRecruitData.getLevel() == 3) commonOperatorCount++;
            if (topAndSeniorOperatorSign > 1) topAndSeniorOperator++;

            if (vulcanSignMain && vulcanSignItem) {
                vulcan++;
            }
            if (jessicaSignMain && jessicaSignItem) {
                jessica++;
            }
            if (gravelSign) {
                gravel++;
            }
            if (jessicaSignMain && jessicaSignItem) {
                jessica++;
            }


            count++;
        }


        MaaRecruitStatistical maaRecruitStatistical =
                MaaRecruitStatistical.builder().id(date.getTime()).topOperator(topOperator).seniorOperator(seniorOperator)
                        .topAndSeniorOperator(topAndSeniorOperator).seniorOperatorCount(seniorOperatorCount).rareOperatorCount(rareOperatorCount)
                        .commonOperatorCount(commonOperatorCount).robot(robot).robotChoice(robotChoice).vulcan(vulcan).gravel(gravel).jessica(jessica)
                        .maaRecruitDataCount(maaRecruitDataList.size()).lastTime(maaRecruitDataList.get(maaRecruitDataList.size() - 1).getCreateTime())
                        .createTime(date).build();

        maaStatisticalMapper.insert(maaRecruitStatistical);

        maaRecruitStatisticalList.add(maaRecruitStatistical);
        MaaRecruitStatistical statistical = statistical(maaRecruitStatisticalList);
        String result = JSON.toJSONString(statistical);
        resultMapper.insert(new ResultVo("maa/statistical", result));
    }


    private static MaaRecruitStatistical statistical(List<MaaRecruitStatistical> statisticalList) {
        MaaRecruitStatistical statistical = new MaaRecruitStatistical();

        statistical.setTopOperator(statisticalList.stream().mapToInt(MaaRecruitStatistical::getTopOperator).sum());
        statistical.setSeniorOperator(statisticalList.stream().mapToInt(MaaRecruitStatistical::getSeniorOperator).sum());
        statistical.setTopAndSeniorOperator(statisticalList.stream().mapToInt(MaaRecruitStatistical::getTopAndSeniorOperator).sum());
        statistical.setSeniorOperatorCount(statisticalList.stream().mapToInt(MaaRecruitStatistical::getSeniorOperatorCount).sum());
        statistical.setRareOperatorCount(statisticalList.stream().mapToInt(MaaRecruitStatistical::getRareOperatorCount).sum());
        statistical.setCommonOperatorCount(statisticalList.stream().mapToInt(MaaRecruitStatistical::getCommonOperatorCount).sum());
        statistical.setRobot(statisticalList.stream().mapToInt(MaaRecruitStatistical::getRobot).sum());
        statistical.setRobotChoice(statisticalList.stream().mapToInt(MaaRecruitStatistical::getRobotChoice).sum());
        statistical.setVulcan(statisticalList.stream().mapToInt(MaaRecruitStatistical::getVulcan).sum());
        statistical.setGravel(statisticalList.stream().mapToInt(MaaRecruitStatistical::getGravel).sum());
        statistical.setJessica(statisticalList.stream().mapToInt(MaaRecruitStatistical::getJessica).sum());
        statistical.setMaaRecruitDataCount(statisticalList.stream().mapToInt(MaaRecruitStatistical::getMaaRecruitDataCount).sum());
        statistical.setCreateTime(new Date());

        return statistical;
    }


    public String maaRecruitStatistical() {
        ResultVo resultVo = resultMapper.selectOne(new QueryWrapper<ResultVo>().eq("path", "maa/statistical").orderByDesc("create_time").last("limit 1"));
        if (resultVo.getId() == null) throw new ServiceException(ResultCode.DATA_NONE);
        return resultVo.getResult();
    }


}
