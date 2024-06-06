package com.lhs.service.survey.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.po.survey.*;
import com.lhs.entity.vo.survey.OperatorExportExcelVO;

import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.survey.AkPlayerBindInfoV2Mapper;
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

    private final ArknightsGameDataService arknightsGameDataService;

    private final AkPlayerBindInfoV2Mapper akPlayerBindInfoV2Mapper;

    private final IdGenerator idGenerator;

    public OperatorDataServiceImpl(OperatorDataMapper operatorDataMapper,
                                   OperatorDataVoMapper operatorDataVoMapper,
                                   SurveyUserService surveyUserService,
                                   RedisTemplate<String, Object> redisTemplate, OSSService ossService,
                                   ArknightsGameDataService arknightsGameDataService,
                                   AkPlayerBindInfoV2Mapper akPlayerBindInfoV2Mapper) {
        this.operatorDataMapper = operatorDataMapper;
        this.operatorDataVoMapper = operatorDataVoMapper;
        this.surveyUserService = surveyUserService;
        this.redisTemplate = redisTemplate;
        this.ossService = ossService;
        this.arknightsGameDataService = arknightsGameDataService;
        this.akPlayerBindInfoV2Mapper = akPlayerBindInfoV2Mapper;
        this.idGenerator = new IdGenerator(1L);
    }


    //    @TakeCount(name = "上传评分")
    @Override
    public Map<String, Object> manualUploadOperator(String token, List<OperatorData> operatorDataList) {
        UserInfoVO userInfo = surveyUserService.getUserInfo(token);
        return saveOperatorData(userInfo, operatorDataList);
    }


    @Override
    public Map<String, Object> importSKLandPlayerInfoV2(String token, String dataStr) {

        UserInfoVO userInfo = surveyUserService.getUserInfo(token);

        List<OperatorData> operatorDataList = new ArrayList<>();


        Map<String, String> uniEquipIdAndType = arknightsGameDataService.getEquipIdAndType();
        JsonNode data = JsonMapper.parseJSONObject(dataStr);
        String akNickName = userInfo.getUserName();
        String akUid = data.get("akUid").asText();
        String channelName = "无服务器";
        int channelMasterId = 1;

        Logger.info("服务器：" + data.get("channelName"));


        if (data.get("akNickName") != null) {
            akNickName = data.get("akNickName").asText();
        }

        if (data.get("channelName") != null) {
            channelName = data.get("channelName").asText();
        }

        if (data.get("channelMasterId") != null) {
            channelMasterId = data.get("channelMasterId").asInt();
        }


        Logger.info("森空岛导入.ver2 {} uid：" + akUid);

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

        AkPlayerBindInfoV2 newAkPlayerBindInfo = new AkPlayerBindInfoV2();
        newAkPlayerBindInfo.setId(idGenerator.nextId());
        newAkPlayerBindInfo.setUid(userInfo.getUid());
        newAkPlayerBindInfo.setAkNickName(akNickName);
        newAkPlayerBindInfo.setAkUid(akUid);
        newAkPlayerBindInfo.setLastActiveTime(System.currentTimeMillis());
        newAkPlayerBindInfo.setDeleteFlag(false);
        newAkPlayerBindInfo.setDefaultFlag(true);
        newAkPlayerBindInfo.setChannelName(channelName);
        newAkPlayerBindInfo.setChannelMasterId(channelMasterId);

        userBindPlayerUid(newAkPlayerBindInfo);

        userInfo.setAkUid(akUid);

        return saveOperatorData(userInfo, operatorDataList);
    }


    /**
     * 用户通过森空岛导入时绑定明日方舟玩家uid
     *
     * @param newAkPlayerBindInfo 明日方舟玩家信息
     */
    private void userBindPlayerUid(AkPlayerBindInfoV2 newAkPlayerBindInfo) {

        QueryWrapper<AkPlayerBindInfoV2> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("uid", newAkPlayerBindInfo.getUid());

        List<AkPlayerBindInfoV2> akPlayerBindInfoV2List = akPlayerBindInfoV2Mapper.selectList(userQueryWrapper);


        //该账号名下没有绑定明日方舟uid，新建一条绑定数据
        if (akPlayerBindInfoV2List == null) {
            akPlayerBindInfoV2Mapper.insert(newAkPlayerBindInfo);
            return;
        }

        //是否已经有绑定账号
        boolean isBindingAkAccount = false;
        //查看该账号名下绑定的方舟uid是否和当前导入的方舟uid一致，一致则更新
        for (AkPlayerBindInfoV2 oldBindInfo : akPlayerBindInfoV2List) {
            if (oldBindInfo.getAkUid().equals(newAkPlayerBindInfo.getAkUid())) {
                newAkPlayerBindInfo.setId(oldBindInfo.getId());
                isBindingAkAccount = true;
                break;
            }
        }


        if (isBindingAkAccount) {
            int i = akPlayerBindInfoV2Mapper.updateById(newAkPlayerBindInfo);
        } else {
            //如果名下绑定的方舟uid都不符合，新增一条
            akPlayerBindInfoV2Mapper.insert(newAkPlayerBindInfo);
        }

        //将该账号名下，所有除当前方舟uid之外其他方舟uid的默认标记设为false
        LambdaUpdateWrapper<AkPlayerBindInfoV2> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AkPlayerBindInfoV2::getUid, newAkPlayerBindInfo.getUid())
                .ne(AkPlayerBindInfoV2::getAkUid, newAkPlayerBindInfo.getAkUid())
                .set(AkPlayerBindInfoV2::getDefaultFlag, false);
        int updateRows = akPlayerBindInfoV2Mapper.update(null, updateWrapper);
    }


    private Map<String, OperatorData> getLastOperatorData(UserInfoVO userInfoVO) {

        //查询用户的干员数据，条件为账号的uid和默认的方舟uid
        QueryWrapper<OperatorData> lastQueryWrapper = new QueryWrapper<>();
        lastQueryWrapper.eq("uid", userInfoVO.getUid())
                .eq("ak_uid", userInfoVO.getAkUid());
        //用户的干员数据
        List<OperatorData> lastOperatorDataListData = operatorDataMapper.selectList(lastQueryWrapper);

        //将集合转为map，方便对比
        Map<String, OperatorData> lastOperatorDataMap = new HashMap<>();
        for (OperatorData operatorData : lastOperatorDataListData) {
            //如果map中已经存在这个干员的id，说明有数据重复，删除该条数据
            if (lastOperatorDataMap.get(operatorData.getCharId()) != null) {
                QueryWrapper<OperatorData> idQueryWrapper = new QueryWrapper<OperatorData>()
                        .eq("id", operatorData.getId());
                operatorDataMapper.delete(idQueryWrapper);
            } else {  //如果是森空岛数据，先将map中的数据全部设为false
                lastOperatorDataMap.put(operatorData.getCharId(), operatorData);
            }
        }

        return lastOperatorDataMap;
    }


    /**
     * 保存干员数据
     *
     * @param userInfo         用户信息
     * @param operatorDataList 干员练度调查表
     * @return 成功信息
     */
    private HashMap<String, Object> saveOperatorData(UserInfoVO userInfo, List<OperatorData> operatorDataList) {
        //防止用户多次点击上传
        Boolean done = redisTemplate.opsForValue().setIfAbsent("SurveyOperatorInterval:" + userInfo.getUid(), "done", 5, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(done)) {
            throw new ServiceException(ResultCode.OPERATION_INTERVAL_TOO_SHORT);
        }

        //用户唯一id
        long surveyUserId = userInfo.getUid();
        //本次修改影响的数据行数
        int affectedRows = 0;
        //获取用户的干员练度数据在几号表
        String tableName = "survey_operator_" + surveyUserService.getTableIndex(surveyUserId);
        //新增数据
        List<OperatorData> insertOperatorDataList = new ArrayList<>();

        Map<String, OperatorData> lastOperatorDataMap = getLastOperatorData(userInfo);
        Logger.info("用户上次上传的森空岛干员数据" + lastOperatorDataMap.size() + "条；来自森空岛干员数据" + operatorDataList.size() + "条；本次导入的方舟uid为：" + userInfo.getAkUid());

        //循环上传的干员练度
        for (OperatorData operatorData : operatorDataList) {

            //和老数据进行对比
            OperatorData lastOperatorDataData = lastOperatorDataMap.get(operatorData.getCharId());
            //为空则新增
            //更新数据条数
            operatorData.setOwn(true);
            operatorData.setAkUid(userInfo.getAkUid());
            operatorData.setUid(surveyUserId);

            checkOperatorDataValidity(operatorData);

            if (lastOperatorDataData == null) {
                operatorData.setId(idGenerator.nextId());
                insertOperatorDataList.add(operatorData);  //加入批量插入集合
            } else {
                //如果数据存在，进行更新
                operatorData.setId(lastOperatorDataData.getId());
                protectManuallyUploadedData(operatorData, lastOperatorDataData);
                operatorDataMapper.updateByUid(tableName, operatorData); //更新数据
            }
            affectedRows++;  //新增数据条数
        }

        try {
            if (!insertOperatorDataList.isEmpty()) {
                operatorDataMapper.insertBatch(tableName, insertOperatorDataList);  //批量插入
            }
        } catch (Exception e) {
            redisTemplate.delete("SurveyOperatorInterval:" + surveyUserId);
            throw new RuntimeException(e);
        }

        Date date = new Date();
        //更新用户最后一次上传时间
//        surveyUser.setUpdateTime(date);
//
//        surveyUserService.backupSurveyUser(surveyUser);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("affectedRows", affectedRows);
        hashMap.put("updateTime", simpleDateFormat.format(date));
        hashMap.put("registered", false);
        return hashMap;
    }


    private void protectManuallyUploadedData(OperatorData newOperatorData, OperatorData oldOperatorData) {
        //检查干员旧数据的模组等级大于新导入的模组等级时，保留旧数据的等级
        if (oldOperatorData.getModX() > newOperatorData.getModX()) {
            newOperatorData.setModX(oldOperatorData.getModX());
            Logger.info(newOperatorData.getCharId() + "X模组被手动设置等级了");
        }
        if (oldOperatorData.getModY() > newOperatorData.getModY()) {
            newOperatorData.setModY(oldOperatorData.getModY());
            Logger.info(newOperatorData.getCharId() + "Y模组被手动设置等级了");
        }
        if (oldOperatorData.getModD() > newOperatorData.getModD()) {
            newOperatorData.setModD(oldOperatorData.getModD());
            Logger.info(newOperatorData.getCharId() + "D模组被手动设置等级了");
        }
    }

    /**
     * 对新老干员数据进行检查，是否有非法数据
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
            if(!operatorData.getCharId().contains("amiya")){
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
            Logger.error(e.getMessage());
        }


        return saveOperatorData(surveyUserService.getUserInfo(token), list);
    }


    @Override
    public Result<Object> operatorDataReset(String token) {
        UserInfoVO userInfoVO = surveyUserService.getUserInfo(token);

        QueryWrapper<OperatorData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", userInfoVO.getUid());

        int delete = operatorDataMapper.delete(queryWrapper);

        return Result.success("重置了" + delete + "条数据");
    }


    @Override
    public List<OperatorDataVo> getOperatorTable(String token) {

        //查询用户信息
        UserInfoVO userInfo = surveyUserService.getUserInfo(token);
        Logger.info("用户uid：" + userInfo.getUid() + "；方舟uid：" + userInfo.getAkUid());
        //保存的干员数据
        List<OperatorDataVo> operatorDataVoList = new ArrayList<>();
        //查询当前用户的默认方舟uid的干员数据
        QueryWrapper<OperatorData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", userInfo.getUid())
                .eq("ak_uid", userInfo.getAkUid());
        List<OperatorData> operatorDataList = operatorDataMapper.selectList(queryWrapper);


        if (operatorDataList == null || operatorDataList.isEmpty()) {
            QueryWrapper<OperatorData> queryWrapperNew = new QueryWrapper<>();
            queryWrapperNew.eq("uid", userInfo.getUid())
                    .eq("ak_uid", "0");
            operatorDataList = operatorDataMapper.selectList(queryWrapperNew);
        }

        if (operatorDataList == null || operatorDataList.isEmpty()) {
            return operatorDataVoList;
        }

        //转换为前端要展示的格式
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
            operatorDataVoList.add(build);
        });

        return operatorDataVoList;
    }

    @Override
    public void exportSurveyOperatorForm(HttpServletResponse response, String token) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        //拿到这个用户的干员练度数据存在了哪个表

        //用户之前上传的数据
        QueryWrapper<OperatorData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", surveyUser.getId());
        List<OperatorData> operatorDataList =
                operatorDataMapper.selectList(queryWrapper);

        List<OperatorExportExcelVO> listVo = new ArrayList<>();


        List<OperatorTable> operatorInfo = arknightsGameDataService.getOperatorTable();

        Map<String, OperatorTable> operatorTableMap = operatorInfo.stream()
                .collect(Collectors.toMap(OperatorTable::getCharId, Function.identity()));

        for (OperatorData operatorData : operatorDataList) {
            OperatorExportExcelVO operatorExportExcelVo = new OperatorExportExcelVO();
            operatorExportExcelVo.copy(operatorData);
            String name = operatorTableMap.get(operatorData.getCharId()) == null ? "未录入" : operatorTableMap.get(operatorData.getCharId()).getName();
            operatorExportExcelVo.setName(name);
            listVo.add(operatorExportExcelVo);
        }

        String userName = surveyUser.getUserName();

        ExcelUtil.exportExcel(response, listVo, OperatorExportExcelVO.class, userName);
    }


}
