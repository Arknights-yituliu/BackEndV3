package com.lhs.service.survey;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.survey.*;
import com.lhs.mapper.SurveyOperatorMapper;
import com.lhs.vo.survey.SurveyStatisticsChar;
import com.lhs.vo.survey.CharacterStatisticsResult;
import com.lhs.vo.survey.SurveyCharacterExcelVo;
import com.lhs.vo.survey.SurveyCharacterVo;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SurveyCharacterService {


    private final SurveyOperatorMapper surveyOperatorMapper;

    private final SurveyUserService surveyUserService;

    private final RedisTemplate<String, Object> redisTemplate;

    public SurveyCharacterService(SurveyOperatorMapper surveyOperatorMapper, SurveyUserService surveyUserService, RedisTemplate<String, Object> redisTemplate) {
        this.surveyOperatorMapper = surveyOperatorMapper;
        this.surveyUserService = surveyUserService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 上传干员练度调查表
     *
     * @param token              token
     * @param surveyOperatorList 干员练度调查表单
     * @return 成功消息
     */
//    @TakeCount(name = "上传评分")
    public Map<String, Object> uploadCharForm(String token, List<SurveyOperator> surveyOperatorList) {
        SurveyUser surveyUser = surveyUserService.getSurveyUserById(token);
        return updateSurveyData(surveyUser, surveyOperatorList);
    }

    /**
     * 导入干员练度调查表
     *
     * @param file  Excel文件
     * @param token token
     * @return 成功消息
     */
    public Map<String, Object> importExcel(MultipartFile file, String token) {
        List<SurveyOperator> list = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), SurveyCharacterExcelVo.class, new AnalysisEventListener<SurveyCharacterExcelVo>() {
                public void invoke(SurveyCharacterExcelVo surveyCharacterExcelVo, AnalysisContext analysisContext) {
                    SurveyOperator surveyOperator = new SurveyOperator();
                    BeanUtils.copyProperties(surveyCharacterExcelVo, surveyOperator);
                    list.add(surveyOperator);
                }

                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                }
            }).sheet().doRead();

        } catch (IOException e) {
            e.printStackTrace();
        }

        SurveyUser surveyUser = surveyUserService.getSurveyUserById(token);
        return updateSurveyData(surveyUser, list);
    }


    /**
     * 通用的上传方法
     * @param surveyUser         用户信息
     * @param surveyOperatorList 干员练度调查表
     * @return
     */
    private Map<String, Object> updateSurveyData(SurveyUser surveyUser, List<SurveyOperator> surveyOperatorList) {

        long uid = surveyUser.getId();

//        if (surveyUser.getUid() != null) {
//            uid = surveyUser.getUid();
//        }

        Date date = new Date();
        String tableName = "survey_character_" + surveyUserService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表
        int affectedRows = 0;

        //用户之前上传的数据
        List<SurveyOperator> surveyOperatorVos
                = surveyOperatorMapper.selectSurveyCharacterByUid(tableName, uid);

        //用户之前上传的数据转为map方便对比
        Map<String, SurveyOperator> savedDataMap = surveyOperatorVos.stream()
                .collect(Collectors.toMap(SurveyOperator::getCharId, Function.identity()));

        //新增数据
        List<SurveyOperator> insertList = new ArrayList<>();


        for (SurveyOperator surveyOperator : surveyOperatorList) {


            if (!surveyOperator.getOwn()) continue;
            //精英化阶段小于2 不能专精和开模组
            if (surveyOperator.getElite() < 2) {
                surveyOperator.setSkill1(-1);
                surveyOperator.setSkill2(-1);
                surveyOperator.setSkill3(-1);
                surveyOperator.setModX(-1);
                surveyOperator.setModY(-1);
            }

            if (surveyOperator.getRarity() < 6) {
                surveyOperator.setSkill3(-1);
            }



            //和老数据进行对比
            SurveyOperator savedData = savedDataMap.get(surveyOperator.getCharId());
            //为空则新增

            if (savedData == null) {
                Long characterId = redisTemplate.opsForValue().increment("CharacterId");
                surveyOperator.setId(characterId);
                surveyOperator.setUid(uid);
                insertList.add(surveyOperator);  //加入批量插入集合
                affectedRows++;  //新增数据条数
            } else {
                //如果数据存在，进行更新
                affectedRows++;  //更新数据条数
                surveyOperator.setId(savedData.getId());
                surveyOperator.setUid(uid);
                surveyOperatorMapper.updateSurveyCharacterById(tableName, surveyOperator); //更新数据
            }
        }

        if (insertList.size() > 0) surveyOperatorMapper.insertBatchSurveyCharacter(tableName, insertList);  //批量插入


        surveyUser.setUpdateTime(date);   //更新用户最后一次上传时间
        surveyUserService.updateSurveyUser(surveyUser);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("affectedRows", affectedRows);
        hashMap.put("updateTime", simpleDateFormat.format(date));
        hashMap.put("registered",false);
        return hashMap;
    }

    public void exportSurveyCharacterForm(HttpServletResponse response, String token) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserById(token);
        //拿到这个用户的干员练度数据存在了哪个表

        String tableName = "survey_character_" + surveyUserService.getTableIndex(surveyUser.getId());
        //用户之前上传的数据

        List<SurveyOperator> list
                = surveyOperatorMapper.selectSurveyCharacterByUid(tableName, surveyUser.getId());

        List<SurveyCharacterExcelVo> listVo = new ArrayList<>();


        Map<String, CharacterTable> characterTable = surveyUserService.getCharacterTable();

        for (SurveyOperator surveyOperator : list) {

            SurveyCharacterExcelVo surveyCharacterExcelVo = new SurveyCharacterExcelVo();
            BeanUtils.copyProperties(surveyOperator, surveyCharacterExcelVo);

            if (characterTable.get(surveyOperator.getCharId()) == null) {
                surveyCharacterExcelVo.setName("未知干员，待更新");
            } else {
                surveyCharacterExcelVo.setName(characterTable.get(surveyOperator.getCharId()).getName());
            }

            listVo.add(surveyCharacterExcelVo);

        }


        ExcelUtil.exportExcel(response, listVo, SurveyCharacterExcelVo.class, "characterForm");
    }

    /**
     * 判断这个干员是否有变更
     *
     * @param newData 新数据
     * @param oldData 旧数据
     * @return 成功消息
     */
    private boolean surveyDataCharEquals(SurveyOperator newData, SurveyOperator oldData) {
        return !Objects.equals(newData.getElite(), oldData.getElite())
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
     */
    public void characterStatistics() {
        List<Long> userIds = surveyUserService.selectSurveyUserIds();


        List<List<Long>> userIdsGroup = new ArrayList<>();
        String updateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        redisTemplate.opsForHash().put("Survey", "UpdateTime.Operator", updateTime);
        redisTemplate.opsForHash().put("Survey", "UserCount.Operator", userIds.size());

        int length = userIds.size();
        // 计算用户id按500个用户一组可以分成多少组
        int num = length / 100 + 1;
        int fromIndex = 0;   // id分组开始
        int toIndex = 100;   //id分组结束
        for (int i = 0; i < num; i++) {
            toIndex = Math.min(toIndex, userIds.size());
            userIdsGroup.add(userIds.subList(fromIndex, toIndex));
            fromIndex += 100;
            toIndex += 100;
        }

        surveyOperatorMapper.truncateCharacterStatisticsTable();  //清空统计表

        HashMap<String, SurveyStatisticsChar> hashMap = new HashMap<>();  //结果暂存对象

        List<SurveyStatisticsCharacter> surveyDataCharList = new ArrayList<>();  //最终结果

        for (List<Long> ids : userIdsGroup) {
            if (ids.size() == 0) continue;
            List<SurveyCharacterVo> surveyDataCharList_DB =
                    surveyOperatorMapper.selectSurveyCharacterVoByUidList("survey_character_1", ids);

            log.info("本次统计数量：" + surveyDataCharList_DB.size());

            //根据干员id分组
            Map<String, List<SurveyCharacterVo>> collectByCharId = surveyDataCharList_DB.stream()
                    .collect(Collectors.groupingBy(SurveyCharacterVo::getCharId));

            //计算结果
            collectByCharId.forEach((charId, list) -> {
                list = list.stream()
                        .filter(SurveyCharacterVo::getOwn)
                        .collect(Collectors.toList());

                int own = list.size();  //持有人数
                int rarity = list.get(0).getRarity();  //星级

                //根据该干员精英等级分组统计 map(精英等级,该等级的数量)
                Map<Integer, Long> collectByElite = list.stream()
                        .filter(e -> e.getElite() > -1)
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getElite, Collectors.counting()));

                //根据该干员的潜能等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectByPotential = list.stream()
                        .filter(e -> e.getPotential() > -1)
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getPotential, Collectors.counting()));

                //根据该干员的技能专精等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectBySkill1 = list.stream()
                        .filter(e -> e.getSkill1() > -1)
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getSkill1, Collectors.counting()));

                Map<Integer, Long> collectBySkill2 = list.stream()
                        .filter(e -> e.getSkill2() > -1)
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getSkill2, Collectors.counting()));

                Map<Integer, Long> collectBySkill3 = list.stream()
                        .filter(e -> e.getSkill3() > -1)
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getSkill3, Collectors.counting()));

                //根据该干员的模组等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectByModX = list.stream()
                        .filter(e -> e.getModX() > -1)
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getModX, Collectors.counting()));

                Map<Integer, Long> collectByModY = list.stream()
                        .filter(e -> e.getModY() > -1)
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getModY, Collectors.counting()));

                //和上一组用户id的数据合并
                if (hashMap.get(charId) != null) {
                    SurveyStatisticsChar lastData = hashMap.get(charId);
                    own += lastData.getOwn();

                    lastData.getElite()
                            .forEach((k, v) -> collectByElite.merge(k, v, Long::sum));

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
                SurveyStatisticsChar build = SurveyStatisticsChar.builder()
                        .charId(charId)
                        .own(own)
                        .elite(collectByElite)
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
            int sampleSizeElite = getSampleSize(v.getElite());
            int sampleSizeSkill1 = getSampleSize(v.getSkill1());
            int sampleSizeSkill2 = getSampleSize(v.getSkill2());
            int sampleSizeSkill3 = getSampleSize(v.getSkill3());
            int sampleSizeModX = getSampleSize(v.getModX());
            int sampleSizeModY = getSampleSize(v.getModY());
            int sampleSizePotential = getSampleSize(v.getPotential());


            SurveyStatisticsCharacter build = SurveyStatisticsCharacter.builder()
                    .charId(v.getCharId())
                    .rarity(v.getRarity())
                    .own(v.getOwn())
                    .elite(JsonMapper.toJSONString(v.getElite()))
                    .sampleSizeElite(sampleSizeElite)
                    .skill1(JsonMapper.toJSONString(v.getSkill1()))
                    .sampleSizeSkill1(sampleSizeSkill1)
                    .skill2(JsonMapper.toJSONString(v.getSkill2()))
                    .sampleSizeSkill2(sampleSizeSkill2)
                    .skill3(JsonMapper.toJSONString(v.getSkill3()))
                    .sampleSizeSkill3(sampleSizeSkill3)
                    .modX(JsonMapper.toJSONString(v.getModX()))
                    .sampleSizeModX(sampleSizeModX)
                    .modY(JsonMapper.toJSONString(v.getModY()))
                    .sampleSizeModY(sampleSizeModY)
                    .potential(JsonMapper.toJSONString(v.getPotential()))
                    .sampleSizePotential(sampleSizePotential)
                    .build();

            surveyDataCharList.add(build);

        });

        surveyOperatorMapper.insertBatchCharacterStatistics(surveyDataCharList);
    }

    private static Integer getSampleSize(Map<Integer, Long> map) {
        long sampleSize = 0L;
        for (Integer rank : map.keySet()) {
            sampleSize += map.get(rank);
        }
        return (int) sampleSize;

    }


    /**
     * 找回用户填写的数据
     *
     * @param token token
     * @return 成功消息
     */
    public List<SurveyCharacterVo> getCharacterForm(String token) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserById(token);
        List<SurveyCharacterVo> surveyCharacterVos = new ArrayList<>();
        if (surveyUser.getCreateTime().getTime() == surveyUser.getUpdateTime().getTime())
            return surveyCharacterVos;  //更新时间和注册时间一致直接返回空

        String tableName = "survey_character_" + surveyUserService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表

        List<SurveyOperator> surveyOperatorList = surveyOperatorMapper.selectSurveyCharacterByUid(tableName, surveyUser.getId());
        surveyOperatorList.forEach(e -> {
            SurveyCharacterVo build = SurveyCharacterVo.builder()
                    .charId(e.getCharId())
                    .level(e.getLevel())
                    .own(e.getOwn())
                    .mainSkill(e.getMainSkill())
                    .elite(e.getElite())
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
     *
     * @return 成功消息
     */
    public HashMap<Object, Object> getCharStatisticsResult() {
        List<SurveyStatisticsCharacter> surveyStatisticsCharacters = surveyOperatorMapper.selectCharacterStatisticsList();
        HashMap<Object, Object> hashMap = new HashMap<>();


        Object survey = redisTemplate.opsForHash().get("Survey", "UserCount.Operator");
        String updateTime = String.valueOf(redisTemplate.opsForHash().get("Survey", "UpdateTime.Operator"));

        double userCount = Double.parseDouble(survey + ".0");
        List<CharacterStatisticsResult> characterStatisticsResultList = new ArrayList<>();
        surveyStatisticsCharacters.forEach(item -> {
            CharacterStatisticsResult build = CharacterStatisticsResult.builder()
                    .charId(item.getCharId())
                    .rarity(item.getRarity())
                    .own((double) item.getOwn() / userCount)
                    .elite(splitCalculation(item.getElite(), item.getSampleSizeElite()))
                    .skill1(splitCalculation(item.getSkill1(), item.getSampleSizeSkill1()))
                    .skill2(splitCalculation(item.getSkill2(), item.getSampleSizeSkill2()))
                    .skill3(splitCalculation(item.getSkill3(), item.getSampleSizeSkill3()))
                    .modX(splitCalculation(item.getModX(), item.getSampleSizeModX()))
                    .modY(splitCalculation(item.getModY(), item.getSampleSizeModY()))
                    .build();

            characterStatisticsResultList.add(build);
        });


        hashMap.put("userCount", userCount);
        hashMap.put("result", characterStatisticsResultList);
        hashMap.put("updateTime", updateTime);

        return hashMap;
    }

    /**
     * 计算具体结果
     *
     * @param result     旧结果
     * @param sampleSize 样本
     * @return 计算结果
     */
    public HashMap<String, Double> splitCalculation(String result, Integer sampleSize) {
        HashMap<String, Double> hashMap = new HashMap<>();
        JsonNode jsonNode = JsonMapper.parseJSONObject(result);
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

        while (fields.hasNext()) {
            String key = fields.next().getKey();

            int sum = jsonNode.get(key).intValue();

            hashMap.put("rank" + key, (double) sum / sampleSize);
        }

        return hashMap;
    }

    public String getEquipDict() {
        if (redisTemplate.opsForValue().get("equipDictSimple") != null) {
            return String.valueOf(redisTemplate.opsForValue().get("equipDictSimple"));
        }
        String read = FileUtil.read(ApplicationConfig.Item + "equipDict.json");
        assert read != null;
        redisTemplate.opsForValue().set("equipDictSimple", read);
        return read;
    }

    public Map<String, Object> importSKLandPlayerInfo(String token, String cred) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserById(token);
        List<SurveyOperator> surveyOperatorList = new ArrayList<>();

        JsonNode equipDict = JsonMapper.parseJSONObject(getEquipDict());

        HashMap<String, String> header = new HashMap<>();
        cred = cred.trim();
        header.put("cred", cred);

        String SKLandPlayerBindingAPI = ApplicationConfig.SKLandPlayerBindingAPI;
        String SKLandPlayerBinding = HttpRequestUtil.get(SKLandPlayerBindingAPI, header);

        JsonNode SKLandPlayerBindingNode = JsonMapper.parseJSONObject(SKLandPlayerBinding);
        JsonNode data = SKLandPlayerBindingNode.get("data");
        int code = SKLandPlayerBindingNode.get("code").intValue();
        if (code != 0) throw new ServiceException(ResultCode.SKLAND_CRED_ERROR);
        JsonNode list = data.get("list");
        JsonNode bindingList = list.get(0).get("bindingList");
        String uid = bindingList.get(0).get("uid").asText();

        SurveyUser surveyUserByUid = surveyUserService.getSurveyUserByUid(Long.parseLong(uid));

        if(surveyUserByUid!=null){
            if(!Objects.equals(surveyUserByUid.getId(), surveyUser.getId())){
                Map<String,Object> hashMap = new HashMap<>();
                hashMap.put("userName",surveyUserByUid.getUserName());
                hashMap.put("registered",true);
                return hashMap;
            }

        }


        surveyUser.setUid(Long.parseLong(uid));

        String SKLandPlayerInfoAPI = ApplicationConfig.SKLandPlayerInfoAPI + uid;
        String SKLandPlayerInfo = HttpRequestUtil.get(SKLandPlayerInfoAPI, header);
        JsonNode SKLandPlayerInfoNode = JsonMapper.parseJSONObject(SKLandPlayerInfo);
        JsonNode data1 = SKLandPlayerInfoNode.get("data");
        JsonNode chars = data1.get("chars");
        JsonNode charInfoMap = data1.get("charInfoMap");

        for (int i = 0; i < chars.size(); i++) {
            SurveyOperator surveyOperator = new SurveyOperator();
            String charId = chars.get(i).get("charId").asText();
            int level = chars.get(i).get("level").intValue();
            int evolvePhase = chars.get(i).get("evolvePhase").intValue();
            int potentialRank = chars.get(i).get("potentialRank").intValue() + 1;
            int rarity = charInfoMap.get(charId).get("rarity").intValue() + 1;
            int mainSkillLvl = chars.get(i).get("mainSkillLvl").intValue();
            surveyOperator.setCharId(charId);
            surveyOperator.setOwn(true);
            surveyOperator.setLevel(level);
            surveyOperator.setElite(evolvePhase);
            surveyOperator.setRarity(rarity);
            surveyOperator.setMainSkill(mainSkillLvl);
            surveyOperator.setPotential(potentialRank);
            surveyOperator.setSkill1(-1);
            surveyOperator.setSkill2(-1);
            surveyOperator.setSkill3(-1);
            surveyOperator.setModX(-1);
            surveyOperator.setModY(-1);
            JsonNode skills = chars.get(i).get("skills");
            for (int j = 0; j < skills.size(); j++) {
                int specializeLevel = skills.get(j).get("specializeLevel").intValue();
                if (j == 0) {
                    surveyOperator.setSkill1(specializeLevel);
                }
                if (j == 1) {
                    surveyOperator.setSkill2(specializeLevel);
                }
                if (j == 2) {
                    surveyOperator.setSkill3(specializeLevel);
                }
            }
            JsonNode equip = chars.get(i).get("equip");
            String defaultEquipId = chars.get(i).get("defaultEquipId").asText();
            for (int j = 0; j < equip.size(); j++) {
                String id = equip.get(j).get("id").asText();
                if (id.contains("_001_")) continue;
                int equipLevel = equip.get(j).get("level").intValue();
                String type = equipDict.get(id).asText();
                if (defaultEquipId.equals(id)) {
                    if ("X".equals(type)) {
                        surveyOperator.setModX(equipLevel);
                    }
                    if ("Y".equals(type)) {
                        surveyOperator.setModY(equipLevel);
                    }
                }
                if (equipLevel > 1) {
                    if ("X".equals(type)) {
                        surveyOperator.setModX(equipLevel);
                    }
                    if ("Y".equals(type)) {
                        surveyOperator.setModY(equipLevel);
                    }
                }
            }

            surveyOperatorList.add(surveyOperator);
        }


        return updateSurveyData(surveyUser, surveyOperatorList);

    }


    /**
     * 上传maa干员信息
     * @param maaOperBoxVo  maa识别出的的结果
     * @param ipAddress   ip
     * @return 成功消息
     */
//    public HashMap<Object, Object> saveMaaCharData(MaaOperBoxVo maaOperBoxVo, String ipAddress) {
//        JSONArray operBoxs = maaOperBoxVo.getOperBox();
//        List<SurveyCharacter> surveyCharacterList = new ArrayList<>();
//
//        operBoxs.forEach(item -> {
//            OperBox operBox = JSONObject.parseObject(String.valueOf(item), OperBox.class);
//            SurveyCharacter build = SurveyCharacter.builder()
//                    .charId(operBox.getId())
//                    .level(operBox.getLevel())
//                    .elite(operBox.getElite())
//                    .potential(operBox.getPotential())
//                    .rarity(operBox.getRarity())
//                    .own(operBox.getOwn())
//                    .skill1(-1)
//                    .skill2(-1)
//                    .skill3(-1)
//                    .modX(-1)
//                    .modY(-1)
//                    .build();
//            surveyCharacterList.add(build);
//        });
//
//        SurveyUser surveyUser = surveyService.registerByMaa(ipAddress);
//        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
//        surveyUser.setUpdateTime(new Date());
//        surveyService.updateSurveyUser(surveyUser);
//
//        String tableName = "survey_character_"+ surveyService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表
//
//        Long uid = surveyUser.getId();
//        int insertRows = 0;
//        int updateRows = 0;
//
//
//        List<SurveyCharacter> surveyCharacterVos
//                = surveyCharacterMapper.selectSurveyCharacterByUid(tableName, surveyUser.getId());
//
//        Map<String, SurveyCharacter> surveyDataCharCollectById = surveyCharacterVos.stream()
//                .collect(Collectors.toMap(SurveyCharacter::getId, Function.identity()));
//
//        List<SurveyCharacter> insertList = new ArrayList<>();
//
//        for (SurveyCharacter surveyCharacter : surveyCharacterList) {
//            String id = uid + "_" + surveyCharacter.getCharId(); //存储id
////            if(!surveyDataChar.getOwn())  continue;  //未持有不记录
//
//            //精英化阶段小于2 不能专精和开模组
//            if (surveyCharacter.getElite() < 2 || !surveyCharacter.getOwn()) {
//                surveyCharacter.setSkill1(0);
//                surveyCharacter.setSkill2(0);
//                surveyCharacter.setSkill3(0);
//                surveyCharacter.setModX(0);
//                surveyCharacter.setModY(0);
//            }
//
////            SurveyDataChar surveyDataCharById = surveyMapper.selectSurveyDataCharById(charTable, id);
//            SurveyCharacter surveyCharacterById = surveyDataCharCollectById.get(id);
//            surveyCharacter.setId(id);
//            surveyCharacter.setUid(uid);
//
//            if (surveyCharacterById == null) {
//                insertList.add(surveyCharacter);
//                insertRows++;
//            } else {
//                if (surveyDataCharEquals(surveyCharacter, surveyCharacterById)) {
//                    updateRows++;
//                    surveyCharacterMapper.updateSurveyCharacterById(tableName, surveyCharacter);
//                }
//            }
//        }
//
//        if (insertList.size() > 0) surveyCharacterMapper.insertBatchSurveyCharacter(tableName, insertList);
//
//        HashMap<Object, Object> hashMap = new HashMap<>();
//        hashMap.put("updateRows", updateRows);
//        hashMap.put("insertRows", insertRows);
//        hashMap.put("uid", uid);
//        return hashMap;
//    }


}
