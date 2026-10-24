package com.lhs.service.user;

public interface BindService {

    /**
     * 备份用户外部账号绑定数据到腾讯云COS
     * <p>
     * 绑定数据的写入链路（/auth/survey/operator/import/skland/v3）已随接口下线一并移除，
     * 绑定关系改由用户中心维护，本地只保留定时备份能力
     */
    void backupUserExternalAccountBinding();

}
