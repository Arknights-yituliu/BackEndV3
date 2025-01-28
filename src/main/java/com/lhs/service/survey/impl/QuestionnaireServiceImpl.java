package com.lhs.service.survey.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.enums.QuestionnaireType;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.IpUtil;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.util.LogUtils;
import com.lhs.common.util.RateLimiter;
import com.lhs.entity.dto.survey.QuestionnaireSubmitInfoDTO;
import com.lhs.entity.po.survey.OperatorCarryRateStatistics;
import com.lhs.entity.po.survey.QuestionnaireResult;
import com.lhs.entity.vo.survey.OperatorCarryRateStatisticsVO;
import com.lhs.entity.vo.survey.SurveySubmitterVO;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.mapper.survey.OperatorCarryRateStatisticsMapper;
import com.lhs.mapper.survey.QuestionnaireResultMapper;
import com.lhs.service.survey.QuestionnaireService;
import com.lhs.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class QuestionnaireServiceImpl implements QuestionnaireService {

    private final RedisTemplate<String, String> redisTemplate;
    private final QuestionnaireResultMapper questionnaireResultMapper;

    private final IdGenerator idGenerator;
    private final RateLimiter rateLimiter;
    private final UserService userService;
    private final OperatorCarryRateStatisticsMapper operatorCarryRateStatisticsMapper;

    public QuestionnaireServiceImpl(RedisTemplate<String, String> redisTemplate,
                                    QuestionnaireResultMapper questionnaireResultMapper,
                                    RateLimiter rateLimiter,
                                    UserService userService, OperatorCarryRateStatisticsMapper operatorCarryRateStatisticsMapper) {
        this.redisTemplate = redisTemplate;
        this.questionnaireResultMapper = questionnaireResultMapper;
        this.rateLimiter = rateLimiter;
        this.userService = userService;
        this.operatorCarryRateStatisticsMapper = operatorCarryRateStatisticsMapper;
        this.idGenerator = new IdGenerator(1L);
    }

    @Override
    public void uploadQuestionnaireResult(HttpServletRequest httpServletRequest, QuestionnaireSubmitInfoDTO questionnaireSubmitInfoDTO) {
        String uidText = httpServletRequest.getHeader("uid");
        Long uid = idGenerator.nextId();
        if (uidText != null && uidText.length() > 6) {
            uid = Long.parseLong(uidText);
        }

        Boolean loginStatus = userService.checkUserLoginStatus(httpServletRequest);
        if (loginStatus) {
            UserInfoVO userInfoVO = userService.getUserInfoVOByHttpServletRequest(httpServletRequest);
            uid = userInfoVO.getUid();
        }

        //获取提交者IP
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);

        //提交间隔不能短于5s，短于5s抛出异常
        rateLimiter.tryAcquire("SurveySubmitterIP:" + ipAddress, 1, 5, ResultCode.NOT_REPEAT_REQUESTS);

        SurveySubmitterVO surveySubmitterVO = new SurveySubmitterVO();

        //检查上传的干员是否有重复
        String result = getResultStr(questionnaireSubmitInfoDTO);
        //根据问卷类型和提交者ip对应的id查询以前的问卷结果
        LambdaQueryWrapper<QuestionnaireResult> questionnaireResultQueryWrapper = new LambdaQueryWrapper<>();
        questionnaireResultQueryWrapper
                .eq(QuestionnaireResult::getType, QuestionnaireType.SELECTED_OPERATOR_FOR_NEW_GAME.getCode())
                .eq(QuestionnaireResult::getUid, uid)
                .orderByDesc(QuestionnaireResult::getCreateTime);
        QuestionnaireResult lastQuestionnaireResult = questionnaireResultMapper.selectOne(questionnaireResultQueryWrapper);

        Date date = new Date();

        //如果没有则直接新增
        if (lastQuestionnaireResult == null) {
            //创建问卷结果信息
            QuestionnaireResult questionnaireResult = new QuestionnaireResult();
            questionnaireResult.setId(idGenerator.nextId());
            questionnaireResult.setUid(uid);
            questionnaireResult.setType(QuestionnaireType.SELECTED_OPERATOR_FOR_NEW_GAME.getCode());
            questionnaireResult.setContent(result);
            questionnaireResult.setCreateTime(date);
            questionnaireResult.setUpdateTime(date);
            questionnaireResultMapper.insert(questionnaireResult);
        } else {
            lastQuestionnaireResult.setContent(result);
            lastQuestionnaireResult.setUpdateTime(date);
            questionnaireResultMapper.updateById(lastQuestionnaireResult);
        }

    }

    @Override
    public void statisticsQuestionnaireResult(int questionnaireType) {
        LambdaQueryWrapper<QuestionnaireResult> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(QuestionnaireResult::getType, questionnaireType);
        List<QuestionnaireResult> resultList = questionnaireResultMapper.selectList(lambdaQueryWrapper);

        int count = 0;
        List<OperatorCarryRateStatistics> operatorCarryRateStatisticsList = new ArrayList<>();

        count += resultList.size();
        if(count<10){
            return;
        }

        LogUtils.info("本次统计的干员携带率问卷数量："+count);
        HashMap<String, Integer> statisticsResult = new HashMap<>();
        for (QuestionnaireResult questionnaireResult : resultList) {
            String questionnaireContent = questionnaireResult.getContent();
            String[] split = questionnaireContent.split(",");
            for (String charId : split) {
                statisticsResult.put(charId, statisticsResult.getOrDefault(charId, 0) + 1);
            }
        }

        operatorCarryRateStatisticsMapper.expireOldData();


        Date date = new Date();
        int finalCount = count;

        statisticsResult.forEach((charId, v) -> {
            OperatorCarryRateStatistics operatorCarryRateStatistics = new OperatorCarryRateStatistics();
            operatorCarryRateStatistics.setId(idGenerator.nextId());
            operatorCarryRateStatistics.setCharId(charId);
            operatorCarryRateStatistics.setCarryingRate((double)v/finalCount);
            operatorCarryRateStatistics.setCreateTime(date);
            operatorCarryRateStatistics.setExpiredFlag(false);
            operatorCarryRateStatisticsList.add(operatorCarryRateStatistics);
        });

        operatorCarryRateStatisticsMapper.insertBatch(operatorCarryRateStatisticsList);
    }

    @Override
    public List<OperatorCarryRateStatisticsVO> getQuestionnaireResultByType(Integer questionnaireType) {

        List<OperatorCarryRateStatisticsVO> list = operatorCarryRateStatisticsMapper.getOperatorCarryRateResult();
        if(list.isEmpty()){
            return new ArrayList<>();
        }
        return list;
    }


    private static String getResultStr(QuestionnaireSubmitInfoDTO questionnaireSubmitInfoDTO) {
        List<String> operatorList = questionnaireSubmitInfoDTO.getOperatorList();
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
