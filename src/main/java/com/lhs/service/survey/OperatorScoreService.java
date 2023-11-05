package com.lhs.service.survey;

import com.lhs.entity.po.survey.OperatorScore;
import com.lhs.entity.po.survey.OperatorScoreStatistics;
import com.lhs.entity.po.survey.SurveyUser;
import com.lhs.mapper.survey.OperatorScoreMapper;
import com.lhs.entity.vo.survey.OperatorScoreVO;
import com.lhs.entity.vo.survey.OperatorScoreStatisticsVO;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OperatorScoreService {


    private OperatorScoreMapper operatorScoreMapper;
    private SurveyUserService surveyUserService;
    private final RedisTemplate<String,Object> redisTemplate;

    public OperatorScoreService(OperatorScoreMapper operatorScoreMapper, SurveyUserService surveyUserService, RedisTemplate<String, Object> redisTemplate) {
        this.operatorScoreMapper = operatorScoreMapper;
        this.surveyUserService = surveyUserService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 上传干员风评表
     *
     * @param token           token
     * @param operatorScoreList 干员风评表
     * @return 成功消息
     */
//    @TakeCount(name = "上传评分")
    public HashMap<Object, Object> uploadScoreForm(String token, List<OperatorScore> operatorScoreList) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        long uid = surveyUser.getId();
        String tableName = "survey_score_" + surveyUserService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表

        int affectedRows = 0;

        //用户之前上传的数据
        List<OperatorScore> operatorScores
                = operatorScoreMapper.selectSurveyScoreByUid(tableName, surveyUser.getId());

        //用户之前上传的数据转为map方便对比
        Map<String, OperatorScore> oldDataCollectById = operatorScores.stream()
                .collect(Collectors.toMap(OperatorScore::getCharId, Function.identity()));

        //新增数据
        List<OperatorScore> insertList = new ArrayList<>();

        for (OperatorScore operatorScore : operatorScoreList) {

            //和老数据进行对比
            OperatorScore savedData = oldDataCollectById.get(operatorScore.getCharId());
            //为空则新增
            if (savedData == null) {
                Long scoreId = redisTemplate.opsForValue().increment("CharacterId");
                operatorScore.setId(scoreId);
                operatorScore.setUid(uid);
                insertList.add(operatorScore);  //加入批量插入集合
                affectedRows++;  //新增数据条数
            } else {
                //如果数据存在,进行更新
                    affectedRows++;  //更新数据条数
                    operatorScore.setId(savedData.getId());
                    operatorScore.setUid(uid);
                    operatorScoreMapper.updateSurveyScoreById(tableName, operatorScore); //更新数据
            }
        }

        if (insertList.size() > 0) operatorScoreMapper.insertBatchSurveyScore(tableName, insertList);  //批量插入
        Date date = new Date();
        surveyUser.setUpdateTime(date);   //更新用户最后一次上传时间
        surveyUserService.backupSurveyUser(surveyUser);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("affectedRows", affectedRows);
        hashMap.put("updateTime", simpleDateFormat.format(date));
        return hashMap;

    }



    public void scoreStatisticsNew() {
        List<Long> userIds = new ArrayList<>();

        List<List<Long>> userIdsGroup = new ArrayList<>();
        String update_time = new SimpleDateFormat("yyyy-MM-dd").format(new Date());


        int length = userIds.size();
        // 计算用户id按300个用户一组可以分成多少组
        int num = length / 300 + 1;
        int fromIndex = 0;   // id分组开始
        int toIndex = 300;   //id分组结束
        for (int i = 0; i < num; i++) {
            toIndex = Math.min(toIndex, userIds.size());
            userIdsGroup.add(userIds.subList(fromIndex, toIndex));
            fromIndex += 300;
            toIndex += 300;
        }
        operatorScoreMapper.truncateScoreStatisticsTable();  //清空统计表

        HashMap<String, OperatorScoreStatistics> scratchResult = new HashMap<>();
        List<OperatorScoreStatistics> statisticsResultList = new ArrayList<>();


        for (List<Long> longs : userIdsGroup) {
            List<OperatorScoreVO> operatorScoreVOList_DB =
                    operatorScoreMapper.selectSurveyScoreVoByUidList("survey_score_1", longs);

            log.info("本次统计数量：" + operatorScoreVOList_DB.size());

            Map<String, List<OperatorScoreVO>> collectByCharId = operatorScoreVOList_DB.stream()
                    .collect(Collectors.groupingBy(OperatorScoreVO::getCharId));

            collectByCharId.forEach((charId, arr) -> {
                int sampleSizeDaily = 0;
                int sampleSizeRogue = 0;
                int sampleSizeSecurityService = 0;
                int sampleSizeHard = 0;
                int sampleSizeUniversal = 0;
                int sampleSizeCounter = 0;
                int sampleSizeBuilding = 0;
                int sampleSizeComprehensive = 0;

                int daily = 0;
                int rogue = 0;
                int hard = 0;
                int securityService = 0;
                int universal = 0;
                int counter = 0;
                int building = 0;
                int comprehensive = 0;

                int rarity = arr.get(0).getRarity();

                for (OperatorScoreVO operatorScoreVo : arr) {
                    if (operatorScoreVo.getDaily() > 0) {
                        sampleSizeDaily++;
                        daily+= operatorScoreVo.getDaily();
                    }
                    if (operatorScoreVo.getRogue() > 0) {
                        sampleSizeRogue++;
                        rogue+= operatorScoreVo.getRogue();
                    }
                    if (operatorScoreVo.getSecurityService() > 0) {
                        sampleSizeSecurityService++;
                        securityService+= operatorScoreVo.getSecurityService();
                    }
                    if (operatorScoreVo.getHard() > 0) {
                        sampleSizeHard++;
                        hard+= operatorScoreVo.getHard();
                    }
                    if (operatorScoreVo.getUniversal() > 0) {
                        sampleSizeUniversal++;
                        universal+= operatorScoreVo.getUniversal();
                    }
                    if (operatorScoreVo.getCounter() > 0) {
                        sampleSizeCounter++;
                        counter+= operatorScoreVo.getCounter();
                    }
                    if (operatorScoreVo.getBuilding() > 0) {
                        sampleSizeBuilding++;
                        building+= operatorScoreVo.getBuilding();
                    }
                    if (operatorScoreVo.getComprehensive() > 0) {
                        sampleSizeComprehensive++;
                        comprehensive+= operatorScoreVo.getComprehensive();
                    }

                }


                //和上次计算结果合并
                if (scratchResult.get(charId) != null) {
                    OperatorScoreStatistics operatorScoreStatistics = scratchResult.get(charId);
                    daily += operatorScoreStatistics.getDaily();
                    sampleSizeDaily += operatorScoreStatistics.getSampleSizeDaily();

                    rogue += operatorScoreStatistics.getRogue();
                    sampleSizeRogue += operatorScoreStatistics.getSampleSizeRogue();

                    hard += operatorScoreStatistics.getHard();
                    sampleSizeHard += operatorScoreStatistics.getSampleSizeHard();

                    securityService += operatorScoreStatistics.getSecurityService();
                    sampleSizeSecurityService += operatorScoreStatistics.getSampleSizeSecurityService();

                    universal += operatorScoreStatistics.getUniversal();
                    sampleSizeUniversal += operatorScoreStatistics.getSampleSizeUniversal();

                    counter += operatorScoreStatistics.getCounter();
                    sampleSizeCounter += operatorScoreStatistics.getSampleSizeCounter();

                    building += operatorScoreStatistics.getBuilding();
                    sampleSizeBuilding += operatorScoreStatistics.getSampleSizeBuilding();

                    comprehensive += operatorScoreStatistics.getComprehensive();
                    sampleSizeComprehensive += operatorScoreStatistics.getSampleSizeComprehensive();
                }

                //存入dto对象进行暂存
                OperatorScoreStatistics build = OperatorScoreStatistics.builder()
                        .charId(charId)
                        .rarity(rarity)
                        .daily(daily)
                        .sampleSizeDaily(sampleSizeDaily)
                        .rogue(rogue)
                        .sampleSizeRogue(sampleSizeRogue)
                        .hard(hard)
                        .sampleSizeHard(sampleSizeHard)
                        .securityService(securityService)
                        .sampleSizeSecurityService(sampleSizeSecurityService)
                        .universal(universal)
                        .sampleSizeUniversal(sampleSizeUniversal)
                        .counter(counter)
                        .sampleSizeCounter(sampleSizeCounter)
                        .building(building)
                        .sampleSizeBuilding(sampleSizeBuilding)
                        .comprehensive(comprehensive)
                        .sampleSizeComprehensive(sampleSizeComprehensive)
                        .build();

                scratchResult.put(charId, build);

            });
        }

        scratchResult.forEach((charId, v) -> {
            statisticsResultList.add(v);
        });

        operatorScoreMapper.insertBatchScoreStatistics(statisticsResultList);
    }

    public List<OperatorScoreStatisticsVO> getScoreStatisticsResult() {
        List<OperatorScoreStatistics> operatorScoreStatistics = operatorScoreMapper.selectScoreStatisticsList();

        List<OperatorScoreStatisticsVO> statisticsResult = new ArrayList<>();

        operatorScoreStatistics.forEach(e -> {
            OperatorScoreStatisticsVO build = OperatorScoreStatisticsVO.builder()
                    .charId(e.getCharId())
                    .rarity(e.getRarity())
                    .daily(limitDecimalPoint(e.getDaily(), e.getSampleSizeDaily()))
                    .sampleSizeDaily(e.getSampleSizeDaily())
                    .rogue(limitDecimalPoint(e.getRogue(), e.getSampleSizeRogue()))
                    .sampleSizeRogue(e.getSampleSizeRogue())
                    .securityService(limitDecimalPoint(e.getSecurityService(), e.getSampleSizeSecurityService()))
                    .sampleSizeSecurityService(e.getSampleSizeSecurityService())
                    .hard(limitDecimalPoint(e.getHard(), e.getSampleSizeHard()))
                    .sampleSizeHard(e.getSampleSizeHard())
                    .universal(limitDecimalPoint(e.getUniversal(), e.getSampleSizeUniversal()))
                    .sampleSizeUniversal(e.getSampleSizeUniversal())
                    .counter(limitDecimalPoint(e.getCounter(), e.getSampleSizeCounter()))
                    .sampleSizeCounter(e.getSampleSizeCounter())
                    .building(limitDecimalPoint(e.getBuilding(), e.getSampleSizeBuilding()))
                    .sampleSizeBuilding(e.getSampleSizeBuilding())
                    .comprehensive(limitDecimalPoint(e.getComprehensive(), e.getSampleSizeComprehensive()))
                    .sampleSizeComprehensive(e.getSampleSizeComprehensive())
                    .build();
            statisticsResult.add(build);
        });

        return statisticsResult;
    }


    private Double limitDecimalPoint(Integer a, Integer b) {
        DecimalFormat df = new DecimalFormat("#.00");
        String format = df.format((double) a / (double) b);
        return Double.valueOf(format);
    }


}
