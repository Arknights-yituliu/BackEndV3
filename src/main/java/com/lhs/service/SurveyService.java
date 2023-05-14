package com.lhs.service;

import com.alibaba.fastjson.JSON;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.SurveyDataChar;
import com.lhs.entity.SurveyDataCharVo;
import com.lhs.entity.SurveyStatisticsChar;
import com.lhs.entity.SurveyUser;
import com.lhs.mapper.SurveyMapper;
import com.lhs.service.dto.SurveyStatisticsCharVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SurveyService {

    @Resource
    private SurveyMapper surveyMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public HashMap<Object, Object> register(String ipAddress, String userName) {


        Long incrId = redisTemplate.opsForValue().increment("incrUId");
        if(incrId==null){
            throw new ServiceException(ResultCode.USER_ID_ERROR);
        }

        Date date = new Date();
        long id =  incrId;

        String userNameAndEnd = userName + randomEnd4(4);

        SurveyUser surveyUser = surveyMapper.selectSurveyUserByUserName(userNameAndEnd);

        for (int i = 0; i < 3; i++) {
            if (surveyUser == null) break;
            userNameAndEnd = userName + randomEnd4(4);
            surveyUser = surveyMapper.selectSurveyUserByUserName(userNameAndEnd);
            log.error("发生uid碰撞");
        }

        if(surveyUser!=null) {
            log.error("uid碰撞过多扩充位数");
            userNameAndEnd = userName + randomEnd4(5);
            surveyUser = surveyMapper.selectSurveyUserByUserName(userNameAndEnd);
        }


        String charTable = String.valueOf(redisTemplate.opsForValue().get("charTable"));


        surveyUser = SurveyUser.builder()
                .id(id)
                .userName(userNameAndEnd)
                .createTime(date)
                .updateTime(date)
                .status(1)
                .ip(ipAddress)
                .charTable(charTable)
                .build();

        Integer row = surveyMapper.insertSurveyUser(surveyUser);
        if (row < 1) throw new ServiceException(ResultCode.SYSTEM_INNER_ERROR);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("userName", userNameAndEnd);
        hashMap.put("uid", id);
        hashMap.put("status", 1);
        return hashMap;
    }

    private String randomEnd4(Integer digit) {
        int random = new Random().nextInt(9999);
        String end4 = String.format("%0"+digit+"d", random);
        return "#" + end4;
    }


    public HashMap<Object, Object> login(String ipAddress, String userName) {
        SurveyUser surveyUser = surveyMapper.selectSurveyUserByUserName(userName);
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("userName", userName);
        hashMap.put("uid", surveyUser.getId());
        hashMap.put("status", surveyUser.getStatus());
        return hashMap;
    }





    public HashMap<Object, Object> uploadCharForm(String userName, List<SurveyDataChar> surveyDataCharList) {
        SurveyUser surveyUser = surveyMapper.selectSurveyUserByUserName(userName);
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        surveyUser.setUpdateTime(new Date());
        surveyMapper.updateSurveyUser(surveyUser);
        String charTable = surveyUser.getCharTable();
        Long uid = surveyUser.getId();
        int rowsAffected = 0;
        List<SurveyDataChar> insertList = new ArrayList<>();
        for (SurveyDataChar surveyDataChar : surveyDataCharList) {
            if(!surveyDataChar.getOwn())  continue;  //未持有不记录
            if(surveyDataChar.getPhase()<2){   //精英化阶段小于2 不能专精和开模组
                surveyDataChar.setSkill1(0);
                surveyDataChar.setSkill2(0);
                surveyDataChar.setSkill3(0);
                surveyDataChar.setModX(0);
                surveyDataChar.setModY(0);
            }

            String id = uid + "_" + surveyDataChar.getCharId(); //存储id
            SurveyDataChar surveyDataCharById = surveyMapper.selectSurveyDataCharById(charTable, id);
            surveyDataChar.setId(id);
            surveyDataChar.setUid(uid);
            if (surveyDataCharById == null) {
                insertList.add(surveyDataChar);
                rowsAffected++;
            } else {
                if (!surveyDataCharEquals(surveyDataChar, surveyDataCharById)) {
                    rowsAffected++;
                    surveyMapper.updateSurveyDataCharById(charTable, surveyDataChar);
                }
            }
        }

        if (insertList.size() > 0) surveyMapper.insertBatchSurveyDataChar(charTable, insertList);

        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("rowsAffected", rowsAffected);
        hashMap.put("uid", uid);
        return hashMap;
    }

    private boolean surveyDataCharEquals(SurveyDataChar newData, SurveyDataChar oldData) {
        return Objects.equals(newData.getPhase(), oldData.getPhase())
                && Objects.equals(newData.getPotential(), oldData.getPotential())
                && Objects.equals(newData.getSkill1(), oldData.getSkill1())
                && Objects.equals(newData.getSkill2(), oldData.getSkill2())
                && Objects.equals(newData.getSkill3(), oldData.getSkill3())
                && Objects.equals(newData.getModX(), oldData.getModX())
                && Objects.equals(newData.getModY(), oldData.getModY());
    }

    public HashMap<Object, Object> surveyStatisticsChar() {
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
            log.info("本次统计数量：" + surveyDataCharList_DB.size());

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

        HashMap<Object, Object> resultMessage = new HashMap<>();

        return resultMessage;
    }


    public List<SurveyDataCharVo> findCharacterForm(String userName) {
        SurveyUser surveyUser = surveyMapper.selectSurveyUserByUserName(userName);
        List<SurveyDataCharVo> surveyDataCharVos =  new ArrayList<>();

        if(surveyUser.getUpdateTime().getTime()==surveyUser.getUpdateTime().getTime()) return surveyDataCharVos;
        surveyDataCharVos =
                surveyMapper.selectSurveyDataCharByUid(surveyUser.getCharTable(), surveyUser.getId());

        return surveyDataCharVos;
    }



}
