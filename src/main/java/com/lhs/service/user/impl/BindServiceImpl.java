package com.lhs.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lhs.common.util.*;
import com.lhs.entity.dto.user.AkPlayerBindInfoDTO;
import com.lhs.entity.po.user.AkPlayerBindInfo;
import com.lhs.entity.po.user.UserExternalAccountBinding;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.user.AkPlayerBindInfoMapper;
import com.lhs.mapper.user.UserExternalAccountBindingMapper;
import com.lhs.service.user.BindService;
import com.lhs.service.util.TencentCloudService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BindServiceImpl implements BindService {

    private final UserExternalAccountBindingMapper userExternalAccountBindingMapper;
    private final AkPlayerBindInfoMapper akPlayerBindInfoMapper;
    private final TencentCloudService tencentCloudService;
    private final IdGenerator idGenerator;

    public BindServiceImpl(UserExternalAccountBindingMapper userExternalAccountBindingMapper,
            AkPlayerBindInfoMapper akPlayerBindInfoMapper,
            TencentCloudService tencentCloudService) {
        this.userExternalAccountBindingMapper = userExternalAccountBindingMapper;
        this.akPlayerBindInfoMapper = akPlayerBindInfoMapper;
        this.tencentCloudService = tencentCloudService;
        this.idGenerator = new IdGenerator(1L);
    }

    @Override
    public void backupUserExternalAccountBinding() {
        String dayText = TimeUtil.getDayText();
        List<UserExternalAccountBinding> list1 = userExternalAccountBindingMapper.selectList(null);
        tencentCloudService.backupCOS(JsonMapper.toJSONString(list1),
                "/mysql/user/" + dayText + "/user_external_account_binding.json");

        List<AkPlayerBindInfo> list2 = akPlayerBindInfoMapper.selectList(null);
        tencentCloudService.backupCOS(JsonMapper.toJSONString(list2),
                "/mysql/user/" + dayText + "/ak_player_bind_info.json");
    }

    @Override
    public void saveExternalAccountBindingInfoAndAKPlayerBindInfo(Long uid,
            AkPlayerBindInfoDTO akPlayerBindInfoDTO) {
        UserExternalAccountBinding userExternalAccountBinding = new UserExternalAccountBinding();
        userExternalAccountBinding.setId(idGenerator.nextId());

        userExternalAccountBinding.setUid(uid);
        userExternalAccountBinding.setAkUid(akPlayerBindInfoDTO.getAkUid());

        saveUserExternalAccountBinding(userExternalAccountBinding);

        AkPlayerBindInfo akPlayerBindInfo = new AkPlayerBindInfo();
        akPlayerBindInfo.copyByAkPlayerBindInfoDTO(akPlayerBindInfoDTO);
        saveAkPlayerBindInfo(akPlayerBindInfo);
    }

    /**
     * 保存或更新用户外部账号绑定信息
     */
    private void saveUserExternalAccountBinding(UserExternalAccountBinding userExternalAccountBinding) {
        LambdaQueryWrapper<UserExternalAccountBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserExternalAccountBinding::getAkUid, userExternalAccountBinding.getAkUid())
                .eq(UserExternalAccountBinding::getUid, userExternalAccountBinding.getUid());
        UserExternalAccountBinding existsData = userExternalAccountBindingMapper.selectOne(queryWrapper);
        long timeStamp = System.currentTimeMillis();

        userExternalAccountBinding.setUpdateTime(timeStamp);

        Logger.info("要添加的外部账号绑定信息 {} " + userExternalAccountBinding);
        if (existsData == null) {
            userExternalAccountBinding.setId(idGenerator.nextId());
            userExternalAccountBinding.setCreateTime(timeStamp);
            userExternalAccountBinding.setDeleteFlag(false);
            userExternalAccountBindingMapper.insert(userExternalAccountBinding);
        } else {
            userExternalAccountBinding.setId(existsData.getId());
            userExternalAccountBinding.setCreateTime(existsData.getCreateTime());
            userExternalAccountBindingMapper.updateById(userExternalAccountBinding);
        }
    }

    /**
     * 保存或更新明日方舟玩家绑定信息
     */
    private void saveAkPlayerBindInfo(AkPlayerBindInfo akPlayerBindInfo) {
        LambdaQueryWrapper<AkPlayerBindInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AkPlayerBindInfo::getAkUid, akPlayerBindInfo.getAkUid());
        AkPlayerBindInfo oldInfo = akPlayerBindInfoMapper.selectOne(queryWrapper);
        akPlayerBindInfo.setUpdateTime(System.currentTimeMillis());
        Logger.info("要添加的明日方舟账号绑定信息，id为" + akPlayerBindInfo);
        if (oldInfo == null) {
            akPlayerBindInfo.setId(idGenerator.nextId());
            akPlayerBindInfo.setDeleteFlag(false);
            akPlayerBindInfoMapper.insert(akPlayerBindInfo);
        } else {
            akPlayerBindInfo.setId(oldInfo.getId());
            akPlayerBindInfoMapper.updateById(akPlayerBindInfo);
        }
    }
}
