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
import com.lhs.mapper.MaaRecruitMapper;
import com.lhs.mapper.MaaRecruitStatisticalMapper;
import com.lhs.mapper.ResultMapper;
import com.lhs.mapper.ScheduleMapper;
import com.lhs.entity.MaaRecruitData;
import com.lhs.entity.MaaRecruitStatistical;
import com.lhs.entity.ResultVo;
import com.lhs.entity.Schedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

@Service
public class MaaService {

    @Resource
    private MaaRecruitMapper maaRecruitMapper;
    @Resource
    private MaaRecruitStatisticalMapper maaStatisticalMapper;
    @Resource
    private ResultMapper resultMapper;
    @Resource
    private ScheduleMapper scheduleMapper;

    



    public String saveMaaRecruitData(MaaRecruitData maaRecruitData) {
        int insert = maaRecruitMapper.insert(maaRecruitData);
        return null;
    }

    
    public void saveScheduleJson(String scheduleJson, Long scheduleId) {
        Schedule schedule = new Schedule();
        schedule.setUid(scheduleId);
        schedule.setScheduleId(scheduleId);
        JSONObject jsonObject = JSONObject.parseObject(scheduleJson);
        jsonObject.put("id",scheduleId);
        schedule.setSchedule(JSON.toJSONString(jsonObject));
        schedule.setCreateTime(new Date());
        scheduleMapper.insert(schedule);
    }

    
    public void exportScheduleFile(HttpServletResponse response, Long scheduleId) {

        Schedule schedule = scheduleMapper.selectOne(new QueryWrapper<Schedule>().eq("schedule_id",scheduleId));
        String jsonForMat = JSON.toJSONString(JSONObject.parseObject(schedule.getSchedule()), SerializerFeature.PrettyFormat,
                SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullListAsEmpty);
//        System.out.println(FileConfig.Schedule);

        FileUtil.save(response, FileConfig.Schedule,scheduleId.toString(),jsonForMat);
    }

    
    public String exportScheduleJson(Long scheduleId) {
//        Schedule schedule = scheduleMapper.selectOne(new QueryWrapper<Schedule>().eq("schedule_id",scheduleId));
        String read = FileUtil.read(FileConfig.Schedule + scheduleId + ".json");
        if(read ==null)throw new ServiceException(ResultCode.DATA_NONE);
//        if(schedule.getSchedule()==null) throw new ServiceException(ResultCode.DATA_NONE);
        return read;
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
        resultMapper.insert(new ResultVo("maa/statistical",result));
    }


    private static MaaRecruitStatistical  statistical(List<MaaRecruitStatistical> statisticalList){
        MaaRecruitStatistical statistical = new MaaRecruitStatistical();
//        MaaRecruitStatistical.MaaRecruitStatisticalBuilder builder = MaaRecruitStatistical.builder();
//        statisticalList.forEach(data->{
//               builder.id(1000L).topOperator(data.getTopOperator()).seniorOperator(data.getSeniorOperator())
//                    .topAndSeniorOperator(data.getTopAndSeniorOperator()).seniorOperatorCount(data.getSeniorOperatorCount())
//                    .rareOperatorCount(data.getRareOperatorCount()).commonOperatorCount(data.getCommonOperatorCount()).robot(data.getRobot())
//                     .robotChoice(data.getRobotChoice()).vulcan(data.getVulcan()).gravel(data.getGravel()).jessica(data.getJessica())
//                    .maaRecruitDataCount(data.getMaaRecruitDataCount()).createTime(data.getCreateTime());
//        });


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
        ResultVo resultVo = resultMapper.selectOne(new QueryWrapper<ResultVo>().eq("path","maa/statistical").orderByDesc("create_time").last("limit 1"));
        if(resultVo.getId()==null) throw new ServiceException(ResultCode.DATA_NONE);
        return resultVo.getResult();
    }


}
