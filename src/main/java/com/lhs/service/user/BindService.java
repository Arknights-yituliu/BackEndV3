package com.lhs.service.user;

import com.lhs.entity.dto.user.AkPlayerBindInfoDTO;
import com.lhs.entity.vo.survey.UserInfoVO;

public interface BindService {

    /**
     * 备份用户外部账号绑定数据到腾讯云COS
     */
    void backupUserExternalAccountBinding();

    void saveSklandBindingAndPlayerInfo(Long uid, AkPlayerBindInfoDTO akPlayerBindInfoDTO);

}
