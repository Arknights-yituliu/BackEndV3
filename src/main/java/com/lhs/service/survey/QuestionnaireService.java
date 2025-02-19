package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.enums.QuestionnaireType;
import com.lhs.common.enums.RecordType;
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
import com.lhs.entity.vo.survey.OperatorCarryResultVO;
import com.lhs.mapper.survey.OperatorCarryRateStatisticsMapper;
import com.lhs.mapper.survey.QuestionnaireResultMapper;
import com.lhs.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;


@Service
public class QuestionnaireService {

    private final RedisTemplate<String, String> redisTemplate;
    private final QuestionnaireResultMapper questionnaireResultMapper;

    private final IdGenerator idGenerator;
    private final RateLimiter rateLimiter;
    private final UserService userService;
    private final OperatorCarryRateStatisticsMapper operatorCarryRateStatisticsMapper;

    public QuestionnaireService(RedisTemplate<String, String> redisTemplate,
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

    
    public void uploadQuestionnaireResult(HttpServletRequest httpServletRequest, QuestionnaireSubmitInfoDTO questionnaireSubmitInfoDTO) {

        //获取提交者IP
        String ipAddress = AES.encrypt(IpUtil.getIpAddress(httpServletRequest), ConfigUtil.Secret);
        Long uid = userService.getUidByHttpServletRequest(httpServletRequest);
        //提交间隔不能短于5s，短于5s抛出异常
        rateLimiter.tryAcquire("SurveySubmitterIP:" + ipAddress, 1, 5, ResultCode.NOT_REPEAT_REQUESTS);



        //检查上传的干员是否有重复
        String result = getResultStr(questionnaireSubmitInfoDTO);
        //根据问卷类型和提交者ip对应的id查询以前的问卷结果
        LambdaQueryWrapper<QuestionnaireResult> questionnaireResultQueryWrapper = new LambdaQueryWrapper<>();
        questionnaireResultQueryWrapper
                .eq(QuestionnaireResult::getType, QuestionnaireType.SELECTED_OPERATOR_FOR_NEW_GAME.getCode())
                .eq(QuestionnaireResult::getUid, uid)
                .orderByDesc(QuestionnaireResult::getCreateTime)
                .last("limit 1");
        QuestionnaireResult lastQuestionnaireResult = questionnaireResultMapper.selectOne(questionnaireResultQueryWrapper);

        Date date = new Date();

        QuestionnaireResult questionnaireResult = new QuestionnaireResult();
        questionnaireResult.setId(idGenerator.nextId());
        questionnaireResult.setUid(uid);
        questionnaireResult.setType(QuestionnaireType.SELECTED_OPERATOR_FOR_NEW_GAME.getCode());
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
        long timeInterval = currentTimeStamp-lastQuestionnaireResult.getCreateTime().getTime();

        if(timeInterval<60*60*24*7*1000L) {
            lastQuestionnaireResult.setContent(result);
            lastQuestionnaireResult.setUpdateTime(date);
            questionnaireResult.setIp(ipAddress);
            questionnaireResultMapper.updateById(lastQuestionnaireResult);
            return;
        }

        questionnaireResultMapper.insert(questionnaireResult);
    }

    
    public void statisticsQuestionnaireResult(Integer questionnaireType,Integer recordType) {
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
        redisTemplate.opsForValue().set("OperatorCarryRateSampleSize", String.valueOf(count),12, TimeUnit.HOURS);
        redisTemplate.opsForValue().set("OperatorCarryRateUpdateTime", String.valueOf(new Date().getTime()),12, TimeUnit.HOURS);

        HashMap<String, Integer> statisticsResult = new HashMap<>();
        for (QuestionnaireResult questionnaireResult : resultList) {
            String questionnaireContent = questionnaireResult.getContent();
            String[] split = questionnaireContent.split(",");
            for (String charId : split) {
                statisticsResult.put(charId, statisticsResult.getOrDefault(charId, 0) + 1);
            }
        }

        operatorCarryRateStatisticsMapper.expireOldData(RecordType.EXPIRE.getCode(),RecordType.DISPLAY.getCode());

        Date date = new Date();
        int finalCount = count;

        statisticsResult.forEach((charId, v) -> {
            OperatorCarryRateStatistics operatorCarryRateStatistics = new OperatorCarryRateStatistics();
            operatorCarryRateStatistics.setId(idGenerator.nextId());
            operatorCarryRateStatistics.setCharId(charId);
            operatorCarryRateStatistics.setCarryRate((double)v/finalCount);
            operatorCarryRateStatistics.setCreateTime(date);
            operatorCarryRateStatistics.setRecordType(recordType);
            operatorCarryRateStatisticsList.add(operatorCarryRateStatistics);
        });

        operatorCarryRateStatisticsMapper.insertBatch(operatorCarryRateStatisticsList);
    }

    
    public void archivedOperatorCarryRateResult() {

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

        LambdaUpdateWrapper<OperatorCarryRateStatistics> existQueryWrapper = new LambdaUpdateWrapper<>();
        existQueryWrapper.eq(OperatorCarryRateStatistics::getRecordType,RecordType.ARCHIVED.getCode())
                .ge(OperatorCarryRateStatistics::getCreateTime,startOfDay)
                .le(OperatorCarryRateStatistics::getCreateTime,endOfDay);

        boolean exists = operatorCarryRateStatisticsMapper.exists(existQueryWrapper);
        if(exists){
            LogUtils.info("干员携带率统计结果今日已归档");
            return;
        }

        LambdaUpdateWrapper<OperatorCarryRateStatistics> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(OperatorCarryRateStatistics::getRecordType,RecordType.DISPLAY.getCode());
        List<OperatorCarryRateStatistics> list = operatorCarryRateStatisticsMapper.selectList(queryWrapper);
        for(OperatorCarryRateStatistics item:list){
            item.setId(idGenerator.nextId());
            item.setRecordType(RecordType.ARCHIVED.getCode());
        }

        Integer i = operatorCarryRateStatisticsMapper.insertBatch(list);
        LogUtils.info("干员携带率统计结果归档成功"+i+"条");
    }


    public void deleteExpireData(){
        LambdaQueryWrapper<OperatorCarryRateStatistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperatorCarryRateStatistics::getRecordType,RecordType.EXPIRE.getCode());
        int delete = operatorCarryRateStatisticsMapper.delete(queryWrapper);
        LogUtils.info("本次清理了"+delete+"条过期干员携带率统计数据");
    }
    
    public OperatorCarryResultVO getQuestionnaireResultByType(Integer questionnaireType) {

        List<OperatorCarryRateStatisticsVO> list = operatorCarryRateStatisticsMapper.getOperatorCarryRateResult();
        OperatorCarryResultVO operatorCarryResultVO = new OperatorCarryResultVO();
        if(list.isEmpty()){
           return operatorCarryResultVO;
        }

        operatorCarryResultVO.setList(list);
        String sampleSize = redisTemplate.opsForValue().get("OperatorCarryRateSampleSize");
        if(sampleSize!=null){
            operatorCarryResultVO.setSampleSize(Integer.parseInt(sampleSize));
        }
        String updateTime = redisTemplate.opsForValue().get("OperatorCarryRateUpdateTime");
        if(updateTime!=null){
            operatorCarryResultVO.setUpdateTime(Long.parseLong(updateTime));
        }

        return operatorCarryResultVO;
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
