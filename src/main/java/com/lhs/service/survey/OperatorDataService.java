package com.lhs.service.survey;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.Result;
import com.lhs.common.util.ResultCode;
import com.lhs.common.util.*;
import com.lhs.entity.po.survey.*;
import com.lhs.entity.vo.survey.OperatorExportExcelVO;
import com.lhs.mapper.survey.AkPlayerBindInfoMapper;
import com.lhs.mapper.survey.OperatorDataMapper;
import com.lhs.mapper.survey.OperatorDataVoMapper;
import com.lhs.service.util.AkGameDataService;
import com.lhs.service.util.OSSService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;



public interface OperatorDataService  {
    /**
     * 手动上传干员练度调查表
     * @param token token
     * @param operatorDataList 干员练度调查表单
     * @return 成功消息
     */
    Map<String, Object> manualUploadOperator(String token, List<OperatorData> operatorDataList);

    /**
     * 导入森空岛干员练度数据
     * @param token 一图流凭证
     * @param dataStr 上传的json字符串
     * @return
     */
    Map<String, Object> importSKLandPlayerInfoV2(String token, String dataStr);

    /**
     * 导入干员练度调查表
     *
     * @param file  Excel文件
     * @param token token
     * @return 成功消息
     */
    Map<String, Object> importExcel(MultipartFile file, String token);

    /**
     * 重置个人上传的干员数据
     * @param token 一图流凭证
     * @return
     */
    Result<Object> operatorDataReset(String token);

    /**
     * 找回用户填写的数据
     * @param token token
     * @return 成功消息
     */
    List<OperatorDataVo> getOperatorForm(String token);

    /**
     * 导出干员的数据
     * @param response 返回体
     * @param token 一图流凭证
     */
    void exportSurveyOperatorForm(HttpServletResponse response, String token);


    List<Map<String,Object>> operatorDataDuplicateDistinct();



}
