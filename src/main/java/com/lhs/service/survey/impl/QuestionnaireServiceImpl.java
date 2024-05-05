package com.lhs.service.survey.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.po.survey.QuestionnaireResult;
import com.lhs.entity.po.survey.SurveySubmitter;
import com.lhs.mapper.survey.QuestionnaireResultMapper;
import com.lhs.mapper.survey.SurveySubmitterMapper;
import com.lhs.service.survey.QuestionnaireService;
import com.sun.mail.imap.protocol.ID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@Service
public class QuestionnaireServiceImpl implements QuestionnaireService {

    private final RedisTemplate<String,String> redisTemplate;
    private final QuestionnaireResultMapper questionnaireResultMapper;
    private final SurveySubmitterMapper surveySubmitterMapper;
    private final IdGenerator idGenerator;

    public QuestionnaireServiceImpl(RedisTemplate<String, String> redisTemplate,
                                    QuestionnaireResultMapper questionnaireResultMapper,
                                    SurveySubmitterMapper surveySubmitterMapper) {
        this.redisTemplate = redisTemplate;
        this.questionnaireResultMapper = questionnaireResultMapper;
        this.surveySubmitterMapper = surveySubmitterMapper;
        this.idGenerator = new IdGenerator(1L);
    }

    @Override
    public void uploadQuestionnaireResult(HttpServletRequest httpServletRequest,String requestContent) {
        //获取提交者IP
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);
        //提交间隔不能短于5s，短于5s抛出异常
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("SurveySubmitterIP", ipAddress, 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(lock)) {
            throw new ServiceException(ResultCode.OPERATION_INTERVAL_TOO_SHORT);
        }

        //根据IP查询提交者此前是否提交过记录
        QueryWrapper<SurveySubmitter> surveySubmitterQueryWrapper = new QueryWrapper<>();
        surveySubmitterQueryWrapper.eq("ip",ipAddress);
        //检查上传的干员是否有重复
        JsonNode operatorNodeList = JsonMapper.parseJSONObject(requestContent);
        Set<String> operatorSet = new HashSet<>();

        for(JsonNode node:operatorNodeList){
            operatorSet.add(node.asText());
        }

        StringBuilder sb = new StringBuilder();
        for (String element : operatorSet) {
            if (!sb.isEmpty()) {
                sb.append(","); // 在非首个元素前添加逗号
            }
            sb.append(element);
        }

        String result = sb.toString();

        if(operatorSet.size()!=12){
            throw new ServiceException(ResultCode.OPERATOR_QUANTITY_NOT_EQUAL_TO_TWELVE);
        }

        long nowTimeStamp = System.currentTimeMillis();

        SurveySubmitter surveySubmitter = new SurveySubmitter();
        surveySubmitter.setId(idGenerator.nextId());
        surveySubmitter.setBanned(false);
        surveySubmitter.setCreateTime(nowTimeStamp);
        surveySubmitter.setUpdateTime(nowTimeStamp);
        surveySubmitter.setIp(ipAddress);

        QuestionnaireResult questionnaireResult = new QuestionnaireResult();
        questionnaireResult.setId(idGenerator.nextId());
        questionnaireResult.setIpId(surveySubmitter.getId());
        questionnaireResult.setQuestionnaireType(1);
        questionnaireResult.setQuestionnaireContent(result);
        questionnaireResult.setCreateTime(nowTimeStamp);
        questionnaireResult.setUpdateTime(nowTimeStamp);

        SurveySubmitter surveySubmitterByIp = surveySubmitterMapper.selectOne(surveySubmitterQueryWrapper);
        if(surveySubmitterByIp!=null){
            surveySubmitter.setId(surveySubmitterByIp.getId());
            surveySubmitter.setCreateTime(surveySubmitterByIp.getCreateTime());

            QueryWrapper<QuestionnaireResult> questionnaireResultQueryWrapper = new QueryWrapper<>();
            questionnaireResultQueryWrapper.eq("ip_id",surveySubmitterByIp.getId());
            QuestionnaireResult questionnaireResultByIpId = questionnaireResultMapper.selectOne(questionnaireResultQueryWrapper);

            if(questionnaireResultByIpId!=null){
                questionnaireResult.setId(questionnaireResultByIpId.getId());
                questionnaireResult.setQuestionnaireContent(result);
                questionnaireResult.setCreateTime(questionnaireResultByIpId.getCreateTime());
                questionnaireResultMapper.updateById(questionnaireResultByIpId);
            }else {
                questionnaireResultMapper.insert(questionnaireResult);
            }

        }else {
            surveySubmitterMapper.insert(surveySubmitter);
            questionnaireResultMapper.insert(questionnaireResult);
        }






    }



}
