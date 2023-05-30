package com.lhs.service;

import com.lhs.entity.survey.SurveyEvaluation;
import com.lhs.entity.survey.SurveyStatisticsEvaluation;
import com.lhs.entity.survey.SurveyUser;
import com.lhs.mapper.SurveyEvaluationMapper;
import com.lhs.mapper.SurveyUserMapper;
import com.lhs.service.vo.SurveyEvaluationVo;
import com.lhs.service.vo.SurveyStatisticsEvaluationVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SurveyEvaluationService {

    @Resource
    private SurveyEvaluationMapper surveyEvaluationMapper;

    @Resource
    private SurveyUserService surveyUserService;

    @Resource
    private SurveyUserMapper surveyUserMapper;


    /**
     * 上传干员风评表
     * @param userName  用户id
     * @param surveyEvaluationList  干员风评表
     * @return  成功消息
     */
    public HashMap<Object, Object> uploadEvaluationForm(String userName, List<SurveyEvaluation> surveyEvaluationList){

        SurveyUser surveyUser = surveyUserService.selectSurveyUser(userName);

        String tableName = "survey_evaluation_"+surveyUserService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表


        Long uid = surveyUser.getId();  //用户的uid
        int insertRows = 0;
        int updateRows = 0;

        //用户之前上传的数据
        List<SurveyEvaluation> surveyEvaluations
                = surveyEvaluationMapper.selectSurveyEvaluationByUid(tableName, surveyUser.getId());

        //用户之前上传的数据转为map方便对比
        Map<String, SurveyEvaluation> oldDataCollectById = surveyEvaluations.stream()
                .collect(Collectors.toMap(SurveyEvaluation::getCharId, Function.identity()));

        List<SurveyEvaluation> insertList = new ArrayList<>();//新增数据批量插入集合

        for (SurveyEvaluation surveyEvaluation : surveyEvaluationList) {
            String charId = surveyEvaluation.getCharId().substring(surveyEvaluation.getCharId().indexOf("_")+1);
            String id = uid + "_" + charId; //id

            //和老数据进行对比
            SurveyEvaluation surveyDataCharByCharId  = oldDataCollectById.get(surveyEvaluation.getCharId());
            //为空则新增
            surveyEvaluation.setId(id);
            surveyEvaluation.setUid(uid);
            if (surveyDataCharByCharId == null) {
                insertList.add(surveyEvaluation);  //加入批量插入集合
                insertRows++;  //新增数据条数
            } else {
                //如果数据存在，同时有某个信息不一致则进行更新 （考虑到可能更新量不大，没用when case批量更新
                if (surveyEvaluationEquals(surveyEvaluation, surveyDataCharByCharId)) {
                    updateRows++;  //更新数据条数
                    surveyEvaluationMapper.updateSurveyEvaluationById(tableName, surveyEvaluation); //更新数据
                    System.out.println(surveyEvaluation);
                }
            }
        }

        if (insertList.size() > 0) surveyEvaluationMapper.insertBatchSurveyEvaluation(tableName, insertList);  //批量插入
        surveyUserMapper.updateSurveyUser(surveyUser);   //更新用户表

        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("updateRows", updateRows);
        hashMap.put("insertRows", insertRows);
        return hashMap;

    }

    /**
     * 判断这个干员风评是否有变更
     * @param newData  新数据
     * @param oldData  旧数据
     * @return 成功消息
     */
    private boolean surveyEvaluationEquals(SurveyEvaluation newData, SurveyEvaluation oldData) {
        return !Objects.equals(newData.getDaily(), oldData.getDaily())
                || !Objects.equals(newData.getRogue(), oldData.getRogue())
                || !Objects.equals(newData.getHard(), oldData.getHard())
                || !Objects.equals(newData.getUniversal(), oldData.getUniversal())
                || !Objects.equals(newData.getCountermeasures(), oldData.getCountermeasures());
    }


    public void evaluationStatistics() {
        List<Long> userIds = surveyUserService.selectSurveyUserIds();

        List<List<Long>> userIdsGroup = new ArrayList<>();
        String update_time = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        surveyUserService.updateConfigByKey(String.valueOf(userIds.size()), "user_count_evaluation");
        surveyUserService.updateConfigByKey(update_time, "update_time_evaluation");

        int length = userIds.size();
        // 计算用户id按500个用户一组可以分成多少组
        int num = length / 500 + 1;
        int fromIndex = 0;   // id分组开始
        int toIndex = 500;   //id分组结束
        for (int i = 0; i < num; i++) {
            toIndex = Math.min(toIndex, userIds.size());
            userIdsGroup.add(userIds.subList(fromIndex, toIndex));
            fromIndex += 500;
            toIndex += 500;
        }
        surveyEvaluationMapper.truncateEvaluationStatisticsTable();  //清空统计表

        HashMap<String, SurveyStatisticsEvaluation> scratchResult = new HashMap<>();
        List<SurveyStatisticsEvaluation> statisticsResultList = new ArrayList<>();



        for (List<Long> longs : userIdsGroup) {
            List<SurveyEvaluationVo> surveyEvaluationVoList_DB =
                    surveyEvaluationMapper.selectSurveyEvaluationVoByUidList("survey_evaluation_1", longs);
            Map<String, List<SurveyEvaluationVo>> collectByCharId = surveyEvaluationVoList_DB.stream()
                    .collect(Collectors.groupingBy(SurveyEvaluationVo::getCharId));
            collectByCharId.forEach((charId,arr)->{

                int rarity = arr.get(0).getRarity();

                List<Integer> collectByDaily = arr.stream()
                        .map(SurveyEvaluationVo::getDaily).filter(e -> e > -1).collect(Collectors.toList());

                List<Integer> collectByRogue = arr.stream()
                        .map(SurveyEvaluationVo::getRogue).filter(e -> e > -1).collect(Collectors.toList());

                List<Integer> collectByHard = arr.stream()
                        .map(SurveyEvaluationVo::getHard).filter(e -> e > -1).collect(Collectors.toList());

                List<Integer> collectBySecurityService = arr.stream()
                        .map(SurveyEvaluationVo::getSecurityService).filter(e -> e > -1).collect(Collectors.toList());

                List<Integer> collectByUniversal = arr.stream()
                        .map(SurveyEvaluationVo::getUniversal).filter(e -> e > -1).collect(Collectors.toList());

                List<Integer> collectByCountermeasures = arr.stream()
                        .map(SurveyEvaluationVo::getCountermeasures).filter(e -> e > -1).collect(Collectors.toList());

                int sampleSizeDaily = collectByDaily.size();
                int sampleSizeRogue = collectByRogue.size();
                int sampleSizeSecurityService = collectBySecurityService.size();
                int sampleSizeHard  = collectByHard.size();
                int sampleSizeUniversal = collectByUniversal.size();
                int sampleSizeCountermeasures = collectByCountermeasures.size();

                int daily = collectByDaily.stream().mapToInt(i->i).sum();
                int rogue = collectByRogue.stream().mapToInt(i->i).sum();
                int hard = collectByHard.stream().mapToInt(i->i).sum();
                int security = collectBySecurityService.stream().mapToInt(i->i).sum();
                int universal =  collectByUniversal.stream().mapToInt(i->i).sum();
                int countermeasures =  collectByCountermeasures.stream().mapToInt(i->i).sum();


                //和上次计算结果合并
                if(scratchResult.get(charId)!=null){
                    SurveyStatisticsEvaluation surveyStatisticsEvaluation = scratchResult.get(charId);
                    daily += surveyStatisticsEvaluation.getDaily();
                    sampleSizeDaily += surveyStatisticsEvaluation.getSampleSizeDaily();
                    rogue += surveyStatisticsEvaluation.getRogue();
                    sampleSizeRogue += surveyStatisticsEvaluation.getSampleSizeRogue();
                    hard += surveyStatisticsEvaluation.getHard();
                    sampleSizeHard += surveyStatisticsEvaluation.getSampleSizeHard();
                    security += surveyStatisticsEvaluation.getSecurityService();
                    sampleSizeSecurityService += surveyStatisticsEvaluation.getSampleSizeSecurityService();
                    universal += surveyStatisticsEvaluation.getUniversal();
                    sampleSizeUniversal += surveyStatisticsEvaluation.getSampleSizeUniversal();
                    countermeasures += surveyStatisticsEvaluation.getCountermeasures();
                    sampleSizeCountermeasures += surveyStatisticsEvaluation.getSampleSizeCountermeasures();
                }

                //存入dto对象进行暂存
                SurveyStatisticsEvaluation build = SurveyStatisticsEvaluation.builder()
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

                scratchResult.put(charId ,build);

            });
        }

        scratchResult.forEach((charId,v)->{
            statisticsResultList.add(v);
        });

        surveyEvaluationMapper.insertBatchEvaluationStatistics(statisticsResultList);
    }


    public List<SurveyStatisticsEvaluationVo> getEvaluationStatisticsResult(){
        List<SurveyStatisticsEvaluation> surveyStatisticsEvaluations = surveyEvaluationMapper.selectEvaluationStatisticsList();

        List<SurveyStatisticsEvaluationVo> statisticsResult = new ArrayList<>();

        surveyStatisticsEvaluations.forEach(e->{
            SurveyStatisticsEvaluationVo build = SurveyStatisticsEvaluationVo.builder()
                    .charId(e.getCharId())
                    .rarity(e.getRarity())
                    .daily(limitDecimalPoint(e.getDaily(),e.getSampleSizeDaily()))
                    .sampleSizeDaily(e.getSampleSizeDaily())
                    .rogue(limitDecimalPoint(e.getRogue(),e.getSampleSizeRogue()))
                    .sampleSizeRogue(e.getSampleSizeRogue())
                    .securityService(limitDecimalPoint(e.getSecurityService(),e.getSampleSizeSecurityService()))
                    .sampleSizeSecurityService(e.getSampleSizeSecurityService())
                    .hard(limitDecimalPoint(e.getHard(),e.getSampleSizeHard()))
                    .sampleSizeHard(e.getSampleSizeHard())
                    .universal(limitDecimalPoint(e.getUniversal(),e.getSampleSizeUniversal()))
                    .sampleSizeUniversal(e.getSampleSizeUniversal())
                    .countermeasures(limitDecimalPoint(e.getCountermeasures(),e.getSampleSizeCountermeasures()))
                    .sampleSizeCountermeasures(e.getSampleSizeCountermeasures())
                    .build();
            statisticsResult.add(build);
        });

        return statisticsResult;
    }


    private Double limitDecimalPoint(Integer a,Integer b){
        java.text.DecimalFormat   df   =new   java.text.DecimalFormat("#.000");
        String format = df.format((double) a / (double) b);
        return Double.valueOf(format);
    }



}
