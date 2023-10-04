package com.lhs.service.survey;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.entity.ResultCode;
import com.lhs.entity.po.survey.SurveyScore;
import com.lhs.entity.po.survey.SurveyStatisticsScore;
import com.lhs.entity.po.survey.SurveyUser;
import com.lhs.mapper.survey.SurveyScoreMapper;
import com.lhs.entity.vo.survey.SurveyScoreVO;
import com.lhs.entity.vo.survey.SurveyStatisticsScoreVO;

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
public class SurveyScoreService {


    private SurveyScoreMapper surveyScoreMapper;
    private SurveyUserService surveyUserService;
    private final RedisTemplate<String,Object> redisTemplate;

    public SurveyScoreService(SurveyScoreMapper surveyScoreMapper, SurveyUserService surveyUserService, RedisTemplate<String, Object> redisTemplate) {
        this.surveyScoreMapper = surveyScoreMapper;
        this.surveyUserService = surveyUserService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 上传干员风评表
     *
     * @param token           token
     * @param surveyScoreList 干员风评表
     * @return 成功消息
     */
//    @TakeCount(name = "上传评分")
    public HashMap<Object, Object> uploadScoreForm(String token, List<SurveyScore> surveyScoreList) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        long uid = surveyUser.getId();
        String tableName = "survey_score_" + surveyUserService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表

        int affectedRows = 0;

        //用户之前上传的数据
        List<SurveyScore> surveyScores
                = surveyScoreMapper.selectSurveyScoreByUid(tableName, surveyUser.getId());

        //用户之前上传的数据转为map方便对比
        Map<String, SurveyScore> oldDataCollectById = surveyScores.stream()
                .collect(Collectors.toMap(SurveyScore::getCharId, Function.identity()));

        //新增数据
        List<SurveyScore> insertList = new ArrayList<>();

        for (SurveyScore surveyScore : surveyScoreList) {

            //和老数据进行对比
            SurveyScore savedData = oldDataCollectById.get(surveyScore.getCharId());
            //为空则新增
            if (savedData == null) {
                Long scoreId = redisTemplate.opsForValue().increment("CharacterId");
                surveyScore.setId(scoreId);
                surveyScore.setUid(uid);
                insertList.add(surveyScore);  //加入批量插入集合
                affectedRows++;  //新增数据条数
            } else {
                //如果数据存在,进行更新
                    affectedRows++;  //更新数据条数
                    surveyScore.setId(savedData.getId());
                    surveyScore.setUid(uid);
                    surveyScoreMapper.updateSurveyScoreById(tableName, surveyScore); //更新数据
            }
        }

        if (insertList.size() > 0) surveyScoreMapper.insertBatchSurveyScore(tableName, insertList);  //批量插入
        Date date = new Date();
        surveyUser.setUpdateTime(date);   //更新用户最后一次上传时间
        surveyUserService.updateSurveyUser(surveyUser);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("affectedRows", affectedRows);
        hashMap.put("updateTime", simpleDateFormat.format(date));
        return hashMap;

    }

    /**
     * 判断这个干员风评是否有变更
     *
     * @param newData 新数据
     * @param oldData 旧数据
     * @return 成功消息
     */
    private boolean comparativeData(SurveyScore newData, SurveyScore oldData) {
        //校验参数
        boolean isInvalid = (newData.getDaily() < 1 || newData.getDaily() > 5) ||
                (newData.getRogue() < 1 || newData.getRogue() > 5) ||
                (newData.getHard() < 1 || newData.getHard() > 5) ||
                (newData.getSecurityService() < 1 || newData.getSecurityService() > 5) ||
                (newData.getUniversal() < 1 || newData.getUniversal() > 5) ||
                (newData.getCounter() < 1 || newData.getCounter() > 5);

        if (isInvalid) throw new ServiceException(ResultCode.PARAM_INVALID);

        return !Objects.equals(newData.getDaily(), oldData.getDaily()) ||
                !Objects.equals(newData.getRogue(), oldData.getRogue()) ||
                !Objects.equals(newData.getHard(), oldData.getHard()) ||
                !Objects.equals(newData.getUniversal(), oldData.getUniversal()) ||
                !Objects.equals(newData.getCounter(), oldData.getCounter());
    }


    public void scoreStatistics() {
        List<Long> userIds = surveyUserService.selectSurveyUserIds();

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
        surveyScoreMapper.truncateScoreStatisticsTable();  //清空统计表

        HashMap<String, SurveyStatisticsScore> scratchResult = new HashMap<>();
        List<SurveyStatisticsScore> statisticsResultList = new ArrayList<>();


        for (List<Long> longs : userIdsGroup) {
            List<SurveyScoreVO> surveyScoreVOList_DB =
                    surveyScoreMapper.selectSurveyScoreVoByUidList("survey_score_1", longs);

            log.info("本次统计数量：" + surveyScoreVOList_DB.size());

            Map<String, List<SurveyScoreVO>> collectByCharId = surveyScoreVOList_DB.stream()
                    .collect(Collectors.groupingBy(SurveyScoreVO::getCharId));
            collectByCharId.forEach((charId, arr) -> {

                int rarity = arr.get(0).getRarity();

                List<Integer> collectByDaily = arr.stream()
                        .map(SurveyScoreVO::getDaily).filter(e -> e > 0).collect(Collectors.toList());

                List<Integer> collectByRogue = arr.stream()
                        .map(SurveyScoreVO::getRogue).filter(e -> e > 0).collect(Collectors.toList());

                List<Integer> collectByHard = arr.stream()
                        .map(SurveyScoreVO::getHard).filter(e -> e > 0).collect(Collectors.toList());

                List<Integer> collectBySecurityService = arr.stream()
                        .map(SurveyScoreVO::getSecurityService).filter(e -> e > 0).collect(Collectors.toList());

                List<Integer> collectByUniversal = arr.stream()
                        .map(SurveyScoreVO::getUniversal).filter(e -> e > 0).collect(Collectors.toList());

                List<Integer> collectByCountermeasures = arr.stream()
                        .map(SurveyScoreVO::getCounter).filter(e -> e > 0).collect(Collectors.toList());


                int sampleSizeDaily = collectByDaily.size();
                int sampleSizeRogue = collectByRogue.size();
                int sampleSizeSecurityService = collectBySecurityService.size();
                int sampleSizeHard = collectByHard.size();
                int sampleSizeUniversal = collectByUniversal.size();
                int sampleSizeCountermeasures = collectByCountermeasures.size();

                int daily = collectByDaily.stream().mapToInt(i -> i).sum();
                int rogue = collectByRogue.stream().mapToInt(i -> i).sum();
                int hard = collectByHard.stream().mapToInt(i -> i).sum();
                int security = collectBySecurityService.stream().mapToInt(i -> i).sum();
                int universal = collectByUniversal.stream().mapToInt(i -> i).sum();
                int countermeasures = collectByCountermeasures.stream().mapToInt(i -> i).sum();


                //和上次计算结果合并
                if (scratchResult.get(charId) != null) {
                    SurveyStatisticsScore surveyStatisticsScore = scratchResult.get(charId);
                    daily += surveyStatisticsScore.getDaily();
                    sampleSizeDaily += surveyStatisticsScore.getSampleSizeDaily();
                    rogue += surveyStatisticsScore.getRogue();
                    sampleSizeRogue += surveyStatisticsScore.getSampleSizeRogue();
                    hard += surveyStatisticsScore.getHard();
                    sampleSizeHard += surveyStatisticsScore.getSampleSizeHard();
                    security += surveyStatisticsScore.getSecurityService();
                    sampleSizeSecurityService += surveyStatisticsScore.getSampleSizeSecurityService();
                    universal += surveyStatisticsScore.getUniversal();
                    sampleSizeUniversal += surveyStatisticsScore.getSampleSizeUniversal();
                    countermeasures += surveyStatisticsScore.getCounter();
                    sampleSizeCountermeasures += surveyStatisticsScore.getCounter();
                }

                //存入dto对象进行暂存
                SurveyStatisticsScore build = SurveyStatisticsScore.builder()
                        .charId(charId)
                        .rarity(rarity)
                        .daily(daily)
                        .sampleSizeDaily(sampleSizeDaily)
                        .rogue(rogue)
                        .sampleSizeRogue(sampleSizeRogue)
                        .hard(hard)
                        .sampleSizeHard(sampleSizeHard)
                        .securityService(security)
                        .sampleSizeSecurityService(sampleSizeSecurityService)
                        .universal(universal)
                        .sampleSizeUniversal(sampleSizeUniversal)
                        .counter(countermeasures)
                        .sampleSizeCounter(sampleSizeCountermeasures)
                        .build();

                scratchResult.put(charId, build);

            });
        }

        scratchResult.forEach((charId, v) -> {
            statisticsResultList.add(v);
        });

        surveyScoreMapper.insertBatchScoreStatistics(statisticsResultList);
    }


    public void scoreStatisticsNew() {
        List<Long> userIds = surveyUserService.selectSurveyUserIds();

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
        surveyScoreMapper.truncateScoreStatisticsTable();  //清空统计表

        HashMap<String, SurveyStatisticsScore> scratchResult = new HashMap<>();
        List<SurveyStatisticsScore> statisticsResultList = new ArrayList<>();


        for (List<Long> longs : userIdsGroup) {
            List<SurveyScoreVO> surveyScoreVOList_DB =
                    surveyScoreMapper.selectSurveyScoreVoByUidList("survey_score_1", longs);

            log.info("本次统计数量：" + surveyScoreVOList_DB.size());

            Map<String, List<SurveyScoreVO>> collectByCharId = surveyScoreVOList_DB.stream()
                    .collect(Collectors.groupingBy(SurveyScoreVO::getCharId));

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

                for (SurveyScoreVO surveyScoreVo : arr) {
                    if (surveyScoreVo.getDaily() > 0) {
                        sampleSizeDaily++;
                        daily+=surveyScoreVo.getDaily();
                    }
                    if (surveyScoreVo.getRogue() > 0) {
                        sampleSizeRogue++;
                        rogue+=surveyScoreVo.getRogue();
                    }
                    if (surveyScoreVo.getSecurityService() > 0) {
                        sampleSizeSecurityService++;
                        securityService+=surveyScoreVo.getSecurityService();
                    }
                    if (surveyScoreVo.getHard() > 0) {
                        sampleSizeHard++;
                        hard+=surveyScoreVo.getHard();
                    }
                    if (surveyScoreVo.getUniversal() > 0) {
                        sampleSizeUniversal++;
                        universal+=surveyScoreVo.getUniversal();
                    }
                    if (surveyScoreVo.getCounter() > 0) {
                        sampleSizeCounter++;
                        counter+=surveyScoreVo.getCounter();
                    }
                    if (surveyScoreVo.getBuilding() > 0) {
                        sampleSizeBuilding++;
                        building+=surveyScoreVo.getBuilding();
                    }
                    if (surveyScoreVo.getComprehensive() > 0) {
                        sampleSizeComprehensive++;
                        comprehensive+=surveyScoreVo.getComprehensive();
                    }

                }


                //和上次计算结果合并
                if (scratchResult.get(charId) != null) {
                    SurveyStatisticsScore surveyStatisticsScore = scratchResult.get(charId);
                    daily += surveyStatisticsScore.getDaily();
                    sampleSizeDaily += surveyStatisticsScore.getSampleSizeDaily();

                    rogue += surveyStatisticsScore.getRogue();
                    sampleSizeRogue += surveyStatisticsScore.getSampleSizeRogue();

                    hard += surveyStatisticsScore.getHard();
                    sampleSizeHard += surveyStatisticsScore.getSampleSizeHard();

                    securityService += surveyStatisticsScore.getSecurityService();
                    sampleSizeSecurityService += surveyStatisticsScore.getSampleSizeSecurityService();

                    universal += surveyStatisticsScore.getUniversal();
                    sampleSizeUniversal += surveyStatisticsScore.getSampleSizeUniversal();

                    counter += surveyStatisticsScore.getCounter();
                    sampleSizeCounter += surveyStatisticsScore.getSampleSizeCounter();

                    building += surveyStatisticsScore.getBuilding();
                    sampleSizeBuilding += surveyStatisticsScore.getSampleSizeBuilding();

                    comprehensive += surveyStatisticsScore.getComprehensive();
                    sampleSizeComprehensive += surveyStatisticsScore.getSampleSizeComprehensive();
                }

                //存入dto对象进行暂存
                SurveyStatisticsScore build = SurveyStatisticsScore.builder()
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

        surveyScoreMapper.insertBatchScoreStatistics(statisticsResultList);
    }

    public List<SurveyStatisticsScoreVO> getScoreStatisticsResult() {
        List<SurveyStatisticsScore> surveyStatisticsScores = surveyScoreMapper.selectScoreStatisticsList();

        List<SurveyStatisticsScoreVO> statisticsResult = new ArrayList<>();

        surveyStatisticsScores.forEach(e -> {
            SurveyStatisticsScoreVO build = SurveyStatisticsScoreVO.builder()
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
