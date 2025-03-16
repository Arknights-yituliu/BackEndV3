package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.enums.QuestionnaireType;
import com.lhs.common.enums.RecordType;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.common.enums.ResultCode;
import com.lhs.entity.dto.survey.OperatorCarryQuestionnaireDTO;
import com.lhs.entity.po.survey.QuestionnaireResult;
import com.lhs.entity.po.survey.QuestionnaireStatisticsResult;
import com.lhs.entity.vo.survey.OperatorCarryVO;
import com.lhs.entity.vo.survey.OperatorCarryStatisticsResultVO;
import com.lhs.mapper.survey.QuestionnaireResultMapper;
import com.lhs.mapper.survey.QuestionnaireStatisticsResultMapper;
import com.lhs.service.user.UserService;
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

    private final QuestionnaireStatisticsResultMapper questionnaireStatisticsResultMapper;

    public QuestionnaireService(RedisTemplate<String, String> redisTemplate,
                                QuestionnaireResultMapper questionnaireResultMapper,
                                RateLimiter rateLimiter,
                                UserService userService,
                                QuestionnaireStatisticsResultMapper questionnaireStatisticsResultMapper) {
        this.redisTemplate = redisTemplate;
        this.questionnaireResultMapper = questionnaireResultMapper;
        this.rateLimiter = rateLimiter;
        this.userService = userService;

        this.questionnaireStatisticsResultMapper = questionnaireStatisticsResultMapper;
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

    /**
     * 根据问卷编号统计数据
     *
     * @param questionnaireType 问卷编号
     * @param recordType        记录类型
     */
    public void statisticsQuestionnaireResult(QuestionnaireType questionnaireType, Integer recordType) {

        LambdaQueryWrapper<QuestionnaireResult> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(QuestionnaireResult::getQuestionnaireCode, questionnaireType.getCode());
        List<QuestionnaireResult> resultList = questionnaireResultMapper.selectList(lambdaQueryWrapper);

        int count = 0;

        count += resultList.size();

        if (count < 10) {
            LogUtils.info(questionnaireType.getType() + "问卷样本数量过少：" + count);
            return;
        }

        LogUtils.info(questionnaireType.getType() + "问卷统计的样本数量：" + count);

        HashMap<String, Integer> statisticsResult = new HashMap<>();
        for (QuestionnaireResult questionnaireResult : resultList) {
            String questionnaireContent = questionnaireResult.getContent();
            String[] split = questionnaireContent.split(",");
            for (String charId : split) {
                statisticsResult.put(charId, statisticsResult.getOrDefault(charId, 0) + 1);
            }
        }

        expireQuestionnaireStatisticsResult(questionnaireType);

        int finalCount = count;
        long timeStamp = System.currentTimeMillis();

        OperatorCarryStatisticsResultVO operatorCarryStatisticsResultVO = new OperatorCarryStatisticsResultVO();
        operatorCarryStatisticsResultVO.setUpdateTime(timeStamp);
        operatorCarryStatisticsResultVO.setSampleSize(finalCount);
        operatorCarryStatisticsResultVO.setQuestionnaireType(questionnaireType.getType());
        operatorCarryStatisticsResultVO.setQuestionnaireCode(questionnaireType.getCode());
        operatorCarryStatisticsResultVO.setList(new ArrayList<>());


        List<OperatorCarryVO> list = new ArrayList<>();
        statisticsResult.forEach((charId, v) -> {
            OperatorCarryVO operatorCarryVO = new OperatorCarryVO();
            operatorCarryVO.setCharId(charId);
            operatorCarryVO.setCarryCount(v);
            list.add(operatorCarryVO);

        });

        list.sort(Comparator.comparing(OperatorCarryVO::getCarryCount).reversed());

        operatorCarryStatisticsResultVO.setList(list);

        QuestionnaireStatisticsResult questionnaireStatisticsResult = new QuestionnaireStatisticsResult();
        questionnaireStatisticsResult.setId(idGenerator.nextId());
        questionnaireStatisticsResult.setQuestionnaireCode(questionnaireType.getCode());
        questionnaireStatisticsResult.setQuestionnaireType(questionnaireType.getType());
        questionnaireStatisticsResult.setRecordType(recordType);
        questionnaireStatisticsResult.setCreateTime(new Date());
        questionnaireStatisticsResult.setResult(JsonMapper.toJSONString(operatorCarryStatisticsResultVO));


        questionnaireStatisticsResultMapper.insert(questionnaireStatisticsResult);
    }


    private void expireQuestionnaireStatisticsResult(QuestionnaireType questionnaireType) {
        questionnaireStatisticsResultMapper.updateStatisticsResultRecordType(questionnaireType.getCode(), RecordType.EXPIRE.getCode(), RecordType.DISPLAY.getCode());
    }


    public void archivedQuestionnaireStatisticsResult(QuestionnaireType questionnaireType) {

        // 获取今天的开始时间和结束时间
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calendar.getTime();

        LambdaUpdateWrapper<QuestionnaireStatisticsResult> existQueryWrapper = new LambdaUpdateWrapper<>();
        existQueryWrapper.eq(QuestionnaireStatisticsResult::getRecordType, RecordType.ARCHIVED.getCode())
                .eq(QuestionnaireStatisticsResult::getQuestionnaireCode, questionnaireType.getCode())
                .ge(QuestionnaireStatisticsResult::getCreateTime, startOfDay)
                .le(QuestionnaireStatisticsResult::getCreateTime, endOfDay);


        boolean exists = questionnaireStatisticsResultMapper.exists(existQueryWrapper);
        if (exists) {
            LogUtils.info("问卷统计结果今日已归档");
            return;
        }

        LambdaUpdateWrapper<QuestionnaireStatisticsResult> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(QuestionnaireStatisticsResult::getRecordType, RecordType.DISPLAY.getCode());
        QuestionnaireStatisticsResult questionnaireStatisticsResult = questionnaireStatisticsResultMapper.selectOne(queryWrapper);
        questionnaireStatisticsResult.setId(idGenerator.nextId());
        questionnaireStatisticsResult.setRecordType(RecordType.ARCHIVED.getCode());

        questionnaireStatisticsResultMapper.insert(questionnaireStatisticsResult);
        LogUtils.info("问卷统计结果归档成功");
    }


    public void deleteExpireData() {
        LambdaQueryWrapper<QuestionnaireStatisticsResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QuestionnaireStatisticsResult::getRecordType, RecordType.EXPIRE.getCode());
        int delete = questionnaireStatisticsResultMapper.delete(queryWrapper);
        LogUtils.info("本次清理了" + delete + "条过期干员携带率统计数据");
    }


    public List<OperatorCarryStatisticsResultVO> getOperatorCarryStatisticsResult() {

        List<OperatorCarryStatisticsResultVO> voList = new ArrayList<>();

        LambdaQueryWrapper<QuestionnaireStatisticsResult> queryWrapperByMainAndSideStory = new LambdaQueryWrapper<>();
        queryWrapperByMainAndSideStory.eq(QuestionnaireStatisticsResult::getRecordType, RecordType.DISPLAY.getCode());
        queryWrapperByMainAndSideStory.eq(QuestionnaireStatisticsResult::getQuestionnaireCode,
                QuestionnaireType.MAIN_AND_SIDE_STORY_FOR_NEW_GAME.getCode());

        QuestionnaireStatisticsResult resultByMainAndSideStory = questionnaireStatisticsResultMapper
                .selectOne(queryWrapperByMainAndSideStory);

        if (resultByMainAndSideStory != null) {
            OperatorCarryStatisticsResultVO operatorCarryResultByMainAndSideStory = JsonMapper.parseObject(resultByMainAndSideStory.getResult(), new TypeReference<>() {
            });
            voList.add(operatorCarryResultByMainAndSideStory);
        }


        LambdaQueryWrapper<QuestionnaireStatisticsResult> queryWrapperByContingencyContract = new LambdaQueryWrapper<>();
        queryWrapperByContingencyContract.eq(QuestionnaireStatisticsResult::getRecordType, RecordType.DISPLAY.getCode());
        queryWrapperByContingencyContract.eq(QuestionnaireStatisticsResult::getQuestionnaireCode,
                QuestionnaireType.CONTINGENCY_CONTRACT_Mode_FOR_NEW_GAME.getCode());

        QuestionnaireStatisticsResult resultByContingencyContract = questionnaireStatisticsResultMapper
                .selectOne(queryWrapperByContingencyContract);

        if (resultByContingencyContract != null) {
            OperatorCarryStatisticsResultVO operatorCarryResultByContingencyContract = JsonMapper.parseObject(resultByContingencyContract.getResult(), new TypeReference<>() {
            });
            voList.add(operatorCarryResultByContingencyContract);
        }


        LambdaQueryWrapper<QuestionnaireStatisticsResult> queryWrapperByIntegratedStrategies = new LambdaQueryWrapper<>();
        queryWrapperByIntegratedStrategies.eq(QuestionnaireStatisticsResult::getRecordType, RecordType.DISPLAY.getCode());
        queryWrapperByIntegratedStrategies.eq(QuestionnaireStatisticsResult::getQuestionnaireCode,
                QuestionnaireType.INTEGRATED_STRATEGIES_FOR_NEW_GAME.getCode());
        QuestionnaireStatisticsResult resultByIntegratedStrategies = questionnaireStatisticsResultMapper
                .selectOne(queryWrapperByIntegratedStrategies);

        if (resultByIntegratedStrategies != null) {
            OperatorCarryStatisticsResultVO operatorCarryResultByIntegratedStrategies = JsonMapper.parseObject(resultByIntegratedStrategies.getResult(), new TypeReference<>() {
            });
            voList.add(operatorCarryResultByIntegratedStrategies);
        }


        return voList;
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
