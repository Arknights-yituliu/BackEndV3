package com.lhs.service.user.impl;

import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.TimeUtil;
import com.lhs.entity.po.user.AkPlayerBindInfo;
import com.lhs.entity.po.user.UserExternalAccountBinding;
import com.lhs.mapper.user.AkPlayerBindInfoMapper;
import com.lhs.mapper.user.UserExternalAccountBindingMapper;
import com.lhs.service.user.BindService;
import com.lhs.service.util.TencentCloudService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户绑定服务实现
 * <p>
 * 绑定写入方法（saveSklandBindingAndPlayerInfo 及其两个私有落库方法）已随
 * /auth/survey/operator/import/skland/v3 下线一并删除，本类只保留定时备份能力
 */
@Service
public class BindServiceImpl implements BindService {

    private final UserExternalAccountBindingMapper userExternalAccountBindingMapper;
    private final AkPlayerBindInfoMapper akPlayerBindInfoMapper;
    private final TencentCloudService tencentCloudService;

    public BindServiceImpl(UserExternalAccountBindingMapper userExternalAccountBindingMapper,
            AkPlayerBindInfoMapper akPlayerBindInfoMapper,
            TencentCloudService tencentCloudService) {
        this.userExternalAccountBindingMapper = userExternalAccountBindingMapper;
        this.akPlayerBindInfoMapper = akPlayerBindInfoMapper;
        this.tencentCloudService = tencentCloudService;
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
}
