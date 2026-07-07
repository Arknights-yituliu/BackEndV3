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

import com.lhs.entity.po.user.UserExternalAccountBinding;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.survey.OperatorProgressionDataMapper;
import com.lhs.mapper.user.UserExternalAccountBindingMapper;
import com.lhs.service.survey.OperatorDataService;
import com.lhs.service.survey.WarehouseInfoService;
import com.lhs.service.user.BindService;
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
    private final BindService bindService;

    private final IdGenerator idGenerator;

    private final OperatorProgressionDataMapper operatorProgressionDataMapper;

    private final WarehouseInfoService warehouseInfoService;


    private final TencentCloudService tencentCloudService;
    private final UserExternalAccountBindingMapper userExternalAccountBindingMapper;

    public OperatorDataServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                   UserService userService, BindService bindService,
                                   OperatorProgressionDataMapper operatorProgressionDataMapper,
                                   WarehouseInfoService warehouseInfoService,
                                   TencentCloudService tencentCloudService,
                                   UserExternalAccountBindingMapper userExternalAccountBindingMapper) {
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        this.bindService = bindService;
        this.operatorProgressionDataMapper = operatorProgressionDataMapper;
        this.warehouseInfoService = warehouseInfoService;
        this.tencentCloudService = tencentCloudService;
        this.userExternalAccountBindingMapper = userExternalAccountBindingMapper;
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
        bindService.saveExternalAccountBindingInfoAndAKPlayerBindInfo(userInfo, akPlayerBindInfoDTO);
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

    @Override
    public Map<String, Object> saveOpenApiOperatorData(Long uid, PlayerInfoDTO playerInfoDTO) {
        String akUid = playerInfoDTO.getUid();
        List<OperatorProgressionDataDTO> operatorDataList = playerInfoDTO.getOperatorDataList();

        // 保存用户与方舟uid的绑定关系
        AkPlayerBindInfoDTO akPlayerBindInfoDTO = new AkPlayerBindInfoDTO();
        akPlayerBindInfoDTO.setAkNickName(playerInfoDTO.getNickName());
        akPlayerBindInfoDTO.setAkUid(akUid);
        akPlayerBindInfoDTO.setChannelName(playerInfoDTO.getChannelName());
        akPlayerBindInfoDTO.setChannelMasterId(playerInfoDTO.getChannelMasterId());

        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setUid(uid);
        userInfoVO.setAkUid(akUid);
        bindService.saveExternalAccountBindingInfoAndAKPlayerBindInfo(userInfoVO, akPlayerBindInfoDTO);

        return saveOperatorData(akUid, operatorDataList);
    }

    @Override
    public List<OperatorProgressionDataDTO> getOperatorDataByUid(Long uid) {
        // 查询用户绑定的方舟uid
        LambdaQueryWrapper<UserExternalAccountBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserExternalAccountBinding::getUid, uid)
                .orderByDesc(UserExternalAccountBinding::getUpdateTime);
        List<UserExternalAccountBinding> bindings = userExternalAccountBindingMapper.selectList(queryWrapper);

        if (bindings.isEmpty()) {
            return new ArrayList<>();
        }

        String akUid = bindings.get(0).getAkUid();

        // 查询干员数据
        LambdaQueryWrapper<OperatorProgressionData> dataQueryWrapper = new LambdaQueryWrapper<>();
        dataQueryWrapper.eq(OperatorProgressionData::getAkUid, akUid);
        OperatorProgressionData operatorProgressionData = operatorProgressionDataMapper.selectOne(dataQueryWrapper);

        if (operatorProgressionData == null) {
            return new ArrayList<>();
        }

        return JsonMapper.parseJSONArray(operatorProgressionData.getOperatorProgression(), new TypeReference<>() {});
    }

    @Override
    public Map<String, Object> openApiUploadOperatorData(HttpServletRequest httpServletRequest, PlayerInfoDTO playerInfoDTO) {
        String token = httpServletRequest.getHeader("Authorization");
        Long uid = userService.validateOpenApiToken(token, "write");
        return saveOpenApiOperatorData(uid, playerInfoDTO);
    }

    @Override
    public List<OperatorProgressionDataDTO> openApiGetOperatorData(HttpServletRequest httpServletRequest) {
        String token = httpServletRequest.getHeader("Authorization");
        Long uid = userService.validateOpenApiToken(token, "read");
        return getOperatorDataByUid(uid);
    }

}
