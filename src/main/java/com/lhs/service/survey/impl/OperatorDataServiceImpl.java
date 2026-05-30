package com.lhs.service.survey.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.survey.OperatorProgressionDataDTO;
import com.lhs.entity.dto.survey.PlayerInfoDTO;
import com.lhs.entity.dto.user.AkPlayerBindInfoDTO;
import com.lhs.entity.po.survey.*;

import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.survey.OperatorProgressionDataMapper;
import com.lhs.service.survey.OperatorDataService;
import com.lhs.service.survey.WarehouseInfoService;
import com.lhs.service.user.UserService;
import com.lhs.service.util.TencentCloudService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class OperatorDataServiceImpl implements OperatorDataService {


    private final RedisTemplate<String, Object> redisTemplate;

    private final UserService userService;

    private final IdGenerator idGenerator;

    private final OperatorProgressionDataMapper operatorProgressionDataMapper;

    private final WarehouseInfoService warehouseInfoService;


    private final TencentCloudService tencentCloudService;

    public OperatorDataServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                   UserService userService, OperatorProgressionDataMapper operatorProgressionDataMapper, WarehouseInfoService warehouseInfoService, TencentCloudService tencentCloudService) {
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        this.operatorProgressionDataMapper = operatorProgressionDataMapper;
        this.warehouseInfoService = warehouseInfoService;
        this.tencentCloudService = tencentCloudService;
        this.idGenerator = new IdGenerator(1L);
    }



    @Override
    public Object importSKLandPlayerInfoV3(HttpServletRequest httpServletRequest,PlayerInfoDTO playerInfoDTO) {

        UserInfoVO userInfo = userService.getUserInfoVOByHttpServletRequest(httpServletRequest);

        //防止用户多次点击上传
        Boolean done = redisTemplate.opsForValue().setIfAbsent("SurveyOperatorInfoUploadInterval:" + userInfo.getUid(), "done", 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(done)) {
            throw new ServiceException(ResultCode.NOT_REPEAT_REQUESTS);
        }

        List<OperatorProgressionDataDTO> operatorDataList = playerInfoDTO.getOperatorDataList();
        String akUid = playerInfoDTO.getUid();

        AkPlayerBindInfoDTO akPlayerBindInfoDTO = new AkPlayerBindInfoDTO();
        akPlayerBindInfoDTO.setAkNickName(playerInfoDTO.getNickName());
        akPlayerBindInfoDTO.setAkUid(akUid);
        akPlayerBindInfoDTO.setChannelName(playerInfoDTO.getChannelName());
        akPlayerBindInfoDTO.setChannelMasterId(playerInfoDTO.getChannelMasterId());
        userService.saveExternalAccountBindingInfoAndAKPlayerBindInfo(userInfo, akPlayerBindInfoDTO);
        userInfo.setAkUid(akUid);

        return saveOperatorData(akUid, operatorDataList);
    }



    /**
     * 保存干员数据
     *
     * @param akUid  明日方舟玩家uid
     * @param operatorProgressionDataDTOList 干员练度调查表
     * @return 成功信息
     */
    private Map<String, Object> saveOperatorData(String akUid, List<OperatorProgressionDataDTO> operatorProgressionDataDTOList) {

        //本次修改影响的数据行数
        int affectedRows = 0;

        //循环上传的干员练度
        for (OperatorProgressionDataDTO operatorProgressionDataDTO : operatorProgressionDataDTOList) {
            //更新数据条数
            operatorProgressionDataDTO.setOwn(true);
            checkOperatorDataValidity(operatorProgressionDataDTO);
            affectedRows++;  //新增数据条数
        }


        LambdaQueryWrapper<OperatorProgressionData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperatorProgressionData::getAkUid,akUid);
        boolean exists = operatorProgressionDataMapper.exists(queryWrapper);

        OperatorProgressionData operatorProgressionData = new OperatorProgressionData();
        operatorProgressionData.setAkUid(akUid);
        operatorProgressionData.setOperatorProgression(JsonMapper.toJSONString(operatorProgressionDataDTOList));
        operatorProgressionData.setCreateTime(new Date());

        if(exists){
            operatorProgressionDataMapper.updateById(operatorProgressionData);
        }else {
            operatorProgressionDataMapper.insert(operatorProgressionData);
        }

        Date date = new Date();
        //更新用户最后一次上传时间
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("affectedRows", affectedRows);
        hashMap.put("updateTime", simpleDateFormat.format(date));
        hashMap.put("registered", false);
        return hashMap;
    }




    /**
     * 对新老干员数据进行检查，是否有非法数据
     *
     * @param operatorProgressionDataDTO 新干员数据
     */
    private void checkOperatorDataValidity(OperatorProgressionDataDTO operatorProgressionDataDTO) {

        //精英化阶段小于2 不能专精和开模组
        if (operatorProgressionDataDTO.getElite() < 2) {
            operatorProgressionDataDTO.setSkill1(0);
            operatorProgressionDataDTO.setSkill2(0);
            operatorProgressionDataDTO.setSkill3(0);
            operatorProgressionDataDTO.setModX(0);
            operatorProgressionDataDTO.setModY(0);
            operatorProgressionDataDTO.setModD(0);
        }

        if (operatorProgressionDataDTO.getRarity() < 6) {
            if (!operatorProgressionDataDTO.getCharId().contains("amiya")) {
                operatorProgressionDataDTO.setSkill3(0);
            }
        }

        if (!operatorProgressionDataDTO.getOwn()) {
            operatorProgressionDataDTO.setMainSkill(0);
            operatorProgressionDataDTO.setPotential(0);
            operatorProgressionDataDTO.setSkill1(0);
            operatorProgressionDataDTO.setSkill2(0);
            operatorProgressionDataDTO.setSkill3(0);
            operatorProgressionDataDTO.setModX(0);
            operatorProgressionDataDTO.setModY(0);
            operatorProgressionDataDTO.setModD(0);
        }

        if (operatorProgressionDataDTO.getMainSkill() == null) {
            operatorProgressionDataDTO.setMainSkill(1);
        }


        if (operatorProgressionDataDTO.getModD() == null) {
            operatorProgressionDataDTO.setModD(0);
        }


    }


    @Override
    public Result<Object> operatorDataReset(String token) {


        return Result.success("重置了" + "条数据");
    }


    @Override
    public List<OperatorProgressionDataDTO> listOperatorProgressionData(String token) {
        //查询用户信息
        UserInfoVO userInfo = userService.getUserInfoVOByToken(token);
        Logger.info("用户uid：" + userInfo.getUid() + "；方舟uid：" + userInfo.getAkUid());
        //保存的干员数据
        List<OperatorProgressionDataDTO> operatorProgressionDataDTOList = new ArrayList<>();
        //查询当前用户的默认方舟uid的干员数据
        LambdaQueryWrapper<OperatorProgressionData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperatorProgressionData::getAkUid, userInfo.getAkUid());

        OperatorProgressionData operatorProgressionData = operatorProgressionDataMapper.selectOne(queryWrapper);

        if (operatorProgressionData == null) {
            return operatorProgressionDataDTOList;
        }


        String operatorProgression = operatorProgressionData.getOperatorProgression();
         operatorProgressionDataDTOList = JsonMapper.parseJSONArray(operatorProgression, new TypeReference<>() {
        });

        return operatorProgressionDataDTOList;
    }

    @Override
    public void backupOperatorProgressionData(){
        String dayText = TimeUtil.getDayText();
        List<OperatorProgressionData> operatorProgressionDataList;
        for (int i = 0; i < 100; i++) {
            operatorProgressionDataList = operatorProgressionDataMapper.getOperatorProgressionData(i * 2000,2000);
            if (operatorProgressionDataList.isEmpty()) {
                break;
            }
            tencentCloudService.backupCOS(JsonMapper.toJSONString(operatorProgressionDataList),"/mysql/operatorProgressionData/"+dayText+"/"+i+".json");
        }
    }



}
