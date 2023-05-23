package com.lhs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.survey.SurveyDataChar;
import com.lhs.entity.survey.SurveyDataCharVo;
import com.lhs.entity.survey.SurveyStatisticsChar;
import com.lhs.entity.survey.SurveyUser;
import com.lhs.mapper.SurveyMapper;
import com.lhs.service.dto.MaaOperBoxVo;
import com.lhs.service.dto.OperBox;
import com.lhs.service.dto.SurveyStatisticsCharVo;
import com.lhs.service.vo.CharStatisticsResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SurveyService {

    @Resource
    private SurveyMapper surveyMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    /**
     * 调查表注册
     *
     * @param ipAddress ip
     * @param userName  用户id
     * @return
     */
    public HashMap<Object, Object> register(String ipAddress, String userName) {
//        Long incrId = (long) new Random().nextInt(100000);  //测试id
//        String charTable = "survey_data_char_1";        //测试表名

        Long incrId = redisTemplate.opsForValue().increment("incrUId");   //从redis拿到自增id
        String charTable = String.valueOf(redisTemplate.opsForValue().get("charTable"));  //从redis拿到要存的表名
        Date date = new Date();  //存入的时间
        String userNameAndEnd = null;
        SurveyUser surveyUser = null;

        for (int i = 0; i < 5; i++) {
            if (i < 3) {
                userNameAndEnd = userName + randomEnd4(4); //用户id后四位后缀  #xxxx
                surveyUser = surveyMapper.selectSurveyUserByUserName(userNameAndEnd);
                log.warn("发生用户id碰撞");
            } else {
                userNameAndEnd = userName + randomEnd4(5);
                surveyUser = surveyMapper.selectSurveyUserByUserName(userNameAndEnd);
                log.warn("用户id碰撞过多扩充位数");
            }
            if (surveyUser == null) break;  //未注册就跳出开始注册
        }

        if (surveyUser != null) throw new ServiceException(ResultCode.USER_ID_ERROR);

        surveyUser = SurveyUser.builder()
                .id(incrId)
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
        hashMap.put("uid", incrId);
        hashMap.put("status", 1);
        return hashMap;
    }

    private String randomEnd4(Integer digit) {
        int random = new Random().nextInt(9999);
        String end4 = String.format("%0" + digit + "d", random);
        return "#" + end4;
    }


    /**
     * 调查表登录
     * @param ipAddress  ip
     * @param userName  用户id
     * @return
     */
    public HashMap<Object, Object> login(String ipAddress, String userName) {
        SurveyUser surveyUser = surveyMapper.selectSurveyUserByUserName(userName);  //查询用户
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("userName", userName);
        hashMap.put("uid", surveyUser.getId());
        hashMap.put("status", surveyUser.getStatus());
        return hashMap;
    }

    /**
     * 上传干员练度调查表
     * @param userName 用户名称
     * @param surveyDataCharList  干员练度调查表单
     * @return
     */
    public HashMap<Object, Object> uploadCharForm(String userName, List<SurveyDataChar> surveyDataCharList) {
        SurveyUser surveyUser = surveyMapper.selectSurveyUserByUserName(userName); //查询用户

        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        surveyUser.setUpdateTime(new Date());   //更新用户最后一次上传时间

        String charTable = surveyUser.getCharTable();  //拿到这个用户的干员练度数据存在了哪个表
        Long uid = surveyUser.getId();  //用户的uid
        int insertRows = 0;
        int updateRows = 0;

        //用户之前上传的数据
        List<SurveyDataChar> surveyDataCharVos
                = surveyMapper.selectSurveyDataCharByUid(surveyUser.getCharTable(), surveyUser.getId());

        //用户之前上传的数据转为map方便对比
        Map<String, SurveyDataChar> oldDataCollectById = surveyDataCharVos.stream()
                .collect(Collectors.toMap(SurveyDataChar::getId, Function.identity()));

        List<SurveyDataChar> insertList = new ArrayList<>();//新增数据批量插入集合

        for (SurveyDataChar surveyDataChar : surveyDataCharList) {
            String id = uid + "_" + surveyDataChar.getCharId(); //存储id

            //精英化阶段小于2 不能专精和开模组
            if (surveyDataChar.getPhase() < 2 || !surveyDataChar.getOwn()) {
                surveyDataChar.setSkill1(0);
                surveyDataChar.setSkill2(0);
                surveyDataChar.setSkill3(0);
                surveyDataChar.setModX(0);
                surveyDataChar.setModY(0);
            }
            //和老数据进行对比
            SurveyDataChar surveyDataCharById = oldDataCollectById.get(id);
             //为空则新增
            if (surveyDataCharById == null) {
                surveyDataChar.setId(id);
                surveyDataChar.setUid(uid);
                insertList.add(surveyDataChar);  //加入批量插入集合
                insertRows++;  //新增数据条数
            } else {
                //如果数据存在，同时有某个信息不一致则进行更新 （考虑到可能更新量不大，没用when case批量更新
                if (!surveyDataCharEquals(surveyDataChar, surveyDataCharById)) {
                    updateRows++;  //更新数据条数
                    surveyMapper.updateSurveyDataCharById(charTable, surveyDataChar); //更新数据
                }
            }
        }

        if (insertList.size() > 0) surveyMapper.insertBatchSurveyDataChar(charTable, insertList);  //批量插入
        surveyMapper.updateSurveyUser(surveyUser);   //更新用户表

        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("updateRows", updateRows);
        hashMap.put("insertRows", insertRows);
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
                && Objects.equals(newData.getModY(), oldData.getModY())
                && Objects.equals(newData.getOwn(), oldData.getOwn());
    }


    /**
     * 干员练度调查表统计
     * @return
     */
    public HashMap<Object, Object> charStatistics() {
        List<Long> userIds = surveyMapper.selectSurveyUserIds();
        List<List<Long>> userIdsGroup = new ArrayList<>();
        int length = userIds.size();
        // 计算用户id按500个用户一组可以分成多少组
        int num = length / 500 + 1;
        int fromIndex = 0;   // id分组开始
        int toIndex = 500;   //id分组结束
        for (int i = 0; i < num; i++) {
            toIndex = Math.min(toIndex, userIds.size());
            userIdsGroup.add(userIds.subList(fromIndex, toIndex));
            fromIndex += 500;
            toIndex += 500;
        }


        surveyMapper.truncateCharStatisticsTable();  //清空统计表

        HashMap<String, SurveyStatisticsCharVo> hashMap = new HashMap<>();
        List<SurveyStatisticsChar> surveyDataCharList = new ArrayList<>();

        for (int i = 0; i < userIdsGroup.size(); i++) {
            List<SurveyDataCharVo> surveyDataCharList_DB =
                    surveyMapper.selectSurveyDataCharVoByUidList("survey_data_char_1", userIdsGroup.get(i));

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
                //根据该干员的技能专精等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectBySkill1 = list.stream()
                        .collect(Collectors.groupingBy(SurveyDataCharVo::getSkill1, Collectors.counting()));
                Map<Integer, Long> collectBySkill2 = list.stream()
                        .collect(Collectors.groupingBy(SurveyDataCharVo::getSkill2, Collectors.counting()));
                Map<Integer, Long> collectBySkill3 = list.stream()
                        .collect(Collectors.groupingBy(SurveyDataCharVo::getSkill3, Collectors.counting()));
                //根据该干员的模组等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectByModX = list.stream()
                        .collect(Collectors.groupingBy(SurveyDataCharVo::getModX, Collectors.counting()));
                Map<Integer, Long> collectByModY = list.stream()
                        .collect(Collectors.groupingBy(SurveyDataCharVo::getModY, Collectors.counting()));

                //和上一组用户id的数据合并
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

               //存入dto对象进行暂存
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

        //将dto对象转为数据库对象
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

    /**
     * 找回用户填写的数据
     * @param userName 用户id
     * @return
     */
    public List<SurveyDataCharVo> findCharacterForm(String userName) {
        SurveyUser surveyUser = surveyMapper.selectSurveyUserByUserName(userName);
        List<SurveyDataCharVo> surveyDataCharVos = new ArrayList<>();
        if (surveyUser.getCreateTime().getTime() == surveyUser.getUpdateTime().getTime()) return surveyDataCharVos;  //更新时间和注册时间一致直接返回空
        surveyDataCharVos =
                surveyMapper.selectSurveyDataCharVoByUid(surveyUser.getCharTable(), surveyUser.getId());

        return surveyDataCharVos;
    }


    public HashMap<Object, Object> charStatisticsResult() {
        List<SurveyStatisticsChar> surveyStatisticsChars = surveyMapper.selectCharStatisticsList();
        HashMap<Object, Object> hashMap = new HashMap<>();
        String userCountStr = surveyMapper.selectConfigByKey("userCount");
        String updateTime = surveyMapper.selectConfigByKey("updateTime");
        double userCount = Double.parseDouble(userCountStr);

        List<CharStatisticsResult> charStatisticsResultList = new ArrayList<>();
        surveyStatisticsChars.forEach(item -> {
            CharStatisticsResult build = CharStatisticsResult.builder()
                    .charId(item.getCharId())
                    .rarity(item.getRarity())
                    .own(item.getOwn() * 100 / userCount)
                    .phase2(item.getPhase2() * 100 / userCount)
                    .skill1(splitCalculation(item.getSkill1(), userCount))
                    .skill2(splitCalculation(item.getSkill2(), userCount))
                    .skill3(splitCalculation(item.getSkill3(), userCount))
                    .modX(splitCalculation(item.getModX(), userCount))
                    .modY(splitCalculation(item.getModY(), userCount))
                    .build();
            charStatisticsResultList.add(build);
        });
        hashMap.put("userCount", userCountStr);
        hashMap.put("result", charStatisticsResultList);
        hashMap.put("updateTime", "2023-11-14");
        return hashMap;
    }

    public HashMap<String, Double> splitCalculation(String JSONStr, Double userCount) {
        JSONObject jsonObject = JSONObject.parseObject(JSONStr);
        HashMap<String, Double> hashMap = new HashMap<>();
        jsonObject.forEach((k, v) -> {
            hashMap.put("rank" + k, Double.parseDouble(String.valueOf(v)) * 100 / userCount);
        });
        return hashMap;
    }

    public HashMap<Object, Object> saveMaaCharData(MaaOperBoxVo maaOperBoxVo, String ipAddress) {
        JSONArray operBoxs = maaOperBoxVo.getOperBox();
        List<SurveyDataChar> surveyDataCharList = new ArrayList<>();

        operBoxs.forEach(item -> {
            OperBox operBox = JSONObject.parseObject(String.valueOf(item), OperBox.class);
            SurveyDataChar build = SurveyDataChar.builder()
                    .charId(operBox.getId())
                    .level(operBox.getLevel())
                    .phase(operBox.getElite())
                    .potential(operBox.getPotential())
                    .rarity(operBox.getRarity())
                    .own(operBox.getOwn())
                    .skill1(-1)
                    .skill2(-1)
                    .skill3(-1)
                    .modX(-1)
                    .modY(-1)
                    .build();
            surveyDataCharList.add(build);
        });

        SurveyUser surveyUser = registerByMaa(ipAddress);
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        surveyUser.setUpdateTime(new Date());
        surveyMapper.updateSurveyUser(surveyUser);
        String charTable = surveyUser.getCharTable();
        Long uid = surveyUser.getId();
        int insertRows = 0;
        int updateRows = 0;


        List<SurveyDataChar> surveyDataCharVos
                = surveyMapper.selectSurveyDataCharByUid(surveyUser.getCharTable(), surveyUser.getId());

        Map<String, SurveyDataChar> surveyDataCharCollectById = surveyDataCharVos.stream()
                .collect(Collectors.toMap(SurveyDataChar::getId, Function.identity()));


        List<SurveyDataChar> insertList = new ArrayList<>();


        for (SurveyDataChar surveyDataChar : surveyDataCharList) {
            String id = uid + "_" + surveyDataChar.getCharId(); //存储id
//            if(!surveyDataChar.getOwn())  continue;  //未持有不记录

            //精英化阶段小于2 不能专精和开模组
            if (surveyDataChar.getPhase() < 2 || !surveyDataChar.getOwn()) {
                surveyDataChar.setSkill1(0);
                surveyDataChar.setSkill2(0);
                surveyDataChar.setSkill3(0);
                surveyDataChar.setModX(0);
                surveyDataChar.setModY(0);
            }

//            SurveyDataChar surveyDataCharById = surveyMapper.selectSurveyDataCharById(charTable, id);
            SurveyDataChar surveyDataCharById = surveyDataCharCollectById.get(id);
            surveyDataChar.setId(id);
            surveyDataChar.setUid(uid);

            if (surveyDataCharById == null) {
                insertList.add(surveyDataChar);
                insertRows++;
            } else {
                if (!surveyDataCharEquals(surveyDataChar, surveyDataCharById)) {
                    updateRows++;
                    surveyMapper.updateSurveyDataCharById(charTable, surveyDataChar);
                }
            }
        }

        if (insertList.size() > 0) surveyMapper.insertBatchSurveyDataChar(charTable, insertList);

        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("updateRows", updateRows);
        hashMap.put("insertRows", insertRows);
        hashMap.put("uid", uid);
        return hashMap;


    }

    public SurveyUser registerByMaa(String ipAddress) {
//        Long incrId = redisTemplate.opsForValue().increment("incrUId");
//        String charTable = String.valueOf(redisTemplate.opsForValue().get("charTable"));
        SurveyUser surveyUser = surveyMapper.selectLastSurveyUserIp(ipAddress);

        if (surveyUser == null) {
            Long incrId = (long) new Random().nextInt(100000);
            String charTable = "survey_data_char_1";

            if (incrId == null) {
                throw new ServiceException(ResultCode.USER_ID_ERROR);
            }

            Date date = new Date();
            long id = incrId;
            String userNameAndEnd = "MAA#" + incrId;
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
        }

        return surveyUser;
    }
}
