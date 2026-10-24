package com.lhs.service.survey;

import com.lhs.entity.dto.survey.OperatorProgressionDataDTO;
import com.lhs.entity.dto.survey.OperatorProgressionDataV2DTO;
import com.lhs.entity.dto.survey.ManualOperatorDataDTO;
import com.lhs.entity.dto.survey.PlayerInfoDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;


public interface OperatorDataService  {


    /**
     * 找回用户填写的数据
     * @return 成功消息
     */
    List<OperatorProgressionDataDTO> listOperatorProgressionData();


    Object importSKLandPlayerInfoV3(HttpServletRequest httpServletRequest,PlayerInfoDTO playerInfoDTO);

    Map<String, Object> importManualOperatorData(ManualOperatorDataDTO manualOperatorDataDTO);

    void backupOperatorProgressionData();

    /**
     * 第三方API获取干员练度数据（含token校验）
     *
     * @param httpServletRequest HTTP请求（从Header提取token）
     * @return 干员练度数据列表（含技能和模组详细信息）
     */
    List<OperatorProgressionDataV2DTO> openApiGetOperatorData(HttpServletRequest httpServletRequest);
}
