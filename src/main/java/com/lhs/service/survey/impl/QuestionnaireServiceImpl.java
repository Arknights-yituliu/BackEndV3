package com.lhs.service.survey.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.dto.survey.QuestionnaireSubmitInfoDTO;
import com.lhs.entity.po.survey.QuestionnaireResult;
import com.lhs.entity.po.survey.SurveySubmitter;
import com.lhs.entity.vo.survey.SurveySubmitterVO;
import com.lhs.mapper.survey.QuestionnaireResultMapper;
import com.lhs.mapper.survey.SurveySubmitterMapper;
import com.lhs.service.survey.QuestionnaireService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    public SurveySubmitterVO uploadQuestionnaireResult(HttpServletRequest httpServletRequest, QuestionnaireSubmitInfoDTO questionnaireSubmitInfoDTO) {
        //获取提交者IP
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);
        //提交间隔不能短于5s，短于5s抛出异常
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("SurveySubmitterIP", ipAddress, 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(lock)) {
            throw new ServiceException(ResultCode.OPERATION_INTERVAL_TOO_SHORT);
        }

        SurveySubmitterVO surveySubmitterVO = new SurveySubmitterVO();

        //检查上传的干员是否有重复
        String result = getResultStr(questionnaireSubmitInfoDTO);
        Integer questionnaireType = questionnaireSubmitInfoDTO.getQuestionnaireType();

        long nowTimeStamp = System.currentTimeMillis();

        //创建提交者信息
        SurveySubmitter surveySubmitter = new SurveySubmitter();
        surveySubmitter.setId(idGenerator.nextId());
        surveySubmitter.setBanned(false);
        surveySubmitter.setCreateTime(nowTimeStamp);
        surveySubmitter.setUpdateTime(nowTimeStamp);
        surveySubmitter.setIp(ipAddress);

        //创建问卷结果信息
        QuestionnaireResult questionnaireResult = new QuestionnaireResult();
        questionnaireResult.setId(idGenerator.nextId());
        questionnaireResult.setIpId(surveySubmitter.getId());
        questionnaireResult.setQuestionnaireType(questionnaireType);
        questionnaireResult.setQuestionnaireContent(result);
        questionnaireResult.setCreateTime(nowTimeStamp);


        //根据IP和用户id查询提交者此前是否提交过记录
        LambdaQueryWrapper<SurveySubmitter> surveySubmitterQueryWrapper = new LambdaQueryWrapper<>();
        surveySubmitterQueryWrapper.eq(SurveySubmitter::getIp,ipAddress).or().eq(SurveySubmitter::getId,surveySubmitter.getId());
        SurveySubmitter surveySubmitterByIpOrId = surveySubmitterMapper.selectOne(surveySubmitterQueryWrapper);

        //未查询到数据，则进行新增
        if(surveySubmitterByIpOrId==null){
            surveySubmitterMapper.insert(surveySubmitter);
            questionnaireResultMapper.insert(questionnaireResult);
            return surveySubmitterVO;
        }



        //根据问卷类型和提交者ip对应的id查询以前的问卷结果
        LambdaQueryWrapper<QuestionnaireResult> questionnaireResultQueryWrapper = new LambdaQueryWrapper<>();
        questionnaireResultQueryWrapper
                .eq(QuestionnaireResult::getIpId,surveySubmitterByIpOrId.getId())
                .eq(QuestionnaireResult::getQuestionnaireType,questionnaireType)
                .orderByDesc(QuestionnaireResult::getCreateTime)
                .last("limit 1");
        QuestionnaireResult lastQuestionnaireResult = questionnaireResultMapper.selectOne(questionnaireResultQueryWrapper);

        //如果没有则直接新增
        if(lastQuestionnaireResult==null){
            questionnaireResultMapper.insert(questionnaireResult);
        }else { //如果有提交过的问卷，判断一下最初的提交日期，未超过1天进行更新，超过1天新增
            long timeStamp = System.currentTimeMillis();
            if(timeStamp-lastQuestionnaireResult.getCreateTime()>60*60*24*1000){
                questionnaireResultMapper.insert(questionnaireResult);
            }else {
                lastQuestionnaireResult.setQuestionnaireContent(result);

                questionnaireResultMapper.updateById(lastQuestionnaireResult);
            }
        }

        //更新提交者的信息
        surveySubmitterByIpOrId.setUpdateTime(nowTimeStamp);
        surveySubmitterByIpOrId.setIp(ipAddress);
        //将更新后的提交者信息更新到数据库
        surveySubmitterMapper.updateById(surveySubmitterByIpOrId);

        return surveySubmitterVO;
    }

    @Override
    public void statisticsQuestionnaireResult(int questionnaireType) {
        LambdaQueryWrapper<QuestionnaireResult> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(QuestionnaireResult::getQuestionnaireType,questionnaireType);
        List<QuestionnaireResult> resultList = questionnaireResultMapper.selectList(lambdaQueryWrapper);


        HashMap<String, Integer> statisticsResult = new HashMap<>();
        for(QuestionnaireResult questionnaireResult:resultList){
            String questionnaireContent = questionnaireResult.getQuestionnaireContent();
            String[] split = questionnaireContent.split(",");
            for(String charId:split){
                statisticsResult.put(charId,statisticsResult.getOrDefault(charId,0)+1);
            }
        }

        statisticsResult.forEach((k,v)->{
            System.out.println(k+"————"+v);
        });
    }


    private static String getResultStr(QuestionnaireSubmitInfoDTO questionnaireSubmitInfoDTO) {
        List<String> operatorList = questionnaireSubmitInfoDTO.getOperatorList();
        Set<String> operatorSet = new HashSet<>(operatorList);


        if(operatorSet.size()>12||operatorSet.size()<6){
            throw new ServiceException(ResultCode.OPERATOR_QUANTITY_INVALID);
        }

        //将其拼接为以 , 拼接的字符串
        StringBuilder sb = new StringBuilder();
        for (String element : operatorSet) {
            if (!sb.isEmpty()) {
                sb.append(","); // 在非首个元素前添加逗号
            }
            sb.append(element);
        }
        return sb.toString();
    }





}
