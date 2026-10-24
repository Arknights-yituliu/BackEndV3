package com.lhs.service.survey;

import com.lhs.entity.dto.survey.OperatorProgressionDataV2DTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 干员练度服务
 * <p>
 * 本地的练度导入与回读方法（listOperatorProgressionData / importSKLandPlayerInfoV3 /
 * importManualOperatorData）已随对应接口下线一并移除，仅保留后台备份与第三方读取能力
 */
public interface OperatorDataService {

    /**
     * 备份本地干员练度数据到对象存储（定时任务调用）
     */
    void backupOperatorProgressionData();

    /**
     * 第三方API获取干员练度数据（含token校验）
     *
     * @param httpServletRequest HTTP请求（从Header提取token）
     * @return 干员练度数据列表（含技能和模组详细信息）
     */
    List<OperatorProgressionDataV2DTO> openApiGetOperatorData(HttpServletRequest httpServletRequest);
}
