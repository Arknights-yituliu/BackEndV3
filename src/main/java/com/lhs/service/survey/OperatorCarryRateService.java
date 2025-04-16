package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.enums.QuestionnaireType;
import com.lhs.common.enums.TimeGranularity;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.LogUtils;
import com.lhs.common.util.RateLimiter;
import com.lhs.entity.po.survey.OperatorCarryRateStatistics;
import com.lhs.entity.po.survey.QuestionnaireResult;
import com.lhs.entity.vo.survey.OperatorCarryRateStatisticsVO;
import com.lhs.entity.vo.survey.OperatorCarryVO;
import com.lhs.mapper.survey.OperatorCarryRateStatisticsMapper;
import com.lhs.mapper.survey.QuestionnaireResultMapper;
import com.lhs.mapper.survey.QuestionnaireStatisticsResultMapper;
import com.lhs.service.user.UserService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OperatorCarryRateService {

    private final RedisTemplate<String, String> redisTemplate;
    private final QuestionnaireResultMapper questionnaireResultMapper;

    private final IdGenerator idGenerator;
    private final RateLimiter rateLimiter;
    private final UserService userService;

    private final OperatorCarryRateStatisticsMapper operatorCarryRateStatisticsMapper;

    private final QuestionnaireStatisticsResultMapper questionnaireStatisticsResultMapper;

    public OperatorCarryRateService(RedisTemplate<String, String> redisTemplate,
                                    QuestionnaireResultMapper questionnaireResultMapper,
                                    RateLimiter rateLimiter,
                                    UserService userService,
                                    OperatorCarryRateStatisticsMapper operatorCarryRateStatisticsMapper,
                                    QuestionnaireStatisticsResultMapper questionnaireStatisticsResultMapper) {
        this.redisTemplate = redisTemplate;
        this.questionnaireResultMapper = questionnaireResultMapper;
        this.rateLimiter = rateLimiter;
        this.userService = userService;
        this.operatorCarryRateStatisticsMapper = operatorCarryRateStatisticsMapper;

        this.questionnaireStatisticsResultMapper = questionnaireStatisticsResultMapper;
        this.idGenerator = new IdGenerator(1L);
    }


    public void statisticsOperatorCarryRate(Date startTime, Date endTime, QuestionnaireType questionnaireType) {
         LogUtils.info("开始统计"+questionnaireType.type()+"问卷");
        LambdaQueryWrapper<OperatorCarryRateStatistics> statisticsQueryWrapper = new LambdaQueryWrapper<>();
        statisticsQueryWrapper.eq(OperatorCarryRateStatistics::getQuestionnaireCode, questionnaireType.code())
                .ge(OperatorCarryRateStatistics::getStartTime, startTime)
                .le(OperatorCarryRateStatistics::getEndTime, endTime);
        OperatorCarryRateStatistics statistics = operatorCarryRateStatisticsMapper.selectOne(statisticsQueryWrapper);


        LambdaQueryWrapper<QuestionnaireResult> resultQueryWrapper = new LambdaQueryWrapper<>();
        resultQueryWrapper.eq(QuestionnaireResult::getQuestionnaireCode, questionnaireType.code())
                .ge(QuestionnaireResult::getUpdateTime, startTime)
                .lt(QuestionnaireResult::getUpdateTime, endTime)
                .orderByAsc(QuestionnaireResult::getUpdateTime);


        List<QuestionnaireResult> resultList = questionnaireResultMapper.selectList(resultQueryWrapper);
        if (resultList.isEmpty()) {
            LogUtils.info(questionnaireType.type() + "没有问卷数据");
            return;
        }

        int count = resultList.size();

        LogUtils.info(questionnaireType.type() + "问卷样本量：" + count);
        HashMap<String, Integer> statisticsResult = new HashMap<>();
        for (QuestionnaireResult questionnaireResult : resultList) {
            String questionnaireContent = questionnaireResult.getContent();
            String[] split = questionnaireContent.split(",");
            for (String charId : split) {
                statisticsResult.put(charId, statisticsResult.getOrDefault(charId, 0) + 1);
            }
        }


        //新的统计结果
        OperatorCarryRateStatistics operatorCarryRateStatistics = new OperatorCarryRateStatistics();
        operatorCarryRateStatistics.setId(idGenerator.nextId());
        operatorCarryRateStatistics.setQuestionnaireCode(questionnaireType.code());
        operatorCarryRateStatistics.setQuestionnaireType(questionnaireType.type());
        operatorCarryRateStatistics.setTimeGranularity(TimeGranularity.DAY.code());
        operatorCarryRateStatistics.setStartTime(resultList.get(0).getUpdateTime());
        operatorCarryRateStatistics.setEndTime(resultList.get(resultList.size()-1).getUpdateTime());
        operatorCarryRateStatistics.setCreateTime(new Date());


        long timeStamp = System.currentTimeMillis();

        OperatorCarryRateStatisticsVO operatorCarryRateStatisticsVO = new OperatorCarryRateStatisticsVO();
        operatorCarryRateStatisticsVO.setSampleSize(count);
        operatorCarryRateStatisticsVO.setList(new ArrayList<>());

        List<OperatorCarryVO> list = new ArrayList<>();
        statisticsResult.forEach((charId, v) -> {
            OperatorCarryVO operatorCarryVO = new OperatorCarryVO();
            operatorCarryVO.setCharId(charId);
            operatorCarryVO.setCarryCount(v);
            list.add(operatorCarryVO);
        });

        list.sort(Comparator.comparing(OperatorCarryVO::getCarryCount).reversed());

        operatorCarryRateStatisticsVO.setList(list);

        operatorCarryRateStatistics.setResult(JsonMapper.toJSONString(operatorCarryRateStatisticsVO));


        if(statistics!=null){
            LogUtils.info("更新了一条携带优先级统计数据");
            operatorCarryRateStatistics.setId(statistics.getId());
            operatorCarryRateStatisticsMapper.updateById(operatorCarryRateStatistics);
        }else {
            LogUtils.info("新增了一条携带优先级统计数据");
            operatorCarryRateStatisticsMapper.insert(operatorCarryRateStatistics);
        }

    }


    public OperatorCarryRateStatisticsVO getOperatorCarryRate(Integer questionnaireCode, Date startTime, Date endTime) {
        LambdaQueryWrapper<OperatorCarryRateStatistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperatorCarryRateStatistics::getQuestionnaireCode,questionnaireCode)
                .ge(OperatorCarryRateStatistics::getStartTime,startTime)
                .le(OperatorCarryRateStatistics::getEndTime,endTime);

        List<OperatorCarryRateStatistics> operatorCarryRateStatisticsList = operatorCarryRateStatisticsMapper.selectList(queryWrapper);


        int sampleSize = 0;
        HashMap<String, Integer> statisticsResult = new HashMap<>();
        for(OperatorCarryRateStatistics operatorCarryRateStatistics:operatorCarryRateStatisticsList){
            String result = operatorCarryRateStatistics.getResult();
            OperatorCarryRateStatisticsVO vo = JsonMapper.parseObject(result, new TypeReference<>() {});
            sampleSize+=vo.getSampleSize();
            List<OperatorCarryVO> list = vo.getList();
            for(OperatorCarryVO operatorCarryVO:list){
                statisticsResult.put(operatorCarryVO.getCharId(), statisticsResult.getOrDefault(operatorCarryVO.getCharId(), 0) + operatorCarryVO.getCarryCount());
            }
        }

        List<OperatorCarryVO> voList = new ArrayList<>();

        statisticsResult.forEach((charId, v) -> {
            OperatorCarryVO operatorCarryVO = new OperatorCarryVO();
            operatorCarryVO.setCharId(charId);
            operatorCarryVO.setCarryCount(v);
            voList.add(operatorCarryVO);
        });

        int minCarryCount = (int) (sampleSize*0.03);

        List<OperatorCarryVO> list = voList.stream().filter(e -> e.getCarryCount() > minCarryCount).sorted(Comparator.comparing(OperatorCarryVO::getCarryCount).reversed()).toList();
        OperatorCarryRateStatisticsVO operatorCarryRateStatisticsVO = new OperatorCarryRateStatisticsVO();
        operatorCarryRateStatisticsVO.setSampleSize(sampleSize);
        operatorCarryRateStatisticsVO.setList(list);

        return operatorCarryRateStatisticsVO;

    }


}
