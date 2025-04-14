package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.enums.QuestionnaireType;
import com.lhs.common.enums.RecordType;
import com.lhs.common.enums.TimeGranularity;
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
     * @param startTime         统计起始时间
     * @param questionnaireType 问卷编号
     * @param timeGranularity   时间粒度
     */
    public void statisticsQuestionnaireResult(Date startTime, QuestionnaireType questionnaireType, TimeGranularity timeGranularity) {

        LambdaQueryWrapper<QuestionnaireResult> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(QuestionnaireResult::getQuestionnaireCode, questionnaireType.getCode());
        if (startTime != null) {
            lambdaQueryWrapper.ge(QuestionnaireResult::getCreateTime, startTime);
        }
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

        //新的统计结果
        QuestionnaireStatisticsResult questionnaireStatisticsResult = new QuestionnaireStatisticsResult();
        questionnaireStatisticsResult.setId(idGenerator.nextId());
        questionnaireStatisticsResult.setQuestionnaireCode(questionnaireType.getCode());
        questionnaireStatisticsResult.setQuestionnaireType(questionnaireType.getType());
        questionnaireStatisticsResult.setRecordType(RecordType.DISPLAY.code());
        questionnaireStatisticsResult.setTimeGranularity(timeGranularity.code());
        String version = questionnaireStatisticsResult.createVersion();
        questionnaireStatisticsResult.setVersion(version);
        questionnaireStatisticsResult.setCreateTime(new Date());

        expireQuestionnaireStatisticsResult(version);

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


        questionnaireStatisticsResult.setResult(JsonMapper.toJSONString(operatorCarryStatisticsResultVO));


        questionnaireStatisticsResultMapper.insert(questionnaireStatisticsResult);
    }


    private void expireQuestionnaireStatisticsResult(String version) {
        questionnaireStatisticsResultMapper.updateRecordType(RecordType.EXPIRE.code(), version);
    }


    public void archivedQuestionnaireStatisticsResult(QuestionnaireType questionnaireType) {
        LogUtils.info(questionnaireType.getType() + "问卷统计开始归档");

        // 获取今天的开始时间和结束时间
        Calendar calendar1 = Calendar.getInstance();
        calendar1.set(Calendar.HOUR_OF_DAY, 0);
        calendar1.set(Calendar.MINUTE, 0);
        calendar1.set(Calendar.SECOND, 0);
        calendar1.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar1.getTime();

        Calendar calendar2 = Calendar.getInstance();
        calendar2.set(Calendar.HOUR_OF_DAY, 23);
        calendar2.set(Calendar.MINUTE, 59);
        calendar2.set(Calendar.SECOND, 59);
        calendar2.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calendar2.getTime();

        QuestionnaireStatisticsResult archivedQuestionnaireStatisticsResult = new QuestionnaireStatisticsResult();
        archivedQuestionnaireStatisticsResult.setQuestionnaireCode(questionnaireType.getCode());
        archivedQuestionnaireStatisticsResult.setRecordType(RecordType.ARCHIVED.code());
        archivedQuestionnaireStatisticsResult.setTimeGranularity(TimeGranularity.FROM_INCEPTION_TO_PRESENT.code());
        String archivedVersion = archivedQuestionnaireStatisticsResult.createVersion();

        QuestionnaireStatisticsResult displayQuestionnaireStatisticsResult = new QuestionnaireStatisticsResult();
        displayQuestionnaireStatisticsResult.setQuestionnaireCode(questionnaireType.getCode());
        displayQuestionnaireStatisticsResult.setRecordType(RecordType.DISPLAY.code());
        displayQuestionnaireStatisticsResult.setTimeGranularity(TimeGranularity.FROM_INCEPTION_TO_PRESENT.code());
        String displayVersion = displayQuestionnaireStatisticsResult.createVersion();

        LambdaUpdateWrapper<QuestionnaireStatisticsResult> existQueryWrapper = new LambdaUpdateWrapper<>();
        existQueryWrapper.eq(QuestionnaireStatisticsResult::getVersion, archivedVersion)
                .ge(QuestionnaireStatisticsResult::getCreateTime, startOfDay)
                .le(QuestionnaireStatisticsResult::getCreateTime, endOfDay);


        boolean exists = questionnaireStatisticsResultMapper.exists(existQueryWrapper);

        if (exists) {
            LogUtils.info(questionnaireType.getType() + "问卷统计结果今日已归档");
            return;
        }


        QuestionnaireStatisticsResult questionnaireStatisticsResult = questionnaireStatisticsResultMapper.getLastData(displayVersion);
        questionnaireStatisticsResult.setId(idGenerator.nextId());
        questionnaireStatisticsResult.setRecordType(RecordType.ARCHIVED.code());
        questionnaireStatisticsResult.setVersion(questionnaireStatisticsResult.getVersion());

        questionnaireStatisticsResultMapper.insert(questionnaireStatisticsResult);
        LogUtils.info(questionnaireType.getType() + "问卷统计结果归档成功");
    }


    public void deleteExpireData() {
        LambdaQueryWrapper<QuestionnaireStatisticsResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QuestionnaireStatisticsResult::getRecordType, RecordType.EXPIRE.code());
        int delete = questionnaireStatisticsResultMapper.delete(queryWrapper);
        LogUtils.info("本次清理了" + delete + "条过期干员携带率统计数据");
    }

    public OperatorCarryStatisticsResultVO getOperatorCarryStatisticsResultByTypeAndTime(Integer questionnaireType, Integer timeGranularity) {

        LambdaQueryWrapper<QuestionnaireStatisticsResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QuestionnaireStatisticsResult::getRecordType, RecordType.DISPLAY.code())
                .eq(QuestionnaireStatisticsResult::getQuestionnaireCode, questionnaireType)
                .eq(QuestionnaireStatisticsResult::getTimeGranularity, timeGranularity)
                .orderByDesc(QuestionnaireStatisticsResult::getCreateTime);
        List<QuestionnaireStatisticsResult> questionnaireStatisticsResultList = questionnaireStatisticsResultMapper.selectList(queryWrapper);

        if (questionnaireStatisticsResultList.isEmpty()) {
            throw new ServiceException(ResultCode.DATA_NONE);
        }
        String result = questionnaireStatisticsResultList.get(0).getResult();

        OperatorCarryStatisticsResultVO operatorCarryStatisticsResultVO = JsonMapper.parseObject(result, new TypeReference<>() {
        });


        operatorCarryStatisticsResultVO.setList(operatorCarryStatisticsResultVO.getList().subList(0,70));
        return operatorCarryStatisticsResultVO;
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
