package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.enums.*;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.survey.OperatorCarryRateDailyDataRequestParamsDTO;
import com.lhs.entity.po.survey.OperatorCarryRate;
import com.lhs.entity.po.survey.OperatorCarryRateStatistics;
import com.lhs.entity.po.survey.QuestionnaireResult;
import com.lhs.entity.tmp.QuestionnaireResultDTO;
import com.lhs.entity.vo.survey.CarryRateDailyDataVO;
import com.lhs.entity.vo.survey.OperatorCarryRateDailyDataVO;
import com.lhs.entity.vo.survey.OperatorCarryRateStatisticsVO;
import com.lhs.entity.vo.survey.OperatorCarryRateVO;
import com.lhs.mapper.survey.OperatorCarryRateMapper;
import com.lhs.mapper.survey.OperatorCarryRateStatisticsMapper;
import com.lhs.mapper.survey.QuestionnaireResultMapper;

import org.jacoco.agent.rt.RT;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OperatorCarryRateService {


    private final QuestionnaireResultMapper questionnaireResultMapper;

    private final IdGenerator idGenerator;

    private final OperatorCarryRateMapper operatorCarryRateMapper;

    private final OperatorCarryRateStatisticsMapper operatorCarryRateStatisticsMapper;


    public OperatorCarryRateService(QuestionnaireResultMapper questionnaireResultMapper,
                                    OperatorCarryRateMapper operatorCarryRateMapper,
                                    OperatorCarryRateStatisticsMapper operatorCarryRateStatisticsMapper) {
        this.questionnaireResultMapper = questionnaireResultMapper;
        this.operatorCarryRateMapper = operatorCarryRateMapper;
        this.operatorCarryRateStatisticsMapper = operatorCarryRateStatisticsMapper;
        this.idGenerator = new IdGenerator(1L);
    }


    public void statisticsTodayOperatorCarryRate() {
        QuestionnaireType[] questionnaireTypeArr = new QuestionnaireType[]{
                QuestionnaireType.MAIN_AND_SIDE_STORY_FOR_NEW_GAME,
                QuestionnaireType.CONTINGENCY_CONTRACT_Mode_FOR_NEW_GAME,
                QuestionnaireType.INTEGRATED_STRATEGIES_FOR_NEW_GAME
        };

        long currentTime = TimeUtil.getCurrentDayTime().getTime();
        Date startTime = new Date(currentTime);
        Date endTime = new Date(currentTime + UnitTime.ONE_DAY.milliseconds());

        for (QuestionnaireType questionnaireType : questionnaireTypeArr) {
            statisticsOperatorCarryRate(startTime, endTime, questionnaireType);
        }
    }


    public void statisticsYesterdayOperatorCarryRate() {
        QuestionnaireType[] questionnaireTypeArr = new QuestionnaireType[]{
                QuestionnaireType.MAIN_AND_SIDE_STORY_FOR_NEW_GAME,
                QuestionnaireType.CONTINGENCY_CONTRACT_Mode_FOR_NEW_GAME,
                QuestionnaireType.INTEGRATED_STRATEGIES_FOR_NEW_GAME
        };

        long currentTime = TimeUtil.getCurrentDayTime().getTime();
        Date startTime = new Date(currentTime - UnitTime.ONE_DAY.milliseconds());
        Date endTime = new Date(currentTime);

        for (QuestionnaireType questionnaireType : questionnaireTypeArr) {
            statisticsOperatorCarryRate(startTime, endTime, questionnaireType);
        }

    }


    public void statisticsOperatorCarryRate(Date startTime, Date endTime, QuestionnaireType questionnaireType) {

        Date date = new Date();

        LogUtils.info("开始统计" + questionnaireType.type() + "问卷");
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
        Date dataStartTime = resultList.get(0).getUpdateTime();
        Date dataEndTime = resultList.get(resultList.size() - 1).getUpdateTime();

        LogUtils.info(questionnaireType.type() + "问卷样本量：" + count);
        HashMap<String, OperatorCarryRateVO> statisticsResult = new HashMap<>();
        for (QuestionnaireResult questionnaireResult : resultList) {
            String questionnaireContent = questionnaireResult.getContent();
            String[] split = questionnaireContent.split(",");
            for (String charId : split) {
                if (statisticsResult.get(charId) != null) {
                    statisticsResult.get(charId).incrementCarryCount(1);
                } else {
                    OperatorCarryRateVO operatorCarryRateVO = new OperatorCarryRateVO();
                    operatorCarryRateVO.setSampleSize(count);
                    operatorCarryRateVO.setCarryCount(1);
                    operatorCarryRateVO.setCharId(charId);
                    statisticsResult.put(charId, operatorCarryRateVO);
                }
            }
        }


        LambdaUpdateWrapper<OperatorCarryRate> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.set(OperatorCarryRate::getRecordType, RecordType.EXPIRE.code())
                .eq(OperatorCarryRate::getQuestionnaireCode, questionnaireType.code())
                .ge(OperatorCarryRate::getStartTime, startTime)
                .lt(OperatorCarryRate::getEndTime, endTime);

        int update = operatorCarryRateMapper.update(null, updateWrapper);
        LogUtils.info("过期了" + update + "条携带率数据");


        List<OperatorCarryRate> operatorCarryRateList = new ArrayList<>();
        statisticsResult.forEach((charId, vo) -> {
            OperatorCarryRate operatorCarryRate = new OperatorCarryRate();
            operatorCarryRate.setId(idGenerator.nextId());
            operatorCarryRate.setCharId(charId);
            operatorCarryRate.setCarryCount(vo.getCarryCount());
            operatorCarryRate.setSampleSize(vo.getSampleSize());
            operatorCarryRate.setCreateTime(date);
            operatorCarryRate.setStartTime(dataStartTime);
            operatorCarryRate.setEndTime(dataEndTime);
            operatorCarryRate.setRecordType(RecordType.DISPLAY.code());
            operatorCarryRate.setQuestionnaireCode(questionnaireType.code());
            operatorCarryRateList.add(operatorCarryRate);
        });
        operatorCarryRateMapper.insertBatch(operatorCarryRateList);


        List<OperatorCarryRateVO> list = statisticsResult.values().stream()
                .sorted(Comparator.comparing(OperatorCarryRateVO::getCarryCount).reversed())
                .toList();


        OperatorCarryRateStatisticsVO operatorCarryRateStatisticsVO = new OperatorCarryRateStatisticsVO();
        operatorCarryRateStatisticsVO.setSampleSize(count);
        operatorCarryRateStatisticsVO.setList(new ArrayList<>());

        operatorCarryRateStatisticsVO.setList(list);

        //新的统计结果
        OperatorCarryRateStatistics operatorCarryRateStatistics = new OperatorCarryRateStatistics();
        operatorCarryRateStatistics.setId(idGenerator.nextId());
        operatorCarryRateStatistics.setQuestionnaireCode(questionnaireType.code());
        operatorCarryRateStatistics.setQuestionnaireType(questionnaireType.type());
        operatorCarryRateStatistics.setTimeGranularity(TimeGranularity.DAY.code());
        operatorCarryRateStatistics.setStartTime(dataStartTime);
        operatorCarryRateStatistics.setEndTime(dataEndTime);
        operatorCarryRateStatistics.setCreateTime(date);
        operatorCarryRateStatistics.setResult(JsonMapper.toJSONString(operatorCarryRateStatisticsVO));


        if (statistics != null) {
            LogUtils.info("更新了一条携带优先级统计数据");
            operatorCarryRateStatistics.setId(statistics.getId());
            operatorCarryRateStatisticsMapper.updateById(operatorCarryRateStatistics);
        } else {
            LogUtils.info("新增了一条携带优先级统计数据");
            operatorCarryRateStatisticsMapper.insert(operatorCarryRateStatistics);
        }
    }


    public void deleteExpireData() {
        LambdaQueryWrapper<OperatorCarryRate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperatorCarryRate::getRecordType, RecordType.EXPIRE.code());
        int delete = operatorCarryRateMapper.delete(queryWrapper);
        LogUtils.info("删除了" + delete + "条携带率数据");
    }


    public OperatorCarryRateStatisticsVO getOperatorCarryRate(Integer questionnaireCode, Date startTime, Date endTime) {
        LambdaQueryWrapper<OperatorCarryRateStatistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperatorCarryRateStatistics::getQuestionnaireCode, questionnaireCode)
                .ge(OperatorCarryRateStatistics::getStartTime, startTime)
                .le(OperatorCarryRateStatistics::getEndTime, endTime);

        List<OperatorCarryRateStatistics> operatorCarryRateStatisticsList = operatorCarryRateStatisticsMapper.selectList(queryWrapper);


        int sampleSize = 0;
        HashMap<String, Integer> statisticsResult = new HashMap<>();
        for (OperatorCarryRateStatistics operatorCarryRateStatistics : operatorCarryRateStatisticsList) {
            String result = operatorCarryRateStatistics.getResult();
            OperatorCarryRateStatisticsVO vo = JsonMapper.parseObject(result, new TypeReference<>() {
            });
            sampleSize += vo.getSampleSize();
            List<OperatorCarryRateVO> list = vo.getList();
            for (OperatorCarryRateVO operatorCarryRateVO : list) {
                statisticsResult.put(operatorCarryRateVO.getCharId(), statisticsResult.getOrDefault(operatorCarryRateVO.getCharId(), 0) + operatorCarryRateVO.getCarryCount());
            }
        }

        List<OperatorCarryRateVO> voList = new ArrayList<>();

        statisticsResult.forEach((charId, v) -> {
            OperatorCarryRateVO operatorCarryRateVO = new OperatorCarryRateVO();
            operatorCarryRateVO.setCharId(charId);
            operatorCarryRateVO.setCarryCount(v);
            voList.add(operatorCarryRateVO);
        });

        int minCarryCount = (int) (sampleSize * 0.03);

        List<OperatorCarryRateVO> list = voList.stream().filter(e -> e.getCarryCount() > minCarryCount).sorted(Comparator.comparing(OperatorCarryRateVO::getCarryCount).reversed()).toList();
        OperatorCarryRateStatisticsVO operatorCarryRateStatisticsVO = new OperatorCarryRateStatisticsVO();
        operatorCarryRateStatisticsVO.setSampleSize(sampleSize);
        operatorCarryRateStatisticsVO.setList(list);

        return operatorCarryRateStatisticsVO;

    }


    public void moveDate() {

        List<QuestionnaireResultDTO> oldData = operatorCarryRateStatisticsMapper.getOldData();
        for (QuestionnaireResultDTO dto : oldData) {
            QuestionnaireResult questionnaireResult = new QuestionnaireResult();
            questionnaireResult.setId(dto.getId());
            questionnaireResult.setUid(dto.getUid());
            questionnaireResult.setQuestionnaireCode(QuestionnaireType.MAIN_AND_SIDE_STORY_FOR_NEW_GAME.code());
            questionnaireResult.setContent(dto.getContent());
            questionnaireResult.setCreateTime(dto.getCreateTime());
            questionnaireResult.setUpdateTime(dto.getUpdateTime());
            questionnaireResult.setIp(dto.getIp());
            questionnaireResultMapper.insert(questionnaireResult);
        }
    }


    public OperatorCarryRateDailyDataVO getOperatorCarryRateLineChart(OperatorCarryRateDailyDataRequestParamsDTO operatorCarryRateDailyDataRequestParamsDTO) {

        LambdaQueryWrapper<OperatorCarryRate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OperatorCarryRate::getCharId, operatorCarryRateDailyDataRequestParamsDTO.getCharId())
                .eq(OperatorCarryRate::getRecordType,RecordType.DISPLAY.code())
                .eq(OperatorCarryRate::getQuestionnaireCode, operatorCarryRateDailyDataRequestParamsDTO.getQuestionnaireCode())
                .ge(OperatorCarryRate::getStartTime, new Date(operatorCarryRateDailyDataRequestParamsDTO.getStart()))
                .le(OperatorCarryRate::getEndTime, new Date(operatorCarryRateDailyDataRequestParamsDTO.getEnd()));
        List<OperatorCarryRate> operatorCarryRateList = operatorCarryRateMapper.selectList(queryWrapper);

        if (operatorCarryRateList.isEmpty()) {
            throw new ServiceException(ResultCode.DATA_NONE);
        }

        OperatorCarryRateDailyDataVO operatorCarryRateDailyDataVO = new OperatorCarryRateDailyDataVO();
        List<CarryRateDailyDataVO> carryRateDailyDataVOList = new ArrayList<>();
        List<Long> dateList = new ArrayList<>();

        operatorCarryRateList.stream()
                .sorted(Comparator.comparing(OperatorCarryRate::getStartTime))
                .filter(e->e.getSampleSize()>0&&e.getCarryCount()>0)
                .forEach(e -> {
//                    System.out.println(e);
                    CarryRateDailyDataVO carryRateDailyDataVO = new CarryRateDailyDataVO();
                    carryRateDailyDataVO.setCarryCount(e.getCarryCount());
                    carryRateDailyDataVO.setSampleSize(e.getSampleSize());
                    carryRateDailyDataVOList.add(carryRateDailyDataVO);
                    dateList.add(e.getStartTime().getTime());
                });

        operatorCarryRateDailyDataVO.setDate(dateList);
        operatorCarryRateDailyDataVO.setList(carryRateDailyDataVOList);
        operatorCarryRateDailyDataVO.setCharId(operatorCarryRateDailyDataVO.getCharId());

        return operatorCarryRateDailyDataVO;
    }
}
