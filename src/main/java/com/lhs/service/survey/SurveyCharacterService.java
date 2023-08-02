package com.lhs.service.survey;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lhs.common.annotation.TakeCount;
import com.lhs.common.util.ExcelUtil;
import com.lhs.entity.survey.*;
import com.lhs.mapper.CharacterTableMapper;
import com.lhs.mapper.SurveyCharacterMapper;
import com.lhs.vo.survey.SurveyStatisticsChar;
import com.lhs.vo.survey.CharacterStatisticsResult;
import com.lhs.vo.survey.SurveyCharacterExcelVo;
import com.lhs.vo.survey.SurveyCharacterVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SurveyCharacterService {

    @Resource
    private SurveyCharacterMapper surveyCharacterMapper;
    @Resource
    private SurveyUserService surveyUserService;
    @Resource
    private CharacterTableMapper characterTableMapper;


    /**
     * 上传干员练度调查表
     * @param token token
     * @param surveyCharacterList  干员练度调查表单
     * @return  成功消息
     */
//    @TakeCount(name = "上传评分")
    public HashMap<Object, Object> uploadCharForm(String token, List<SurveyCharacter> surveyCharacterList) {
        SurveyUser surveyUser = surveyUserService.getSurveyUserById(token);
        return updateSurveyData(surveyUser, surveyCharacterList);
    }

    /**
     * 导入干员练度调查表
     * @param file Excel文件
     * @param token token
     * @return 成功消息
     */
    public HashMap<Object, Object> importSurveyCharacterForm(MultipartFile file,String token){
        Long uid = surveyUserService.decryptToken(token);
        List<SurveyCharacter> list = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), SurveyCharacterExcelVo.class, new AnalysisEventListener<SurveyCharacterExcelVo>() {
                public void invoke(SurveyCharacterExcelVo surveyCharacterExcelVo, AnalysisContext analysisContext) {
                    SurveyCharacter surveyCharacter = new SurveyCharacter();
                    BeanUtils.copyProperties(surveyCharacterExcelVo,surveyCharacter);
                    list.add(surveyCharacter);
                }
                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                }
            }).sheet().doRead();

        } catch (IOException e) {
            e.printStackTrace();
        }

        SurveyUser surveyUser = surveyUserService.getSurveyUserById(token);
        return updateSurveyData(surveyUser, list);
    }


    /**
     * 通用的上传抽象方法
     * @param surveyUser 用户信息
     * @param surveyCharacterList  干员练度调查表
     * @return
     */
    private HashMap<Object, Object> updateSurveyData(SurveyUser surveyUser, List<SurveyCharacter> surveyCharacterList){

        long uid = surveyUser.getId();

        String tableName = "survey_character_"+ surveyUserService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表

        int affectedRows = 0;


        //用户之前上传的数据
        List<SurveyCharacter> surveyCharacterVos
                = surveyCharacterMapper.selectSurveyCharacterByUid(tableName, surveyUser.getId());

        //用户之前上传的数据转为map方便对比
        Map<Long, SurveyCharacter> oldDataCollectById = surveyCharacterVos.stream()
                .collect(Collectors.toMap(SurveyCharacter::getId, Function.identity()));

        //新增数据
        List<SurveyCharacter> insertList = new ArrayList<>();

        //干员的自定义id
        List<CharacterTable> characterTables = characterTableMapper.selectList(null);
        //干员的自定义id
        Map<String, String> charIdAndId = characterTables.stream()
                .collect(Collectors.toMap(CharacterTable::getCharId, CharacterTable::getId));


        for (SurveyCharacter surveyCharacter : surveyCharacterList) {
            //没有自定义id的跳出
            if(charIdAndId.get(surveyCharacter.getCharId())==null) continue;
            //唯一id为uid+自定义id
            Long id = Long.parseLong(uid + charIdAndId.get(surveyCharacter.getCharId()));

            //精英化阶段小于2 不能专精和开模组
            if (surveyCharacter.getElite() < 2 &&surveyCharacter.getOwn()) {
                surveyCharacter.setSkill1(-1);
                surveyCharacter.setSkill2(-1);
                surveyCharacter.setSkill3(-1);
                surveyCharacter.setModX(-1);
                surveyCharacter.setModY(-1);
            }

            //和老数据进行对比
            SurveyCharacter surveyCharacterById = oldDataCollectById.get(id);
            //为空则新增
            surveyCharacter.setId(id);
            surveyCharacter.setUid(uid);
            if (surveyCharacterById == null) {
                insertList.add(surveyCharacter);  //加入批量插入集合
                affectedRows++;  //新增数据条数
            } else {
                //如果数据存在，进行更新
                    affectedRows++;  //更新数据条数
                    surveyCharacterMapper.updateSurveyCharacterById(tableName, surveyCharacter); //更新数据

            }
        }

        if (insertList.size() > 0) surveyCharacterMapper.insertBatchSurveyCharacter(tableName, insertList);  //批量插入
        Date date = new Date();
        surveyUser.setUpdateTime(date);   //更新用户最后一次上传时间
        surveyUserService.updateSurveyUser(surveyUser);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("affectedRows", affectedRows);
        hashMap.put("updateTime",simpleDateFormat.format(date));
        return hashMap;
    }

    public void exportSurveyCharacterForm(HttpServletResponse response,String token){

        SurveyUser surveyUser = surveyUserService.getSurveyUserById(token);
        String tableName = "survey_character_"+ surveyUserService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表
        //用户之前上传的数据
        List<SurveyCharacter> list
                = surveyCharacterMapper.selectSurveyCharacterByUid(tableName, surveyUser.getId());
        List<SurveyCharacterExcelVo> listVo = new ArrayList<>();
        JSONObject characterTable = surveyUserService.getCharacterTable();
        for(SurveyCharacter surveyCharacter:list){
            SurveyCharacterExcelVo surveyCharacterExcelVo = new SurveyCharacterExcelVo();
            BeanUtils.copyProperties(surveyCharacter,surveyCharacterExcelVo);
            String charInfoStr = characterTable.getString(surveyCharacter.getCharId());
            JSONObject charInfo = JSONObject.parseObject(charInfoStr);
            if(charInfo.get("name")==null){
                surveyCharacterExcelVo.setName("未知干员，待更新");
            }else {
                surveyCharacterExcelVo.setName(charInfo.getString("name"));
            }

            listVo.add(surveyCharacterExcelVo);
        }
        ExcelUtil.exportExcel(response,listVo,SurveyCharacterExcelVo.class,"characterForm");
    }

    /**
     * 判断这个干员是否有变更
     * @param newData  新数据
     * @param oldData  旧数据
     * @return 成功消息
     */
    private boolean surveyDataCharEquals(SurveyCharacter newData, SurveyCharacter oldData) {
        return !Objects.equals(newData.getElite(), oldData.getElite())
                || !Objects.equals(newData.getPotential(), oldData.getPotential())
                || !Objects.equals(newData.getSkill1(), oldData.getSkill1())
                || !Objects.equals(newData.getSkill2(), oldData.getSkill2())
                || !Objects.equals(newData.getSkill3(), oldData.getSkill3())
                || !Objects.equals(newData.getModX(), oldData.getModX())
                || !Objects.equals(newData.getModY(), oldData.getModY())
                || !Objects.equals(newData.getOwn(), oldData.getOwn());
    }

    /**
     * 干员练度调查表统计
     *
     */
    public void characterStatistics() {
        List<Long> userIds = surveyUserService.selectSurveyUserIds();

        List<List<Long>> userIdsGroup = new ArrayList<>();
        String update_time = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        surveyUserService.updateConfigByKey(String.valueOf(userIds.size()),"user_count_character");
        surveyUserService.updateConfigByKey(String.valueOf(userIds.size()),update_time);

        int length = userIds.size();
        // 计算用户id按500个用户一组可以分成多少组
        int num = length / 300 + 1;
        int fromIndex = 0;   // id分组开始
        int toIndex = 300;   //id分组结束
        for (int i = 0; i < num; i++) {
            toIndex = Math.min(toIndex, userIds.size());
            userIdsGroup.add(userIds.subList(fromIndex, toIndex));
            fromIndex += 300;
            toIndex += 300;
        }

        surveyCharacterMapper.truncateCharacterStatisticsTable();  //清空统计表

        HashMap<String, SurveyStatisticsChar> hashMap = new HashMap<>();  //结果暂存对象
        List<SurveyStatisticsCharacter> surveyDataCharList = new ArrayList<>();  //最终结果

        for (List<Long> ids : userIdsGroup) {
            if(ids.size()==0) continue;
            List<SurveyCharacterVo> surveyDataCharList_DB =
                    surveyCharacterMapper.selectSurveyCharacterVoByUidList("survey_character_1", ids);

            log.info("本次统计数量：" + surveyDataCharList_DB.size());

            //根据干员id分组
            Map<String, List<SurveyCharacterVo>> collectByCharId = surveyDataCharList_DB.stream()
                    .collect(Collectors.groupingBy(SurveyCharacterVo::getCharId));

            //计算结果
            collectByCharId.forEach((charId, list) -> {
                list = list.stream()
                        .filter(SurveyCharacterVo::getOwn)
                        .collect(Collectors.toList());

                int own = list.size();  //持有人数
                int rarity = list.get(0).getRarity();  //星级

                //根据该干员精英等级分组统计 map(精英等级,该等级的数量)
                Map<Integer, Long> collectByElite = list.stream()
                        .filter(e->e.getElite()>-1)
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getElite,Collectors.counting()));

                //根据该干员的潜能等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectByPotential =list.stream()
                        .filter(e->e.getPotential()>-1)
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getPotential,Collectors.counting()));

                //根据该干员的技能专精等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectBySkill1 = list.stream()
                        .filter(e->e.getSkill1()>-1)
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getSkill1,Collectors.counting()));

                Map<Integer, Long> collectBySkill2 = list.stream()
                        .filter(e->e.getSkill2()>-1)
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getSkill2,Collectors.counting()));

                Map<Integer, Long> collectBySkill3 = list.stream()
                        .filter(e->e.getSkill3()>-1)
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getSkill3,Collectors.counting()));

                //根据该干员的模组等级分组统计  map(潜能等级,该等级的数量)
                Map<Integer, Long> collectByModX = list.stream()
                        .filter(e->e.getModX()>-1)
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getModX,Collectors.counting()));

                Map<Integer, Long> collectByModY = list.stream()
                        .filter(e->e.getModY()>-1)
                        .collect(Collectors.groupingBy(SurveyCharacterVo::getModY,Collectors.counting()));

                //和上一组用户id的数据合并
                if (hashMap.get(charId) != null) {
                    SurveyStatisticsChar lastData = hashMap.get(charId);
                    own += lastData.getOwn();

                    lastData.getElite()
                            .forEach((k, v) -> collectByElite.merge(k, v, Long::sum));

                    lastData.getPotential()
                            .forEach((k, v) -> collectByPotential.merge(k, v, Long::sum));

                    lastData.getSkill1()
                            .forEach((k, v) -> collectBySkill1.merge(k, v, Long::sum));

                    lastData.getSkill2()
                            .forEach((k, v) -> collectBySkill2.merge(k, v, Long::sum));

                    lastData.getSkill3()
                            .forEach((k, v) -> collectBySkill3.merge(k, v, Long::sum));

                    lastData.getModX()
                            .forEach((k, v) -> collectByModX.merge(k, v, Long::sum));

                    lastData.getModY()
                            .forEach((k, v) -> collectByModY.merge(k, v, Long::sum));
                }

                //存入dto对象进行暂存
                SurveyStatisticsChar build = SurveyStatisticsChar.builder()
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
                        .build();
                hashMap.put(charId, build);
            });
        }

        //将dto对象转为数据库对象
        hashMap.forEach((k, v) -> {
            int sampleSizeElite = getSampleSize(v.getElite());
            int sampleSizeSkill1 = getSampleSize(v.getSkill1());
            int sampleSizeSkill2 = getSampleSize(v.getSkill2());
            int sampleSizeSkill3 = getSampleSize(v.getSkill3());
            int sampleSizeModX = getSampleSize(v.getModX());
            int sampleSizeModY = getSampleSize(v.getModY());
            int sampleSizePotential  = getSampleSize(v.getPotential());

            SurveyStatisticsCharacter build = SurveyStatisticsCharacter.builder()
                    .charId(v.getCharId())
                    .rarity(v.getRarity())
                    .own(v.getOwn())
                    .elite(JSON.toJSONString(v.getElite()))
                    .sampleSizeElite(sampleSizeElite)
                    .skill1(JSON.toJSONString(v.getSkill1()))
                    .sampleSizeSkill1(sampleSizeSkill1)
                    .skill2(JSON.toJSONString(v.getSkill2()))
                    .sampleSizeSkill2(sampleSizeSkill2)
                    .skill3(JSON.toJSONString(v.getSkill3()))
                    .sampleSizeSkill3(sampleSizeSkill3)
                    .modX(JSON.toJSONString(v.getModX()))
                    .sampleSizeModX(sampleSizeModX)
                    .modY(JSON.toJSONString(v.getModY()))
                    .sampleSizeModY(sampleSizeModY)
                    .potential(JSON.toJSONString(v.getPotential()))
                    .sampleSizePotential(sampleSizePotential)
                    .build();
            surveyDataCharList.add(build);
        });

        surveyCharacterMapper.insertBatchCharacterStatistics(surveyDataCharList);
    }

    private static Integer getSampleSize(Map<Integer,Long> map){
        long sampleSize = 0L;
        for(Integer rank : map.keySet()){
            sampleSize+=map.get(rank);
        }
        return (int) sampleSize;

    }


    /**
     * 找回用户填写的数据
     * @param token token
     * @return 成功消息
     */
    public List<SurveyCharacterVo> findCharacterForm(String token) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserById(token);
        List<SurveyCharacterVo> surveyCharacterVos = new ArrayList<>();
        if (surveyUser.getCreateTime().getTime() == surveyUser.getUpdateTime().getTime())
            return surveyCharacterVos;  //更新时间和注册时间一致直接返回空

        String tableName = "survey_character_"+ surveyUserService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表

        List<SurveyCharacter> surveyCharacterList = surveyCharacterMapper.selectSurveyCharacterByUid(tableName, surveyUser.getId());
        surveyCharacterList.forEach(e->{
            SurveyCharacterVo build = SurveyCharacterVo.builder()
                    .charId(e.getCharId())
                    .level(e.getLevel())
                    .own(e.getOwn())
                    .elite(e.getElite())
                    .potential(e.getPotential())
                    .rarity(e.getRarity())
                    .skill1(e.getSkill1())
                    .skill2(e.getSkill2())
                    .skill3(e.getSkill3())
                    .modX(e.getModX())
                    .modY(e.getModY())
                    .build();
            surveyCharacterVos.add(build);
        });

        return surveyCharacterVos;
    }


    /**
     * 干员信息统计
     * @return 成功消息
     */
    public HashMap<Object, Object> charStatisticsResult() {
        List<SurveyStatisticsCharacter> surveyStatisticsCharacters = surveyCharacterMapper.selectCharacterStatisticsList();
        HashMap<Object, Object> hashMap = new HashMap<>();

        String userCountStr = surveyUserService.selectConfigByKey("user_count_character");
        String updateTime = surveyUserService.selectConfigByKey("update_time_character");
        double userCount = Double.parseDouble(userCountStr);

        List<CharacterStatisticsResult> characterStatisticsResultList = new ArrayList<>();
        surveyStatisticsCharacters.forEach(item -> {
            CharacterStatisticsResult build = CharacterStatisticsResult.builder()
                    .charId(item.getCharId())
                    .rarity(item.getRarity())
                    .own(item.getOwn()  / userCount)
                    .elite(splitCalculation(item.getElite(), item.getSampleSizeElite()))
                    .skill1(splitCalculation(item.getSkill1(), item.getSampleSizeSkill1()))
                    .skill2(splitCalculation(item.getSkill2(), item.getSampleSizeSkill2()))
                    .skill3(splitCalculation(item.getSkill3(), item.getSampleSizeSkill3()))
                    .modX(splitCalculation(item.getModX(), item.getSampleSizeModX()))
                    .modY(splitCalculation(item.getModY(), item.getSampleSizeModY()))
                    .build();
            characterStatisticsResultList.add(build);
        });

        hashMap.put("userCount", userCountStr);
        hashMap.put("result", characterStatisticsResultList);
        hashMap.put("updateTime", updateTime);

        return hashMap;
    }

    /**
     * 计算具体结果
     * @param  result 旧结果
     * @param  sampleSize 样本
     * @return 计算结果
     */
    public HashMap<String, Double> splitCalculation(String result, Integer sampleSize) {
        HashMap<String, Double> hashMap = new HashMap<>();
        JSONObject jsonObject = JSONObject.parseObject(result);
        jsonObject.forEach((k, v) ->hashMap.put("rank" + k, Double.parseDouble(String.valueOf(v)) / sampleSize));
        return hashMap;
    }


    /**
     * 上传maa干员信息
     * @param maaOperBoxVo  maa识别出的的结果
     * @param ipAddress   ip
     * @return  成功消息
     */
//    public HashMap<Object, Object> saveMaaCharData(MaaOperBoxVo maaOperBoxVo, String ipAddress) {
//        JSONArray operBoxs = maaOperBoxVo.getOperBox();
//        List<SurveyCharacter> surveyCharacterList = new ArrayList<>();
//
//        operBoxs.forEach(item -> {
//            OperBox operBox = JSONObject.parseObject(String.valueOf(item), OperBox.class);
//            SurveyCharacter build = SurveyCharacter.builder()
//                    .charId(operBox.getId())
//                    .level(operBox.getLevel())
//                    .elite(operBox.getElite())
//                    .potential(operBox.getPotential())
//                    .rarity(operBox.getRarity())
//                    .own(operBox.getOwn())
//                    .skill1(-1)
//                    .skill2(-1)
//                    .skill3(-1)
//                    .modX(-1)
//                    .modY(-1)
//                    .build();
//            surveyCharacterList.add(build);
//        });
//
//        SurveyUser surveyUser = surveyService.registerByMaa(ipAddress);
//        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
//        surveyUser.setUpdateTime(new Date());
//        surveyService.updateSurveyUser(surveyUser);
//
//        String tableName = "survey_character_"+ surveyService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表
//
//        Long uid = surveyUser.getId();
//        int insertRows = 0;
//        int updateRows = 0;
//
//
//        List<SurveyCharacter> surveyCharacterVos
//                = surveyCharacterMapper.selectSurveyCharacterByUid(tableName, surveyUser.getId());
//
//        Map<String, SurveyCharacter> surveyDataCharCollectById = surveyCharacterVos.stream()
//                .collect(Collectors.toMap(SurveyCharacter::getId, Function.identity()));
//
//        List<SurveyCharacter> insertList = new ArrayList<>();
//
//        for (SurveyCharacter surveyCharacter : surveyCharacterList) {
//            String id = uid + "_" + surveyCharacter.getCharId(); //存储id
////            if(!surveyDataChar.getOwn())  continue;  //未持有不记录
//
//            //精英化阶段小于2 不能专精和开模组
//            if (surveyCharacter.getElite() < 2 || !surveyCharacter.getOwn()) {
//                surveyCharacter.setSkill1(0);
//                surveyCharacter.setSkill2(0);
//                surveyCharacter.setSkill3(0);
//                surveyCharacter.setModX(0);
//                surveyCharacter.setModY(0);
//            }
//
////            SurveyDataChar surveyDataCharById = surveyMapper.selectSurveyDataCharById(charTable, id);
//            SurveyCharacter surveyCharacterById = surveyDataCharCollectById.get(id);
//            surveyCharacter.setId(id);
//            surveyCharacter.setUid(uid);
//
//            if (surveyCharacterById == null) {
//                insertList.add(surveyCharacter);
//                insertRows++;
//            } else {
//                if (surveyDataCharEquals(surveyCharacter, surveyCharacterById)) {
//                    updateRows++;
//                    surveyCharacterMapper.updateSurveyCharacterById(tableName, surveyCharacter);
//                }
//            }
//        }
//
//        if (insertList.size() > 0) surveyCharacterMapper.insertBatchSurveyCharacter(tableName, insertList);
//
//        HashMap<Object, Object> hashMap = new HashMap<>();
//        hashMap.put("updateRows", updateRows);
//        hashMap.put("insertRows", insertRows);
//        hashMap.put("uid", uid);
//        return hashMap;
//    }









}
