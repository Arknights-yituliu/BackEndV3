package com.lhs.service.survey.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.survey.PlayerInfoDTO;
import com.lhs.entity.dto.user.AkPlayerBindInfoDTO;
import com.lhs.entity.po.survey.*;
import com.lhs.entity.po.user.AkPlayerBindInfo;

import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.survey.AkPlayerBindInfoV2Mapper;
import com.lhs.mapper.survey.OperatorDataMapper;
import com.lhs.mapper.survey.SurveyOperatorDataMapper;
import com.lhs.mapper.survey.OperatorDataVoMapper;
import com.lhs.service.survey.OperatorDataService;
import com.lhs.service.user.UserService;
import com.lhs.service.util.ArknightsGameDataService;
import com.lhs.service.util.OSSService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class OperatorDataServiceImpl implements OperatorDataService {

    private final SurveyOperatorDataMapper surveyOperatorDataMapper;
    private final OperatorDataVoMapper operatorDataVoMapper;


    private final RedisTemplate<String, Object> redisTemplate;

    private final OSSService ossService;

    private final ArknightsGameDataService arknightsGameDataService;

    private final AkPlayerBindInfoV2Mapper akPlayerBindInfoV2Mapper;

    private final UserService userService;

    private final IdGenerator idGenerator;

    private final OperatorDataMapper operatorDataMapper;

    public OperatorDataServiceImpl(SurveyOperatorDataMapper surveyOperatorDataMapper,
                                   OperatorDataVoMapper operatorDataVoMapper,
                                   RedisTemplate<String, Object> redisTemplate, OSSService ossService,
                                   ArknightsGameDataService arknightsGameDataService,
                                   AkPlayerBindInfoV2Mapper akPlayerBindInfoV2Mapper,
                                   UserService userService, OperatorDataMapper operatorDataMapper) {
        this.surveyOperatorDataMapper = surveyOperatorDataMapper;
        this.operatorDataVoMapper = operatorDataVoMapper;
        this.redisTemplate = redisTemplate;
        this.ossService = ossService;
        this.arknightsGameDataService = arknightsGameDataService;
        this.akPlayerBindInfoV2Mapper = akPlayerBindInfoV2Mapper;
        this.userService = userService;
        this.operatorDataMapper = operatorDataMapper;
        this.idGenerator = new IdGenerator(1L);
    }


    //    @TakeCount(name = "上传评分")
    @Override
    public Map<String, Object> manualUploadOperator(String token, List<OperatorData> surveyOperatorDataList) {
        UserInfoVO userInfo = userService.getUserInfoVOByToken(token);
        String akUid = String.valueOf(userInfo.getUid());
        if(userInfo.getAkUid()!=null){
            akUid = userInfo.getAkUid();
        }
        return saveOperatorData(String.valueOf(akUid), surveyOperatorDataList);
    }


    @Override
    public Map<String, Object> importSKLandPlayerInfoV2(String token, String dataStr) {

        //防止用户多次点击上传
        Boolean done = redisTemplate.opsForValue().setIfAbsent("SurveyOperatorInterval:" + token, "done", 5, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(done)) {
            throw new ServiceException(ResultCode.NOT_REPEAT_REQUESTS);
        }

        UserInfoVO userInfo = userService.getUserInfoVOByToken(token);

        List<OperatorData> operatorDataList = new ArrayList<>();


        Map<String, String> uniEquipIdAndType = arknightsGameDataService.getEquipIdAndType();
        JsonNode data = JsonMapper.parseJSONObject(dataStr);
        String akNickName = userInfo.getUserName();
        String akUid = data.get("akUid").asText();
        String channelName = "无服务器";
        int channelMasterId = 0;

        if (data.get("akNickName") != null) {
            akNickName = data.get("akNickName").asText();
        }

        if (data.get("channelName") != null) {
            channelName = data.get("channelName").asText();
        }

        if (data.get("channelMasterId") != null) {
            channelMasterId = data.get("channelMasterId").asInt();
        }

        Logger.info("森空岛导入v2 {} uid：" + akUid + "，服务器：" + data.get("channelName"));

        JsonNode chars = data.get("chars");
        JsonNode charInfoMap = data.get("charInfoMap");


        for (int i = 0; i < chars.size(); i++) {
            OperatorData operatorData = new OperatorData();
            String charId = chars.get(i).get("charId").asText();
            int level = chars.get(i).get("level").intValue();
            int evolvePhase = chars.get(i).get("evolvePhase").intValue();
            int potentialRank = chars.get(i).get("potentialRank").intValue() + 1;
            int rarity = charInfoMap.get(charId).get("rarity").intValue() + 1;
            int mainSkillLvl = chars.get(i).get("mainSkillLvl").intValue();
            operatorData.setCharId(charId);
            operatorData.setOwn(true);
            operatorData.setLevel(level);
            operatorData.setElite(evolvePhase);
            operatorData.setRarity(rarity);
            operatorData.setMainSkill(mainSkillLvl);
            operatorData.setPotential(potentialRank);
            operatorData.setSkill1(0);
            operatorData.setSkill2(0);
            operatorData.setSkill3(0);
            operatorData.setModX(0);
            operatorData.setModY(0);
            operatorData.setModD(0);
            operatorData.setModA(0);

            JsonNode skills = chars.get(i).get("skills");
            for (int j = 0; j < skills.size(); j++) {
                int specializeLevel = skills.get(j).get("specializeLevel").intValue();
                if (j == 0) {
                    operatorData.setSkill1(specializeLevel);
                }
                if (j == 1) {
                    operatorData.setSkill2(specializeLevel);
                }
                if (j == 2) {
                    operatorData.setSkill3(specializeLevel);
                }
            }

            JsonNode equip = chars.get(i).get("equip");
            String defaultEquipId = chars.get(i).get("defaultEquipId").asText();
            for (int j = 0; j < equip.size(); j++) {
                String id = equip.get(j).get("id").asText();
                if (id.contains("_001_")) continue;
                int equipLevel = equip.get(j).get("level").intValue();
                if (uniEquipIdAndType.get(id) == null) continue;
                String type = uniEquipIdAndType.get(id);
                if (defaultEquipId.equals(id)) {
                    if ("X".equals(type)) {
                        operatorData.setModX(equipLevel);
                    }
                    if ("Y".equals(type)) {
                        operatorData.setModY(equipLevel);
                    }
                    if ("D".equals(type)) {
                        operatorData.setModD(equipLevel);
                    }
                    if ("A".equals(type)) {
                        operatorData.setModA(equipLevel);
                    }
                }
                if (equipLevel > 1) {
                    if ("X".equals(type)) {
                        operatorData.setModX(equipLevel);
                    }
                    if ("Y".equals(type)) {
                        operatorData.setModY(equipLevel);
                    }
                    if ("D".equals(type)) {
                        operatorData.setModD(equipLevel);
                    }
                    if ("A".equals(type)) {
                        operatorData.setModA(equipLevel);
                    }
                }
            }
            operatorDataList.add(operatorData);
        }

        AkPlayerBindInfoDTO akPlayerBindInfoDTO = new AkPlayerBindInfoDTO();
        akPlayerBindInfoDTO.setAkNickName(akNickName);
        akPlayerBindInfoDTO.setAkUid(akUid);
        akPlayerBindInfoDTO.setChannelName(channelName);
        akPlayerBindInfoDTO.setChannelMasterId(channelMasterId);


        userService.saveBindInfo(userInfo, akPlayerBindInfoDTO);

        userInfo.setAkUid(akUid);

        return saveOperatorData(akUid, operatorDataList);
    }


    @Override
    public Object importSKLandPlayerInfoV3(PlayerInfoDTO playerInfoDTO) {

        String token = playerInfoDTO.getToken();

        //防止用户多次点击上传
        Boolean done = redisTemplate.opsForValue().setIfAbsent("SurveyOperatorInterval:" + token, "done", 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(done)) {
            throw new ServiceException(ResultCode.NOT_REPEAT_REQUESTS);
        }

        UserInfoVO userInfo = userService.getUserInfoVOByToken(token);
        List<OperatorData> operatorDataList = playerInfoDTO.getOperatorDataList();

        String akUid = playerInfoDTO.getUid();

        AkPlayerBindInfoDTO akPlayerBindInfoDTO = new AkPlayerBindInfoDTO();
        akPlayerBindInfoDTO.setAkNickName(playerInfoDTO.getNickName());
        akPlayerBindInfoDTO.setAkUid(akUid);
        akPlayerBindInfoDTO.setChannelName(playerInfoDTO.getChannelName());
        akPlayerBindInfoDTO.setChannelMasterId(playerInfoDTO.getChannelMasterId());
        userService.saveBindInfo(userInfo, akPlayerBindInfoDTO);
        userInfo.setAkUid(akUid);

        return saveOperatorData(akUid, operatorDataList);
    }

    @Override
    public Object operatorDataReport() {


        return null;
    }

    /**
     * 保存干员数据
     *
     * @param akUid  明日方舟玩家uid
     * @param surveyOperatorDataList 干员练度调查表
     * @return 成功信息
     */
    private Map<String, Object> saveOperatorData(String akUid, List<OperatorData> surveyOperatorDataList) {

        //本次修改影响的数据行数
        int affectedRows = 0;

        //新增数据
        List<OperatorData> insertSurveyOperatorDataList = new ArrayList<>();

        Map<String, OperatorData> lastOperatorDataMap = getLastOperatorDataMap(akUid);


        //循环上传的干员练度
        for (OperatorData operatorData : surveyOperatorDataList) {

            //和老数据进行对比
            OperatorData lastSurveyOperatorDataData = lastOperatorDataMap.get(operatorData.getCharId());
            //为空则新增
            //更新数据条数
            operatorData.setOwn(true);
            operatorData.setAkUid(akUid);

            checkOperatorDataValidity(operatorData);

            if (lastSurveyOperatorDataData == null) {
                operatorData.setId(idGenerator.nextId());
                insertSurveyOperatorDataList.add(operatorData);  //加入批量插入集合
            } else {
                //如果数据存在，进行更新
                operatorData.setId(lastSurveyOperatorDataData.getId());
                protectManuallyUploadedData(operatorData, lastSurveyOperatorDataData);
                operatorDataMapper.updateById(operatorData); //更新数据
            }
            affectedRows++;  //新增数据条数
        }


        if (!insertSurveyOperatorDataList.isEmpty()) {
            operatorDataMapper.insertBatch(insertSurveyOperatorDataList);  //批量插入
        }


        Date date = new Date();
        //更新用户最后一次上传时间
//        surveyUser.setUpdateTime(date);
//
//        surveyUserService.backupSurveyUser(surveyUser);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("affectedRows", affectedRows);
        hashMap.put("updateTime", simpleDateFormat.format(date));
        hashMap.put("registered", false);
        return hashMap;
    }






    /**
     *
     * @param akUid 明日方舟玩家uid
     * @return 上次保存的干员数据
     */
    private Map<String, OperatorData> getLastOperatorDataMap(String akUid) {

        //查询用户的干员数据，条件为账号的uid和默认的方舟uid
        LambdaQueryWrapper<OperatorData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperatorData::getAkUid,akUid);
        //用户的干员数据
        List<OperatorData> lastSurveyOperatorDataListData = operatorDataMapper.selectList(queryWrapper);

        //将集合转为map，方便对比
        Map<String, OperatorData> lastOperatorDataMap = new HashMap<>();
        for (OperatorData surveyOperatorData : lastSurveyOperatorDataListData) {
            //如果map中已经存在这个干员的id，说明有数据重复，删除该条数据
            if (lastOperatorDataMap.get(surveyOperatorData.getCharId()) != null) {
                LambdaQueryWrapper<OperatorData> idQueryWrapper = new LambdaQueryWrapper<>();
                idQueryWrapper.eq(OperatorData::getId, surveyOperatorData.getId());
                operatorDataMapper.delete(idQueryWrapper);
            } else {  //如果是森空岛数据，先将map中的数据全部设为false
                lastOperatorDataMap.put(surveyOperatorData.getCharId(), surveyOperatorData);
            }
        }

        return lastOperatorDataMap;
    }

    private void protectManuallyUploadedData(OperatorData newData, OperatorData lastData) {
        //检查干员旧数据的模组等级大于新导入的模组等级时，保留旧数据的等级
        if (lastData.getModX() > newData.getModX()) {
            newData.setModX(lastData.getModX());
            Logger.info(newData.getCharId() + "X模组被手动设置等级了");
        }
        if (lastData.getModY() > newData.getModY()) {
            newData.setModY(lastData.getModY());
            Logger.info(newData.getCharId() + "Y模组被手动设置等级了");
        }
        if (lastData.getModD() > newData.getModD()) {
            newData.setModD(lastData.getModD());
            Logger.info(newData.getCharId() + "D模组被手动设置等级了");
        }
    }

    /**
     * 对新老干员数据进行检查，是否有非法数据
     *
     * @param operatorData 新干员数据
     */
    private void checkOperatorDataValidity(OperatorData operatorData) {

        //精英化阶段小于2 不能专精和开模组
        if (operatorData.getElite() < 2) {
            operatorData.setSkill1(0);
            operatorData.setSkill2(0);
            operatorData.setSkill3(0);
            operatorData.setModX(0);
            operatorData.setModY(0);
            operatorData.setModD(0);
        }

        if (operatorData.getRarity() < 6) {
            if (!operatorData.getCharId().contains("amiya")) {
                operatorData.setSkill3(0);
            }
        }

        if (!operatorData.getOwn()) {
            operatorData.setMainSkill(0);
            operatorData.setPotential(0);
            operatorData.setSkill1(0);
            operatorData.setSkill2(0);
            operatorData.setSkill3(0);
            operatorData.setModX(0);
            operatorData.setModY(0);
            operatorData.setModD(0);
        }

        if (operatorData.getMainSkill() == null) {
            operatorData.setMainSkill(1);
        }


        if (operatorData.getModD() == null) {
            operatorData.setModD(0);
        }


    }


    @Override
    public Result<Object> operatorDataReset(String token) {


        return Result.success("重置了" + "条数据");
    }


    @Override
    public List<OperatorDataVo> getOperatorInfoByToken(String token) {

        //查询用户信息
        UserInfoVO userInfo = userService.getUserInfoVOByToken(token);
        Logger.info("用户uid：" + userInfo.getUid() + "；方舟uid：" + userInfo.getAkUid());
        //保存的干员数据
        List<OperatorDataVo> operatorDataVoList = new ArrayList<>();
        //查询当前用户的默认方舟uid的干员数据
        LambdaQueryWrapper<OperatorData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperatorData::getAkUid, userInfo.getAkUid());
        List<OperatorData> surveyOperatorDataList = operatorDataMapper.selectList(queryWrapper);

        if (surveyOperatorDataList == null || surveyOperatorDataList.isEmpty()) {
            return operatorDataVoList;
        }

        //转换为前端要展示的格式
        surveyOperatorDataList.forEach(e -> {
            OperatorDataVo build = OperatorDataVo.builder()
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
                    .modD(e.getModD())
                    .modA(e.getModA())
                    .build();
            operatorDataVoList.add(build);
        });

        return operatorDataVoList;
    }

    @Override
    public Map<String, Object> saveOperatorDataByRhodes(PlayerInfoDTO playerInfoDTO) {

        String uid = playerInfoDTO.getUid();
        String nickName = playerInfoDTO.getNickName();
        String channelName = playerInfoDTO.getChannelName();
        Integer channelMasterId = playerInfoDTO.getChannelMasterId();
        List<OperatorData> operatorDataList = playerInfoDTO.getOperatorDataList();

        AkPlayerBindInfo akPlayerBindInfo = new AkPlayerBindInfo();
        akPlayerBindInfo.setId(idGenerator.nextId());
        akPlayerBindInfo.setAkUid(uid);
        akPlayerBindInfo.setAkNickName(nickName);
        akPlayerBindInfo.setChannelName(channelName);
        akPlayerBindInfo.setChannelMasterId(channelMasterId);
        userService.saveAkPlayerBindInfo(akPlayerBindInfo);

        return saveOperatorData(uid, operatorDataList);
    }




}
