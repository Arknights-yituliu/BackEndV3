package com.lhs.service.survey;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.survey.OperatorDataDTO;
import com.lhs.entity.po.survey.*;

import jakarta.servlet.http.HttpServletResponse;

import java.util.*;


public interface OperatorDataService  {
    /**
     * 手动上传干员练度调查表
     * @param token token
     * @param surveyOperatorDataList 干员练度调查表单
     * @return 成功消息
     */
    Map<String, Object> manualUploadOperator(String token, List<OperatorData> surveyOperatorDataList);

    /**
     * 导入森空岛干员练度数据
     * @param token 一图流凭证
     * @param dataStr 上传的json字符串
     * @return 返回成功信息
     */
    Map<String, Object> importSKLandPlayerInfoV2(String token, String dataStr);


    /**
     * 重置个人上传的干员数据
     * @param token 一图流凭证
     * @return 成功消息
     */
    Result<Object> operatorDataReset(String token);

    /**
     * 找回用户填写的数据
     * @param token token
     * @return 成功消息
     */
    List<OperatorDataVo> getOperatorInfoByToken(String token);


    Map<String, Object> saveOperatorDataByRhodes(OperatorDataDTO operatorDataDTO);

}
