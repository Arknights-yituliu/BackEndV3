package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.common.enums.ResultCode;
import com.lhs.entity.dto.survey.OperatorCarryQuestionnaireDTO;
import com.lhs.entity.po.survey.QuestionnaireResult;
import com.lhs.mapper.survey.QuestionnaireResultMapper;
import com.lhs.service.user.UserService;
import com.lhs.service.util.TencentCloudService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import java.util.*;


@Service
public class QuestionnaireService {

    private final RedisTemplate<String, String> redisTemplate;
    private final QuestionnaireResultMapper questionnaireResultMapper;

    private final IdGenerator idGenerator;
    private final RateLimiter rateLimiter;
    private final UserService userService;
    private final TencentCloudService tencentCloudService;


    public QuestionnaireService(RedisTemplate<String, String> redisTemplate,
                                QuestionnaireResultMapper questionnaireResultMapper,
                                RateLimiter rateLimiter,
                                UserService userService, TencentCloudService tencentCloudService) {
        this.redisTemplate = redisTemplate;
        this.questionnaireResultMapper = questionnaireResultMapper;
        this.rateLimiter = rateLimiter;
        this.userService = userService;
        this.tencentCloudService = tencentCloudService;

        this.idGenerator = new IdGenerator(1L);
    }


    public void uploadQuestionnaireResult(HttpServletRequest httpServletRequest, OperatorCarryQuestionnaireDTO operatorCarryQuestionnaireDTO) {

        //获取提交者IP
        String ipAddress = IpUtil.getIpAddress(httpServletRequest);
        Long uid = userService.getUidByHttpServletRequest(httpServletRequest);
        //提交间隔不能短于5s，短于5s抛出异常
        rateLimiter.tryAcquire("SurveySubmitterIP:" + ipAddress, 1, 5, ResultCode.NOT_REPEAT_REQUESTS);


        //检查上传的干员是否有重复
        String result = getResultStr(operatorCarryQuestionnaireDTO);
        //根据问卷类型和提交者ip对应的id查询以前的问卷结果
        LambdaQueryWrapper<QuestionnaireResult> questionnaireResultQueryWrapper = new LambdaQueryWrapper<>();
        questionnaireResultQueryWrapper
                .eq(QuestionnaireResult::getQuestionnaireCode, operatorCarryQuestionnaireDTO.getQuestionnaireCode())
                .eq(QuestionnaireResult::getUid, uid)
                .orderByDesc(QuestionnaireResult::getCreateTime)
                .last("limit 1");
        QuestionnaireResult lastQuestionnaireResult = questionnaireResultMapper.selectOne(questionnaireResultQueryWrapper);

        Date date = new Date();


        QuestionnaireResult questionnaireResult = new QuestionnaireResult();
        questionnaireResult.setId(idGenerator.nextId());
        questionnaireResult.setUid(uid);
        questionnaireResult.setQuestionnaireCode(operatorCarryQuestionnaireDTO.getQuestionnaireCode());

        questionnaireResult.setContent(result);
        questionnaireResult.setCreateTime(date);
        questionnaireResult.setUpdateTime(date);
        questionnaireResult.setIp(ipAddress);

        //如果没有则直接新增
        if (lastQuestionnaireResult == null) {
            //创建问卷结果信息
            questionnaireResultMapper.insert(questionnaireResult);
            return;
        }

        long currentTimeStamp = System.currentTimeMillis();
        long timeInterval = currentTimeStamp - lastQuestionnaireResult.getCreateTime().getTime();

        if (timeInterval < 60 * 60 * 24 * 7 * 1000L) {
            lastQuestionnaireResult.setContent(result);
            lastQuestionnaireResult.setUpdateTime(date);
            questionnaireResult.setIp(ipAddress);
            questionnaireResultMapper.updateById(lastQuestionnaireResult);
            return;
        }

        questionnaireResultMapper.insert(questionnaireResult);
    }


    public void backup() {
        String dayText = TimeUtil.getDayText();
        List<QuestionnaireResult> questionnaireResults;
        for (int i = 0; i < 10; i++) {
            LambdaQueryWrapper<QuestionnaireResult> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.last("limit " + i * 1000 + ",1000");
            questionnaireResults = questionnaireResultMapper.selectList(queryWrapper);
            if (questionnaireResults.isEmpty()) {
                break;
            }
            tencentCloudService.backupCOS(JsonMapper.toJSONString(questionnaireResults),"/mysql/questionnaireResultData/"+dayText+"/"+i+".json");
        }
    }


    private static String getResultStr(OperatorCarryQuestionnaireDTO operatorCarryQuestionnaireDTO) {
        List<String> operatorList = operatorCarryQuestionnaireDTO.getOperatorList();
        Set<String> operatorSet = new HashSet<>(operatorList);


        if (operatorSet.size() > 12 || operatorSet.size() < 6) {
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
