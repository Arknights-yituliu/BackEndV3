package com.lhs.service.survey.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.po.survey.*;
import com.lhs.entity.vo.survey.OperatorExportExcelVO;
import com.lhs.mapper.survey.AkPlayerBindInfoMapper;
import com.lhs.mapper.survey.OperatorDataMapper;
import com.lhs.mapper.survey.OperatorDataVoMapper;
import com.lhs.service.survey.OperatorDataService;
import com.lhs.service.survey.SurveyUserService;
import com.lhs.service.util.ArknightsGameDataService;
import com.lhs.service.util.OSSService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OperatorDataServiceImpl implements OperatorDataService {

    private final OperatorDataMapper operatorDataMapper;
    private final OperatorDataVoMapper operatorDataVoMapper;

    private final SurveyUserService surveyUserService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final OSSService ossService;

    private final AkPlayerBindInfoMapper akPlayerBindInfoMapper;

    private final ArknightsGameDataService arknightsGameDataService;

    public OperatorDataServiceImpl(OperatorDataMapper operatorDataMapper, OperatorDataVoMapper operatorDataVoMapper, SurveyUserService surveyUserService, RedisTemplate<String, Object> redisTemplate, OSSService ossService, AkPlayerBindInfoMapper akPlayerBindInfoMapper, ArknightsGameDataService arknightsGameDataService) {
        this.operatorDataMapper = operatorDataMapper;
        this.operatorDataVoMapper = operatorDataVoMapper;
        this.surveyUserService = surveyUserService;
        this.redisTemplate = redisTemplate;
        this.ossService = ossService;
        this.akPlayerBindInfoMapper = akPlayerBindInfoMapper;
        this.arknightsGameDataService = arknightsGameDataService;
    }







//    @TakeCount(name = "上传评分")
    @Override
    public Map<String, Object> manualUploadOperator(String token, List<OperatorData> operatorDataList) {
        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        return saveOperatorData(surveyUser, operatorDataList);
    }


    @Override
    public Map<String, Object> importSKLandPlayerInfoV2(String token, String dataStr) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        List<OperatorData> operatorDataList = new ArrayList<>();

        Map<String, String> uniEquipIdAndType = arknightsGameDataService.getEquipIdAndType();
        JsonNode data =  JsonMapper.parseJSONObject(dataStr);
//        String nickName = data.get("nickName").asText();
        String akUid = data.get("uid").asText();

        LogUtil.info("森空岛导入V2 {} uid："+akUid);

        surveyUser.setAkUid(akUid);
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
                if(uniEquipIdAndType.get(id)==null) continue;
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
                }
            }
//            System.out.println(surveyOperator);
            operatorDataList.add(operatorData);
        }

        userBindPlayerUid(surveyUser);

        return saveOperatorData(surveyUser, operatorDataList);
    }


    /**
     * 用户通过森空岛导入时绑定uid
     * @param surveyUser 调查站用户信息
     */
    private void userBindPlayerUid(SurveyUser surveyUser){
        QueryWrapper<AkPlayerBindInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ak_uid", surveyUser.getAkUid());
        AkPlayerBindInfo akPlayerBindInfoByAkUid = akPlayerBindInfoMapper.selectOne(queryWrapper);
        AkPlayerBindInfo surveyAkPlayerBindInfo = new AkPlayerBindInfo();
        surveyAkPlayerBindInfo.setId(surveyUser.getId());
        surveyAkPlayerBindInfo.setUserName(surveyUser.getUserName());
        surveyAkPlayerBindInfo.setIp(surveyUser.getIp());
        surveyAkPlayerBindInfo.setAkUid(surveyUser.getAkUid());
        surveyAkPlayerBindInfo.setLastTime(System.currentTimeMillis());
        surveyAkPlayerBindInfo.setDeleteFlag(surveyUser.getDeleteFlag());

        if(akPlayerBindInfoByAkUid == null){
            queryWrapper.clear();
            queryWrapper.eq("id",surveyUser.getId());
            AkPlayerBindInfo akPlayerBindInfoByUid = akPlayerBindInfoMapper.selectOne(queryWrapper);
            if(akPlayerBindInfoByUid==null){
                akPlayerBindInfoMapper.insert(surveyAkPlayerBindInfo);
            }else {
                akPlayerBindInfoMapper.update(surveyAkPlayerBindInfo,queryWrapper);
            }
        }else {
            akPlayerBindInfoMapper.update(surveyAkPlayerBindInfo,queryWrapper);
        }
    }



    /**
     * 通用的上传方法
     * @param surveyUser         用户信息
     * @param operatorDataList 干员练度调查表
     * @return
     */
    private Map<String, Object> saveOperatorData(SurveyUser surveyUser, List<OperatorData> operatorDataList) {

        long userId = surveyUser.getId();

        Date date = new Date();
        String tableName = "survey_operator_" + surveyUserService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表

        int affectedRows = 0;


        //用户之前上传的数据
        QueryWrapper<OperatorData> lastQueryWrapper= new QueryWrapper<>();
        lastQueryWrapper.eq("uid",userId);
        List<OperatorData> lastOperatorDataListData = operatorDataMapper.selectList(lastQueryWrapper);

        //防止用户多次点击上传
        Boolean done = redisTemplate.opsForValue().setIfAbsent("SurveyOperatorInterval:"+surveyUser.getId(), "done", 5, TimeUnit.SECONDS);
        if(Boolean.FALSE.equals(done)){
            throw new ServiceException(ResultCode.OPERATION_INTERVAL_TOO_SHORT);
        }


        //用户上次保存的干员练度数据，如果有重复的干员数据进行删除
        Map<String, OperatorData> lastOperatorDataMap = new HashMap<>();
        for(OperatorData operatorData:lastOperatorDataListData){
            if(lastOperatorDataMap.get(operatorData.getCharId())!=null){
                operatorDataMapper.delete(new QueryWrapper<OperatorData>().eq("id",operatorData.getId()));
            }else {
                lastOperatorDataMap.put(operatorData.getCharId(),operatorData);
            }
        }


        //新增数据
        List<OperatorData> insertOperatorListData = new ArrayList<>();

        //循环上传的干员练度
        for (OperatorData operatorData : operatorDataList) {

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
                operatorData.setSkill3(0);
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

            //和老数据进行对比
            OperatorData lastOperatorDataData = lastOperatorDataMap.get(operatorData.getCharId());
            //为空则新增
            checkData(operatorData);
            if (lastOperatorDataData == null) {
                Long characterId = redisTemplate.opsForValue().increment("CharacterId");
                operatorData.setId(characterId);
                operatorData.setUid(userId);
                insertOperatorListData.add(operatorData);  //加入批量插入集合
                affectedRows++;  //新增数据条数
            } else {
                //如果数据存在，进行更新
                affectedRows++;  //更新数据条数
                operatorData.setId(lastOperatorDataData.getId());
                operatorData.setUid(userId);
                operatorDataMapper.updateByUid(tableName, operatorData); //更新数据
            }
        }

//        System.out.println(insertOperatorList.size());

        try {
            if (insertOperatorListData.size() > 0) operatorDataMapper.insertBatch(tableName, insertOperatorListData);  //批量插入
        } catch (Exception e) {
            redisTemplate.delete("SurveyOperatorInterval:"+surveyUser.getId());
            throw new RuntimeException(e);
        }

        //更新用户最后一次上传时间
        surveyUser.setUpdateTime(date);

        surveyUserService.backupSurveyUser(surveyUser);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("affectedRows", affectedRows);
        hashMap.put("updateTime", simpleDateFormat.format(date));
        hashMap.put("registered",false);
        return hashMap;
    }

    private Boolean checkData(OperatorData operatorData){

        if(operatorData.getMainSkill()==null)operatorData.setMainSkill(0);
        if(operatorData.getModD()==null)operatorData.setModD(0);

        return true;
    }

    @Override
    public Map<String, Object> importExcel(MultipartFile file, String token) {
        List<OperatorData> list = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), OperatorExportExcelVO.class, new AnalysisEventListener<OperatorExportExcelVO>() {
                public void invoke(OperatorExportExcelVO operatorExportExcelVo, AnalysisContext analysisContext) {
                    OperatorData operatorData = new OperatorData();
                    BeanUtils.copyProperties(operatorExportExcelVo, operatorData);
                    list.add(operatorData);
                }

                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                }
            }).sheet().doRead();

        } catch (IOException e) {
            e.printStackTrace();
        }

        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        return saveOperatorData(surveyUser, list);
    }


    @Override
    public Result<Object> operatorDataReset(String token){
        SurveyUser surveyUserByToken = surveyUserService.getSurveyUserByToken(token);
        surveyUserByToken.setAkUid("delete"+surveyUserByToken.getId());
        surveyUserService.backupSurveyUser(surveyUserByToken);
        QueryWrapper<OperatorData> queryWrapper = new QueryWrapper<>();
        Long id = surveyUserByToken.getId();
        queryWrapper.eq("uid", id);

        int delete = operatorDataMapper.delete(queryWrapper);

        return Result.success("重置了"+delete+"条数据");
    }



    @Override
    public List<OperatorDataVo> getOperatorForm(String token) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        List<OperatorDataVo> operatorDataVos = new ArrayList<>();
        if (surveyUser.getCreateTime().getTime() == surveyUser.getUpdateTime().getTime())
            return operatorDataVos;  //更新时间和注册时间一致直接返回空

        QueryWrapper<OperatorData> queryWrapper= new QueryWrapper<>();
        queryWrapper.eq("uid",surveyUser.getId());

        List<OperatorData> operatorDataList = operatorDataMapper.selectList(queryWrapper);

        operatorDataList.forEach(e -> {
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
                    .build();
            operatorDataVos.add(build);
        });

        return operatorDataVos;
    }

    @Override
    public void exportSurveyOperatorForm(HttpServletResponse response, String token) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        //拿到这个用户的干员练度数据存在了哪个表

        //用户之前上传的数据
        QueryWrapper<OperatorData> queryWrapper= new QueryWrapper<>();
        queryWrapper.eq("uid",surveyUser.getId());
        List<OperatorData> operatorDataList =
                operatorDataMapper.selectList(queryWrapper);

        List<OperatorExportExcelVO> listVo = new ArrayList<>();


        List<OperatorTable> operatorInfo = arknightsGameDataService.getOperatorTable();

        Map<String, OperatorTable> operatorTableMap = operatorInfo.stream()
                .collect(Collectors.toMap(OperatorTable::getCharId, Function.identity()));

        for(OperatorData operatorData : operatorDataList){
            OperatorExportExcelVO operatorExportExcelVo = new OperatorExportExcelVO();
            operatorExportExcelVo.copy(operatorData);
            String name = operatorTableMap.get(operatorData.getCharId())==null?"未录入":operatorTableMap.get(operatorData.getCharId()).getName();
            operatorExportExcelVo.setName(name);
            listVo.add(operatorExportExcelVo);
        }

        String userName = surveyUser.getUserName();

        ExcelUtil.exportExcel(response, listVo, OperatorExportExcelVO.class, userName);
    }



    @Override
    public List<Map<String,Object>> operatorDataDuplicateDistinct() {
        List<SurveyUser> surveyUserList = surveyUserService.getAllUserData();

        List<Map<String,Object>> resultList = new ArrayList<>();
        for (SurveyUser surveyUser : surveyUserList) {
            if (surveyUser.getAkUid() != null && !(surveyUser.getAkUid().startsWith("delete"))) {

                List<OperatorData> operatorDataList = operatorDataMapper.selectList(new QueryWrapper<OperatorData>().eq("uid", surveyUser.getId()));

                int count = 0;
                boolean distinctFlag = false;
                Map<String, OperatorData> hashMap = new HashMap<>();
                for (OperatorData operatorData : operatorDataList) {
                    if (hashMap.get(operatorData.getCharId()) != null) {
                        distinctFlag = true;
                        count += operatorDataMapper.delete(new QueryWrapper<OperatorData>().eq("id", operatorData.getId()));
                    } else {
                        hashMap.put(operatorData.getCharId(), operatorData);
                    }
                }
                Map<String, Object> result = new HashMap<>();
                result.put("userName",surveyUser.getUserName());
                result.put("distinct",+ count + "条");
                resultList.add(result);
            }
        }

        return resultList;
    }



}
