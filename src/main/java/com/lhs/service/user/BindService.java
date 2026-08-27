package com.lhs.service.user;

import com.lhs.entity.dto.user.AkPlayerBindInfoDTO;
import com.lhs.entity.vo.survey.UserInfoVO;

public interface BindService {

    /**
     * 备份用户外部账号绑定数据到腾讯云COS
     */
    void backupUserExternalAccountBinding();

    /**
     * 保存一图流用户与第三方游戏账号（明日方舟）的绑定关系
     *
     * @param uid          一图流用户信息
     * @param akPlayerBindInfoDTO 第三方账号的信息
     */
    void saveExternalAccountBindingInfoAndAKPlayerBindInfo(Long uid, AkPlayerBindInfoDTO akPlayerBindInfoDTO);
}
