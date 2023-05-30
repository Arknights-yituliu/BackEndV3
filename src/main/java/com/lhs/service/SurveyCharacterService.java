package com.lhs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.survey.*;
import com.lhs.mapper.SurveyCharacterMapper;
import com.lhs.service.dto.MaaOperBoxVo;
import com.lhs.service.dto.OperBox;
import com.lhs.service.dto.SurveyStatisticsCharVo;
import com.lhs.service.vo.CharStatisticsResult;
import com.lhs.service.vo.SurveyCharacterVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SurveyCharacterService {

    @Resource
    private SurveyCharacterMapper surveyCharacterMapper;

    @Resource
    private SurveyUserService surveyUserService;

    /**
     * 上传干员练度调查表
     * @param userName 用户名称
     * @param surveyCharacterList  干员练度调查表单
     * @return  成功消息
     */
    public HashMap<Object, Object> uploadCharForm(String userName, List<SurveyCharacter> surveyCharacterList) {

        SurveyUser surveyUser = surveyUserService.selectSurveyUser(userName);

        String tableName = "survey_character_"+surveyUserService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表

        Long uid = surveyUser.getId();  //用户的uid

        int insertRows = 0;
        int updateRows = 0;

        //用户之前上传的数据
        List<SurveyCharacter> surveyCharacterVos
                = surveyCharacterMapper.selectSurveyCharacterByUid(tableName, surveyUser.getId());

        //用户之前上传的数据转为map方便对比
        Map<String, SurveyCharacter> oldDataCollectById = surveyCharacterVos.stream()
                .collect(Collectors.toMap(SurveyCharacter::getId, Function.identity()));

        List<SurveyCharacter> insertList = new ArrayList<>();//新增数据批量插入集合

        for (SurveyCharacter surveyCharacter : surveyCharacterList) {
            String charId = surveyCharacter.getCharId().substring(surveyCharacter.getCharId().indexOf("_"));
            String id = uid + "_" + charId; //存储id

            //精英化阶段小于2 不能专精和开模组
            if (surveyCharacter.getPhase() < 2 || !surveyCharacter.getOwn()) {
                surveyCharacter.setSkill1(0);
                surveyCharacter.setSkill2(0);
                surveyCharacter.setSkill3(0);
                surveyCharacter.setModX(0);
                surveyCharacter.setModY(0);
            }
            //和老数据进行对比
            SurveyCharacter surveyCharacterById = oldDataCollectById.get(id);
             //为空则新增
            surveyCharacter.setId(id);
            surveyCharacter.setUid(uid);
            if (surveyCharacterById == null) {
                insertList.add(surveyCharacter);  //加入批量插入集合
                insertRows++;  //新增数据条数
            } else {
                //如果数据存在，同时有某个信息不一致则进行更新
                if (surveyDataCharEquals(surveyCharacter, surveyCharacterById)) {
                    updateRows++;  //更新数据条数
                    surveyCharacterMapper.updateSurveyCharacterById(tableName, surveyCharacter); //更新数据
                }
            }
        }

        if (insertList.size() > 0) surveyCharacterMapper.insertBatchSurveyCharacter(tableName, insertList);  //批量插入
        surveyUserService.updateSurveyUser(surveyUser);

        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("updateRows", updateRows);
        hashMap.put("insertRows", insertRows);
        return hashMap;
    }

    /**
     * 判断这个干员是否有变更
     * @param newData  新数据
     * @param oldData  旧数据
     * @return 成功消息
     */
    private boolean surveyDataCharEquals(SurveyCharacter newData, SurveyCharacter oldData) {
        return !Objects.equals(newData.getPhase(), oldData.getPhase())
                || !Objects.equals(newData.getPotential(), oldData.getPotential())
                || !Objects.equals(newData.getSkill1(), oldData.getSkill1())
                || !Objects.equals(newData.getSkill2(), oldData.getSkill2())
                || !Objects.equals(newData.getSkill3(), oldData.getSkill3())
                || !Objects.equals(newData.getModX(), oldData.getModX())
                || !Objects.equals(newData.getModY(), oldData.getModY())
                || !Objects.equals(newData.getOwn(), oldData.getOwn());
    }

    /**
     * 干员练度调查表统计
     *
     */
    public void characterStatistics() {
        List<Long> userIds = surveyUserService.selectSurveyUserIds();

        List<List<Long>> userIdsGroup = new ArrayList<>();
        String update_time = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        surveyUserService.updateConfigByKey(String.valueOf(userIds.size()),"user_count_character");
        surveyUserService.updateConfigByKey(String.valueOf(userIds.size()),update_time);

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

        surveyCharacterMapper.truncateCharacterStatisticsTable();  //清空统计表

        HashMap<String, SurveyStatisticsCharVo> hashMap = new HashMap<>();  //结果暂存对象
        List<SurveyStatisticsCharacter> surveyDataCharList = new ArrayList<>();  //最终结果

        for (List<Long> longs : userIdsGroup) {
            List<SurveyCharacterVo> surveyDataCharList_DB =
                    surveyCharacterMapper.selectSurveyCharacterVoByUidList("survey_character_1", longs);

//            log.info("本次统计数量：" + surveyDataCharList_DB.size());
            log.info("本次统计数量：" + surveyDataCharList_DB.size());

            //根据干员id分组
            Map<String, List<SurveyCharacterVo>> collectByCharId = surveyDataCharList_DB.stream()
                    .collect(Collectors.groupingBy(SurveyCharacterVo::getCharId));

            //计算结果
            collectByCharId.forEach((charId, list) -> {
                int own = list.size();  //持有人数
                int rarity = list.get(0).getRarity();  //星级

                Map<Integer, Long> collectByPhases = list.stream()
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getPhase, Collectors.counting()));
                //该干员精英等级一的数量
                int phases1 = collectByPhases.get(1) == null ? 0 : collectByPhases.get(1).intValue();
                //该干员精英等级二的数量
                int phases2 = collectByPhases.get(2) == null ? 0 : collectByPhases.get(2).intValue();

                //根据该干员的潜能等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectByPotential = list.stream()
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getPotential, Collectors.counting()));
                //根据该干员的技能专精等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectBySkill1 = list.stream()
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getSkill1, Collectors.counting()));
                Map<Integer, Long> collectBySkill2 = list.stream()
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getSkill2, Collectors.counting()));
                Map<Integer, Long> collectBySkill3 = list.stream()
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getSkill3, Collectors.counting()));
                //根据该干员的模组等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectByModX = list.stream()
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getModX, Collectors.counting()));
                Map<Integer, Long> collectByModY = list.stream()
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getModY, Collectors.counting()));

                //和上一组用户id的数据合并
                if (hashMap.get(charId) != null) {
                    SurveyStatisticsCharVo lastData = hashMap.get(charId);
                    own += lastData.getOwn();
                    phases2 += lastData.getPhase2();
                    lastData.getPotential()
                            .forEach((k, v) -> collectByPotential.merge(k, v, Long::sum));
                    lastData.getSkill1()
                            .forEach((k, v) -> collectBySkill1.merge(k, v, Long::sum));
                    lastData.getSkill2()
                            .forEach((k, v) -> collectBySkill2.merge(k, v, Long::sum));
                    lastData.getSkill3()
                            .forEach((k, v) -> collectBySkill3.merge(k, v, Long::sum));
                    lastData.getModX()
                            .forEach((k, v) -> collectByModX.merge(k, v, Long::sum));
                    lastData.getModY()
                            .forEach((k, v) -> collectByModY.merge(k, v, Long::sum));
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
            SurveyStatisticsCharacter build = SurveyStatisticsCharacter.builder()
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

        surveyCharacterMapper.insertBatchCharacterStatistics(surveyDataCharList);
    }



    /**
     * 找回用户填写的数据
     * @param userName 用户id
     * @return 成功消息
     */
    public List<SurveyCharacterVo> findCharacterForm(String userName) {
        SurveyUser surveyUser = surveyUserService.selectSurveyUser(userName);
        List<SurveyCharacterVo> surveyCharacterVos = new ArrayList<>();
        if (surveyUser.getCreateTime().getTime() == surveyUser.getUpdateTime().getTime())
            return surveyCharacterVos;  //更新时间和注册时间一致直接返回空

        String tableName = "survey_character_"+surveyUserService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表

        List<SurveyCharacter> surveyCharacterList = surveyCharacterMapper.selectSurveyCharacterByUid(tableName, surveyUser.getId());
        surveyCharacterList.forEach(e->{
            SurveyCharacterVo build = SurveyCharacterVo.builder()
                    .charId(e.getCharId())
                    .level(e.getLevel())
                    .own(e.getOwn())
                    .phase(e.getPhase())
                    .potential(e.getPotential())
                    .rarity(e.getRarity())
                    .skill1(e.getSkill1())
                    .skill2(e.getSkill2())
                    .skill3(e.getSkill3())
                    .modX(e.getModX())
                    .modY(e.getModY())
                    .build();
            surveyCharacterVos.add(build);
        });

        return surveyCharacterVos;
    }


    /**
     * 干员信息统计
     * @return 成功消息
     */
    public HashMap<Object, Object> charStatisticsResult() {
        List<SurveyStatisticsCharacter> surveyStatisticsCharacters = surveyCharacterMapper.selectCharacterStatisticsList();
        HashMap<Object, Object> hashMap = new HashMap<>();

        String userCountStr = surveyUserService.selectConfigByKey("user_count_character");
        String updateTime = surveyUserService.selectConfigByKey("update_time_character");
        double userCount = Double.parseDouble(userCountStr);

        List<CharStatisticsResult> charStatisticsResultList = new ArrayList<>();
        surveyStatisticsCharacters.forEach(item -> {
            CharStatisticsResult build = CharStatisticsResult.builder()
                    .charId(item.getCharId())
                    .rarity(item.getRarity())
                    .own(item.getOwn() * 100 / userCount)
                    .phase2(item.getPhase2() * 100 / (double) item.getOwn())
                    .skill1(splitCalculation(item.getSkill1(), item.getOwn()))
                    .skill2(splitCalculation(item.getSkill2(), item.getOwn()))
                    .skill3(splitCalculation(item.getSkill3(), item.getOwn()))
                    .modX(splitCalculation(item.getModX(), item.getOwn()))
                    .modY(splitCalculation(item.getModY(), item.getOwn()))
                    .build();
            charStatisticsResultList.add(build);
        });

        hashMap.put("userCount", userCountStr);
        hashMap.put("result", charStatisticsResultList);
        hashMap.put("updateTime", updateTime);

        return hashMap;
    }

    /**
     * 分割计算
     * @param JSONStr  旧结果
     * @param own  干员持有数
     * @return 计算结果
     */
    public HashMap<String, Double> splitCalculation(String JSONStr, Integer own) {
        JSONObject jsonObject = JSONObject.parseObject(JSONStr);
        HashMap<String, Double> hashMap = new HashMap<>();
        jsonObject.forEach((k, v) -> hashMap.put("rank" + k, Double.parseDouble(String.valueOf(v)) * 100 / own));
        return hashMap;
    }


    /**
     * 上传maa干员信息
     * @param maaOperBoxVo  maa识别出的的结果
     * @param ipAddress   ip
     * @return  成功消息
     */
    public HashMap<Object, Object> saveMaaCharData(MaaOperBoxVo maaOperBoxVo, String ipAddress) {
        JSONArray operBoxs = maaOperBoxVo.getOperBox();
        List<SurveyCharacter> surveyCharacterList = new ArrayList<>();

        operBoxs.forEach(item -> {
            OperBox operBox = JSONObject.parseObject(String.valueOf(item), OperBox.class);
            SurveyCharacter build = SurveyCharacter.builder()
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
            surveyCharacterList.add(build);
        });

        SurveyUser surveyUser = surveyUserService.registerByMaa(ipAddress);
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        surveyUser.setUpdateTime(new Date());
        surveyUserService.updateSurveyUser(surveyUser);

        String tableName = "survey_character_"+surveyUserService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表

        Long uid = surveyUser.getId();
        int insertRows = 0;
        int updateRows = 0;


        List<SurveyCharacter> surveyCharacterVos
                = surveyCharacterMapper.selectSurveyCharacterByUid(tableName, surveyUser.getId());

        Map<String, SurveyCharacter> surveyDataCharCollectById = surveyCharacterVos.stream()
                .collect(Collectors.toMap(SurveyCharacter::getId, Function.identity()));

        List<SurveyCharacter> insertList = new ArrayList<>();

        for (SurveyCharacter surveyCharacter : surveyCharacterList) {
            String id = uid + "_" + surveyCharacter.getCharId(); //存储id
//            if(!surveyDataChar.getOwn())  continue;  //未持有不记录

            //精英化阶段小于2 不能专精和开模组
            if (surveyCharacter.getPhase() < 2 || !surveyCharacter.getOwn()) {
                surveyCharacter.setSkill1(0);
                surveyCharacter.setSkill2(0);
                surveyCharacter.setSkill3(0);
                surveyCharacter.setModX(0);
                surveyCharacter.setModY(0);
            }

//            SurveyDataChar surveyDataCharById = surveyMapper.selectSurveyDataCharById(charTable, id);
            SurveyCharacter surveyCharacterById = surveyDataCharCollectById.get(id);
            surveyCharacter.setId(id);
            surveyCharacter.setUid(uid);

            if (surveyCharacterById == null) {
                insertList.add(surveyCharacter);
                insertRows++;
            } else {
                if (surveyDataCharEquals(surveyCharacter, surveyCharacterById)) {
                    updateRows++;
                    surveyCharacterMapper.updateSurveyCharacterById(tableName, surveyCharacter);
                }
            }
        }

        if (insertList.size() > 0) surveyCharacterMapper.insertBatchSurveyCharacter(tableName, insertList);

        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("updateRows", updateRows);
        hashMap.put("insertRows", insertRows);
        hashMap.put("uid", uid);
        return hashMap;
    }









}
