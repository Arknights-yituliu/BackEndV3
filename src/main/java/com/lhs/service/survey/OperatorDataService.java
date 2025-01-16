package com.lhs.service.survey;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.PlayerInfoDTO;
import com.lhs.entity.po.survey.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;


public interface OperatorDataService  {
    /**
     * 手动上传干员练度调查表
     * @param httpServletRequest 请求信息
     * @param surveyOperatorDataList 干员练度调查表单
     * @return 成功消息
     */
    Map<String, Object> manualUploadOperator(HttpServletRequest httpServletRequest, List<OperatorData> surveyOperatorDataList);


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
    List<OperatorDataVo> getUserOperatorInfo(String token);


    Object importSKLandPlayerInfoV3(HttpServletRequest httpServletRequest,PlayerInfoDTO playerInfoDTO);


}
