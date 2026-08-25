package com.lhs.service.survey;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.OperatorProgressionDataDTO;
import com.lhs.entity.dto.survey.OperatorProgressionDataV2DTO;
import com.lhs.entity.dto.survey.PlayerInfoDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;


public interface OperatorDataService  {


    /**
     * 重置个人上传的干员数据
     * @param token 一图流凭证
     * @return 成功消息
     */
    Result<Object> operatorDataReset(String token);

    /**
     * 找回用户填写的数据
     * @return 成功消息
     */
    List<OperatorProgressionDataDTO> listOperatorProgressionData();


    Object importSKLandPlayerInfoV3(HttpServletRequest httpServletRequest,PlayerInfoDTO playerInfoDTO);


    void backupOperatorProgressionData();

    /**
     * 第三方API上传干员练度数据（含token校验）
     *
     * @param httpServletRequest HTTP请求（从Header提取token）
     * @param playerInfoDTO      玩家信息
     * @return 处理结果
     */
    Map<String, Object> openApiUploadOperatorData(HttpServletRequest httpServletRequest, PlayerInfoDTO playerInfoDTO);

    /**
     * 第三方API获取干员练度数据（含token校验）
     *
     * @param httpServletRequest HTTP请求（从Header提取token）
     * @return 干员练度数据列表（含技能和模组详细信息）
     */
    List<OperatorProgressionDataV2DTO> openApiGetOperatorData(HttpServletRequest httpServletRequest);
}
