package com.lhs.service.survey;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.OperatorProgressionDataDTO;
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
    List<OperatorProgressionDataDTO> listOperatorProgressionData(String token);


    Object importSKLandPlayerInfoV3(HttpServletRequest httpServletRequest,PlayerInfoDTO playerInfoDTO);


    void backupOperatorProgressionData();
}
