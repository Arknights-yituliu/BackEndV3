package com.lhs.service.survey;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.entity.po.survey.AkPlayerBindInfo;
import com.lhs.entity.po.survey.OperatorDataVo;
import com.lhs.entity.po.survey.OperatorStatistics;
import com.lhs.mapper.survey.AkPlayerBindInfoMapper;
import com.lhs.mapper.survey.OperatorDataVoMapper;
import com.lhs.mapper.survey.OperatorSurveyStatisticsMapper;
import com.lhs.service.util.ArknightsGameDataService;
import com.lhs.service.util.OSSService;
import com.lhs.entity.vo.survey.OperatorStatisticsResultVO;
import com.lhs.entity.dto.survey.OperatorStatisticsDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OperatorStatisticsService {


    private final OperatorSurveyStatisticsMapper operatorSurveyStatisticsMapper;

    private final AkPlayerBindInfoMapper akPlayerBindInfoMapper;

    private final OperatorDataVoMapper operatorDataVoMapper;

    private final ArknightsGameDataService arknightsGameDataService;

    private final RedisTemplate<String,Object> redisTemplate;

    private final OSSService ossService;


    public OperatorStatisticsService(OperatorSurveyStatisticsMapper operatorSurveyStatisticsMapper, AkPlayerBindInfoMapper akPlayerBindInfoMapper, OperatorDataVoMapper operatorDataVoMapper, ArknightsGameDataService arknightsGameDataService, RedisTemplate<String, Object> redisTemplate, OSSService ossService) {
        this.operatorSurveyStatisticsMapper = operatorSurveyStatisticsMapper;
        this.akPlayerBindInfoMapper = akPlayerBindInfoMapper;
        this.operatorDataVoMapper = operatorDataVoMapper;
        this.arknightsGameDataService = arknightsGameDataService;
        this.redisTemplate = redisTemplate;
        this.ossService = ossService;
    }



    /**
     * 干员练度调查表统计
     */
//    @Scheduled(cron = "0 10 0/2 * * ?")
    public void operatorStatistics() {
        QueryWrapper<AkPlayerBindInfo> playerBindInfoQueryWrapper = new QueryWrapper<>();
        playerBindInfoQueryWrapper.ge("last_time",new Date(System.currentTimeMillis()-60*60*24*1000*30L));
        System.out.println(System.currentTimeMillis()-60*60*24*1000*30L);
        List<Long> userIds = akPlayerBindInfoMapper.selectList(null)
                .stream()
                .map(AkPlayerBindInfo::getId)
                .toList();

        String updateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        redisTemplate.opsForHash().put("Survey", "UpdateTime.Operator", updateTime);
        redisTemplate.opsForHash().put("Survey", "UserCount.Operator", userIds.size());

        // 计算用户id按500个用户一组可以分成多少组
        operatorSurveyStatisticsMapper.truncate(); //清空统计表
        HashMap<String, OperatorStatisticsDTO> tmpResult = new HashMap<>();  //结果暂存对象

        List<Long> tmpIds = new ArrayList<>();
        for(Long id : userIds){
            tmpIds.add(id);
            if(tmpIds.size()>300){
                operatorStatistics(tmpIds,tmpResult);
                tmpIds.clear();
            }
        }

        if(!tmpIds.isEmpty()){
            operatorStatistics(tmpIds,tmpResult);
        }

        List<OperatorStatistics> statisticsOperatorList = new ArrayList<>();  //最终结果


        //将dto对象转为数据库对象
        tmpResult.forEach((k, v) -> {

            OperatorStatistics build = OperatorStatistics.builder()
                    .charId(v.getCharId())
                    .rarity(v.getRarity())
                    .own(v.getOwn())
                    .elite(JsonMapper.toJSONString(v.getElite()))
                    .skill1(JsonMapper.toJSONString(v.getSkill1()))
                    .skill2(JsonMapper.toJSONString(v.getSkill2()))
                    .skill3(JsonMapper.toJSONString(v.getSkill3()))
                    .modX(JsonMapper.toJSONString(v.getModX()))
                    .modY(JsonMapper.toJSONString(v.getModY()))
                    .modD(JsonMapper.toJSONString(v.getModD()))
                    .potential(JsonMapper.toJSONString(v.getPotential()))
                    .build();
            statisticsOperatorList.add(build);
        });

        operatorSurveyStatisticsMapper.insertBatch(statisticsOperatorList);

    }

    private void operatorStatistics(List<Long> ids,HashMap<String, OperatorStatisticsDTO> tmpResult){

        QueryWrapper<OperatorDataVo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("uid",ids);
        List<OperatorDataVo> operatorDataVoByBindUser = operatorDataVoMapper.selectList(queryWrapper);

        Logger.info("本次统计数量：" + operatorDataVoByBindUser.size());

        //根据干员id分组
        Map<String, List<OperatorDataVo>> collectByCharId = operatorDataVoByBindUser.stream()
                .collect(Collectors.groupingBy(OperatorDataVo::getCharId));

        //计算结果
        collectByCharId.forEach((charId, list) -> {
            list = list.stream()
                    .filter(OperatorDataVo::getOwn)
                    .collect(Collectors.toList());

            int own = list.size();  //持有人数
            int rarity = list.get(0).getRarity();  //星级

            Map<Integer, Long> collectByElite = new HashMap<>();
            Map<Integer, Long> collectByPotential = new HashMap<>();
            Map<Integer, Long> collectBySkill1 = new HashMap<>();
            Map<Integer, Long> collectBySkill2 = new HashMap<>();
            Map<Integer, Long> collectBySkill3 = new HashMap<>();
            Map<Integer, Long> collectByModX = new HashMap<>();
            Map<Integer, Long> collectByModY = new HashMap<>();
            Map<Integer, Long> collectByModD = new HashMap<>();

            for (OperatorDataVo operatorDataVo :list){
                collectByElite.merge(operatorDataVo.getElite(),1L,Long::sum);
                collectByPotential.merge(operatorDataVo.getPotential(),1L,Long::sum);
                collectBySkill1.merge(operatorDataVo.getSkill1(),1L,Long::sum);
                collectBySkill2.merge(operatorDataVo.getSkill2(),1L,Long::sum);
                collectBySkill3.merge(operatorDataVo.getSkill3(),1L,Long::sum);
                collectByModX.merge(operatorDataVo.getModX(),1L,Long::sum);
                collectByModY.merge(operatorDataVo.getModY(),1L,Long::sum);
                collectByModD.merge(operatorDataVo.getModD(),1L,Long::sum);
            }


            //和上一组用户id的数据合并
            if (tmpResult.get(charId) != null) {
                OperatorStatisticsDTO lastData = tmpResult.get(charId);
                own += lastData.getOwn();
                mergeLastData(lastData.getElite(),collectByElite);
                mergeLastData(lastData.getPotential(),collectByPotential);
                mergeLastData(lastData.getSkill1(),collectBySkill1);
                mergeLastData(lastData.getSkill2(),collectBySkill2);
                mergeLastData(lastData.getSkill3(),collectBySkill3);
                mergeLastData(lastData.getModX(),collectByModX);
                mergeLastData(lastData.getModY(),collectByModY);
                mergeLastData(lastData.getModD(),collectByModD);
            }

            //存入dto对象进行暂存
            OperatorStatisticsDTO build = OperatorStatisticsDTO.builder()
                    .charId(charId)
                    .own(own)
                    .elite(collectByElite)
                    .rarity(rarity)
                    .skill1(collectBySkill1)
                    .skill2(collectBySkill2)
                    .skill3(collectBySkill3)
                    .potential(collectByPotential)
                    .modX(collectByModX)
                    .modY(collectByModY)
                    .modD(collectByModD)
                    .build();
            tmpResult.put(charId, build);
        });
    }

    private  void mergeLastData(Map<Integer,Long> resource,Map<Integer,Long> target){
        for (Integer key:resource.keySet()){
             target.merge(key,resource.get(key),Long::sum);
        }
    }

    /**
     * 干员信息统计
     *
     * @return 成功消息
     */
    public HashMap<Object, Object> getCharStatisticsResult() {
        List<OperatorStatistics> statisticsOperatorList = operatorSurveyStatisticsMapper.selectList(null);

        HashMap<Object, Object> hashMap = new HashMap<>();

        Object survey = redisTemplate.opsForHash().get("Survey", "UserCount.Operator");
        String updateTime = String.valueOf(redisTemplate.opsForHash().get("Survey", "UpdateTime.Operator"));

        double userCount = Double.parseDouble(survey + ".0");
        List<OperatorStatisticsResultVO> operatorStatisticsResultVOList = new ArrayList<>();
        statisticsOperatorList.forEach(item -> {
            OperatorStatisticsResultVO build = OperatorStatisticsResultVO.builder()
                    .charId(item.getCharId())
                    .rarity(item.getRarity())
                    .own((double) item.getOwn() / userCount)
                    .elite(splitCalculation(item.getElite(), item.getOwn()))
                    .skill1(splitCalculation(item.getSkill1(), item.getOwn()))
                    .skill2(splitCalculation(item.getSkill2(), item.getOwn()))
                    .skill3(splitCalculation(item.getSkill3(), item.getOwn()))
                    .modX(splitCalculation(item.getModX(), item.getOwn()))
                    .modY(splitCalculation(item.getModY(),item.getOwn()))
                    .modD(splitCalculation(item.getModD(),item.getOwn()))
                    .build();

            operatorStatisticsResultVOList.add(build);
        });


        hashMap.put("userCount", userCount);
        hashMap.put("result", operatorStatisticsResultVOList);
        hashMap.put("updateTime", updateTime);

        return hashMap;
    }

    /**
     * 计算具体结果
     *
     * @param result     旧结果
     * @param sampleSize 样本
     * @return 计算结果
     */
    public HashMap<String, Double> splitCalculation(String result, Integer sampleSize) {
        HashMap<String, Double> hashMap = new HashMap<>();
        JsonNode jsonNode = JsonMapper.parseJSONObject(result);
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

        hashMap.put("rank1",0.0);
        hashMap.put("rank2",0.0);
        hashMap.put("rank3",0.0);
        double count = 0.0;
        while (fields.hasNext()) {
            String key = fields.next().getKey();
            int sum = jsonNode.get(key).intValue();
            if(Integer.parseInt(key)>0) count += ((double) sum / sampleSize);
            hashMap.put("rank" + key, (double) sum / sampleSize);
        }
        hashMap.put("count",count);

        return hashMap;
    }




    @Scheduled(cron = "0 0 0/1 * * ? ")
    public void saveOperatorStatisticsData(){
        List<OperatorStatistics> operatorStatistics = operatorSurveyStatisticsMapper.selectList(null);
        String data = JsonMapper.toJSONString(operatorStatistics);
        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHH = new SimpleDateFormat("yyyy-MM-dd HH").format(new Date()); // 设置日期格式
        ossService.upload(data, "backup/survey/operator/statistics" + yyyyMMdd + "/operator " + yyyyMMddHH + ".json");
    }




}
