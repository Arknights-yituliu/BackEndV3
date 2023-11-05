package com.lhs.service.survey;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.util.Result;
import com.lhs.common.util.ResultCode;
import com.lhs.common.util.*;
import com.lhs.entity.po.survey.*;
import com.lhs.entity.vo.survey.OperatorExportExcelVO;
import com.lhs.entity.vo.survey.UserDataVO;
import com.lhs.mapper.survey.OperatorUploadLogMapper;
import com.lhs.mapper.survey.OperatorDataMapper;
import com.lhs.mapper.survey.OperatorDataVoMapper;
import com.lhs.service.util.OSSService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class OperatorDataService {


    private final OperatorDataMapper operatorDataMapper;
    private final OperatorDataVoMapper operatorDataVoMapper;

    private final SurveyUserService surveyUserService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final OSSService ossService;

    private final OperatorUploadLogMapper operatorUploadLogMapper;

    private final OperatorBaseDataService operatorBaseDataService;

    public OperatorDataService(OperatorDataMapper operatorDataMapper, OperatorDataVoMapper operatorDataVoMapper, SurveyUserService surveyUserService, RedisTemplate<String, Object> redisTemplate, OSSService ossService, OperatorUploadLogMapper operatorUploadLogMapper, OperatorBaseDataService operatorBaseDataService) {
        this.operatorDataMapper = operatorDataMapper;
        this.operatorDataVoMapper = operatorDataVoMapper;
        this.surveyUserService = surveyUserService;
        this.redisTemplate = redisTemplate;
        this.ossService = ossService;
        this.operatorUploadLogMapper = operatorUploadLogMapper;
        this.operatorBaseDataService = operatorBaseDataService;
    }

    /**
     * 通用的上传方法
     * @param surveyUser         用户信息
     * @param operatorDataList 干员练度调查表
     * @return
     */
    private Map<String, Object> saveOperatorData(SurveyUser surveyUser, List<OperatorData> operatorDataList) {

        long yituliuId = surveyUser.getId();

        Date date = new Date();
        String tableName = "survey_operator_" + surveyUserService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表

        int affectedRows = 0;


        //用户之前上传的数据
        QueryWrapper<OperatorData> lastQueryWrapper= new QueryWrapper<>();
        lastQueryWrapper.eq("uid",yituliuId);
        List<OperatorData> lastOperatorDataListData = operatorDataMapper.selectList(lastQueryWrapper);

        Map<String, OperatorData> lastOperatorDataMap = lastOperatorDataListData.stream()
                .collect(Collectors.toMap(OperatorData::getCharId, Function.identity()));

        //新增数据
        List<OperatorData> insertOperatorListData = new ArrayList<>();


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

//            System.out.println(surveyOperator);

            //和老数据进行对比
            OperatorData lastOperatorDataData = lastOperatorDataMap.get(operatorData.getCharId());
            //为空则新增

            if (lastOperatorDataData == null) {
                Long characterId = redisTemplate.opsForValue().increment("CharacterId");
                operatorData.setId(characterId);
                operatorData.setUid(yituliuId);
                insertOperatorListData.add(operatorData);  //加入批量插入集合
                affectedRows++;  //新增数据条数
            } else {
                //如果数据存在，进行更新
                affectedRows++;  //更新数据条数
                operatorData.setId(lastOperatorDataData.getId());
                operatorData.setUid(yituliuId);
                operatorDataMapper.updateByUid(tableName, operatorData); //更新数据
            }
        }

//        System.out.println(insertOperatorList.size());

        if (insertOperatorListData.size() > 0) operatorDataMapper.insertBatch(tableName, insertOperatorListData);  //批量插入


        surveyUser.setUpdateTime(date);   //更新用户最后一次上传时间

        surveyUserService.backupSurveyUser(surveyUser);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("affectedRows", affectedRows);
        hashMap.put("updateTime", simpleDateFormat.format(date));
        hashMap.put("registered",false);
        return hashMap;
    }

    /**
     * 手动上传干员练度调查表
     * @param token              token
     * @param operatorDataList 干员练度调查表单
     * @return 成功消息
     */
//    @TakeCount(name = "上传评分")
    public Map<String, Object> manualUploadOperator(String token, List<OperatorData> operatorDataList) {
        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        return saveOperatorData(surveyUser, operatorDataList);
    }

    /**
     * 导入森空岛干员练度数据
      * @param token 一图流凭证
     * @param dataStr 上传的json字符串
     * @return
     */
    public Result<Object> importSKLandPlayerInfoV2(String token, String dataStr) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        List<OperatorData> operatorDataList = new ArrayList<>();

        Map<String, String> uniEquipIdAndType = operatorBaseDataService.getEquipIdAndType();
        JsonNode data =  JsonMapper.parseJSONObject(dataStr);
//        String nickName = data.get("nickName").asText();
        String uid = data.get("uid").asText();

        Log.info("森空岛导入V2 {} uid："+uid);
        SurveyUser bindAccount = surveyUserService.getSurveyUserByUid(uid);

        if(bindAccount!=null){
            if(!Objects.equals(bindAccount.getId(), surveyUser.getId())){
                UserDataVO response = new UserDataVO();
                response.setUserName(bindAccount.getUserName());
                return Result.failure(ResultCode.USER_BIND_UID,response);
            }
        }


        surveyUser.setUid(uid);
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

        return Result.success(saveOperatorData(surveyUser, operatorDataList));
    }

    /**
     * 用户通过森空岛导入时绑定uid
     * @param surveyUser 调查站用户信息
     */
    private void userBindPlayerUid(SurveyUser surveyUser){
        QueryWrapper<OperatorUploadLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", surveyUser.getId());
        OperatorUploadLog operatorUploadLog = operatorUploadLogMapper.selectOne(queryWrapper);
        OperatorUploadLog surveyOperatorUploadLog = new OperatorUploadLog();
        surveyOperatorUploadLog.setId(surveyUser.getId());
        surveyOperatorUploadLog.setUserName(surveyUser.getUserName());
        surveyOperatorUploadLog.setIp(surveyUser.getIp());
        surveyOperatorUploadLog.setUid(surveyUser.getUid());
        surveyOperatorUploadLog.setLastTime(System.currentTimeMillis());
        surveyOperatorUploadLog.setDeleteFlag(surveyUser.getDeleteFlag());

        if(operatorUploadLog == null){
            operatorUploadLogMapper.insert(surveyOperatorUploadLog);
        }else {
            operatorUploadLogMapper.update(surveyOperatorUploadLog,queryWrapper);
        }


    }

    /**
     * 导入干员练度调查表
     *
     * @param file  Excel文件
     * @param token token
     * @return 成功消息
     */
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

    /**
     * 重置个人上传的干员数据
     * @param token 一图流凭证
     * @return
     */
    public Result<Object> operatorDataReset(String token){
        SurveyUser surveyUserByToken = surveyUserService.getSurveyUserByToken(token);
        Long resetId = redisTemplate.opsForValue().increment("resetId");
        surveyUserByToken.setUid("delete"+resetId);
        surveyUserService.backupSurveyUser(surveyUserByToken);
        QueryWrapper<OperatorData> queryWrapper = new QueryWrapper<>();
        Long id = surveyUserByToken.getId();
        queryWrapper.eq("uid", id);

        int delete = operatorDataMapper.delete(queryWrapper);

        return Result.success("重置了"+delete+"条数据");
    }


    /**
     * 找回用户填写的数据
     * @param token token
     * @return 成功消息
     */
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

    /**
     * 导出干员的数据
     * @param response 返回体
     * @param token 一图流凭证
     */
    public void exportSurveyOperatorForm(HttpServletResponse response, String token) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        //拿到这个用户的干员练度数据存在了哪个表

        //用户之前上传的数据
        QueryWrapper<OperatorData> queryWrapper= new QueryWrapper<>();
        queryWrapper.eq("uid",surveyUser.getId());
        List<OperatorData> operatorDataList =
                operatorDataMapper.selectList(queryWrapper);

        List<OperatorExportExcelVO> listVo = new ArrayList<>();


        List<OperatorTable> operatorInfo = operatorBaseDataService.getOperatorTable();

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



}
