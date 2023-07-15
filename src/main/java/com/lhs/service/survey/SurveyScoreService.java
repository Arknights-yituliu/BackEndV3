package com.lhs.service.survey;

import com.lhs.common.annotation.TakeCount;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.survey.CharacterTable;
import com.lhs.entity.survey.SurveyScore;
import com.lhs.entity.survey.SurveyStatisticsScore;
import com.lhs.entity.survey.SurveyUser;
import com.lhs.mapper.CharacterTableMapper;
import com.lhs.mapper.SurveyScoreMapper;
import com.lhs.mapper.SurveyUserMapper;
import com.lhs.vo.survey.SurveyScoreVo;
import com.lhs.vo.survey.SurveyStatisticsScoreVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SurveyScoreService {

    @Resource
    private SurveyScoreMapper surveyScoreMapper;
    @Resource
    private SurveyService surveyService;
    @Resource
    private SurveyUserMapper surveyUserMapper;
    @Resource
    private CharacterTableMapper characterTableMapper;

    /**
     * 上传干员风评表
     *
     * @param token token
     * @param surveyScoreList 干员风评表
     * @return 成功消息
     */
    @TakeCount(name = "上传评分")
    public HashMap<Object, Object> uploadScoreForm(String token, List<SurveyScore> surveyScoreList) {

        SurveyUser surveyUser = surveyService.getSurveyUserById(token);
        long uid = surveyUser.getId();
        String tableName = "survey_score_" + surveyService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表

        int insertRows = 0;
        int updateRows = 0;

        //用户之前上传的数据
        List<SurveyScore> surveyScores
                = surveyScoreMapper.selectSurveyScoreByUid(tableName, surveyUser.getId());

        //用户之前上传的数据转为map方便对比
        Map<String, SurveyScore> oldDataCollectById = surveyScores.stream()
                .collect(Collectors.toMap(SurveyScore::getCharId, Function.identity()));

        //新增数据
        List<SurveyScore> insertList = new ArrayList<>();

        //干员的自定义id
        List<CharacterTable> characterTables = characterTableMapper.selectList(null);
        //干员的自定义id
        Map<String, String> charIdAndId = characterTables.stream()
                .collect(Collectors.toMap(CharacterTable::getCharId, CharacterTable::getId));

        for (SurveyScore surveyScore : surveyScoreList) {
            //没有自定义id的跳出
            if(charIdAndId.get(surveyScore.getCharId())==null) continue;
            //唯一id为uid+自定义id
            Long id = Long.parseLong(uid + charIdAndId.get(surveyScore.getCharId()));

            //和老数据进行对比
            SurveyScore surveyDataCharByCharId = oldDataCollectById.get(surveyScore.getCharId());
            //为空则新增
            surveyScore.setId(id);
            surveyScore.setUid(uid);

            if (surveyDataCharByCharId == null) {
                insertList.add(surveyScore);  //加入批量插入集合
                insertRows++;  //新增数据条数
            } else {
                //如果数据存在，同时有某个信息不一致则进行更新 （考虑到可能更新量不大，没用when case批量更新
                if (comparativeData(surveyScore, surveyDataCharByCharId)) {
                    updateRows++;  //更新数据条数
                    surveyScoreMapper.updateSurveyScoreById(tableName, surveyScore); //更新数据
                }
            }
        }


        if (insertList.size() > 0) surveyScoreMapper.insertBatchSurveyScore(tableName, insertList);  //批量插入
        surveyUserMapper.updateSurveyUser(surveyUser);   //更新用户表



        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("updateRows", updateRows);
        hashMap.put("insertRows", insertRows);
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
                (newData.getSecurityService() < 1 || newData.getSecurityService() > 5)||
                (newData.getUniversal() < 1 || newData.getUniversal() > 5)||
                (newData.getCountermeasures()< 1 || newData.getCountermeasures() > 5);

        if(isInvalid) throw new ServiceException(ResultCode.PARAM_INVALID);

        return !Objects.equals(newData.getDaily(), oldData.getDaily()) ||
                !Objects.equals(newData.getRogue(), oldData.getRogue()) ||
                !Objects.equals(newData.getHard(), oldData.getHard()) ||
                !Objects.equals(newData.getUniversal(), oldData.getUniversal()) ||
                !Objects.equals(newData.getCountermeasures(), oldData.getCountermeasures());
    }



    public void scoreStatistics() {
        List<Long> userIds = surveyService.selectSurveyUserIds();

        List<List<Long>> userIdsGroup = new ArrayList<>();
        String update_time = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        surveyService.updateConfigByKey(String.valueOf(userIds.size()), "user_count_score");
        surveyService.updateConfigByKey(update_time, "update_time_score");

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
            List<SurveyScoreVo> surveyScoreVoList_DB =
                    surveyScoreMapper.selectSurveyScoreVoByUidList("survey_score_1", longs);

            log.info("本次统计数量：" + surveyScoreVoList_DB.size());

            Map<String, List<SurveyScoreVo>> collectByCharId = surveyScoreVoList_DB.stream()
                    .collect(Collectors.groupingBy(SurveyScoreVo::getCharId));
            collectByCharId.forEach((charId, arr) -> {

                int rarity = arr.get(0).getRarity();

                List<Integer> collectByDaily = arr.stream()
                        .map(SurveyScoreVo::getDaily).filter(e -> e > 0).collect(Collectors.toList());

                List<Integer> collectByRogue = arr.stream()
                        .map(SurveyScoreVo::getRogue).filter(e -> e > 0).collect(Collectors.toList());

                List<Integer> collectByHard = arr.stream()
                        .map(SurveyScoreVo::getHard).filter(e -> e > 0).collect(Collectors.toList());

                List<Integer> collectBySecurityService = arr.stream()
                        .map(SurveyScoreVo::getSecurityService).filter(e -> e > 0).collect(Collectors.toList());

                List<Integer> collectByUniversal = arr.stream()
                        .map(SurveyScoreVo::getUniversal).filter(e -> e > 0).collect(Collectors.toList());

                List<Integer> collectByCountermeasures = arr.stream()
                        .map(SurveyScoreVo::getCountermeasures).filter(e -> e > 0).collect(Collectors.toList());

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
                    countermeasures += surveyStatisticsScore.getCountermeasures();
                    sampleSizeCountermeasures += surveyStatisticsScore.getSampleSizeCountermeasures();
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
                        .countermeasures(countermeasures)
                        .sampleSizeCountermeasures(sampleSizeCountermeasures)
                        .build();

                scratchResult.put(charId, build);

            });
        }

        scratchResult.forEach((charId, v) -> {
            statisticsResultList.add(v);
        });

        surveyScoreMapper.insertBatchScoreStatistics(statisticsResultList);
    }


    public List<SurveyStatisticsScoreVo> getScoreStatisticsResult() {
        List<SurveyStatisticsScore> surveyStatisticsScores = surveyScoreMapper.selectScoreStatisticsList();

        List<SurveyStatisticsScoreVo> statisticsResult = new ArrayList<>();

        surveyStatisticsScores.forEach(e -> {
            SurveyStatisticsScoreVo build = SurveyStatisticsScoreVo.builder()
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
                    .countermeasures(limitDecimalPoint(e.getCountermeasures(), e.getSampleSizeCountermeasures()))
                    .sampleSizeCountermeasures(e.getSampleSizeCountermeasures())
                    .build();
            statisticsResult.add(build);
        });

        return statisticsResult;
    }


    private Double limitDecimalPoint(Integer a, Integer b) {
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.000");
        String format = df.format((double) a / (double) b);
        return Double.valueOf(format);
    }


}
