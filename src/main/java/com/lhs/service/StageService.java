package com.lhs.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lhs.common.util.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.HttpRequestUtil;
import com.lhs.mapper.ItemMapper;
import com.lhs.mapper.StageMapper;
import com.lhs.entity.stage.Item;
import com.lhs.entity.stage.Stage;
import com.lhs.entity.stage.StageResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StageService extends ServiceImpl<StageMapper, Stage>  {

    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private StageMapper stageMapper;


    public void savePenguinData(String dataType, String url) {
        String response = HttpRequestUtil.doGet(url, new HashMap<>());
        String saveTime = new SimpleDateFormat("yyyy-MM-dd HH mm").format(new Date()); // 设置日期格式
        FileUtil.save(ConfigUtil.Penguin, "matrix " + dataType + ".json", response);
        FileUtil.save(ConfigUtil.Penguin, "matrix " + saveTime + " " + dataType + ".json", response);

    }
    public List<Stage> findAll(QueryWrapper<Stage> queryWrapper) {

        return stageMapper.selectList(queryWrapper);

    }




    @Transactional
    public void importStageData(MultipartFile file) {
        List<Stage> list = new ArrayList<>();
        Map<String, Item> itemMap = itemMapper.selectList(null).stream().collect(Collectors.toMap(Item::getItemName, Function.identity()));
        JSONObject itemType_table = JSONObject.parseObject(FileUtil.read(ConfigUtil.Item + "itemType_table.json"));
//        JSONObject stageZone_table = JSONObject.parseObject(FileUtil.read(FileConfig.Item + "zone_table.json"));


        try {
            EasyExcel.read(file.getInputStream(), Stage.class, new AnalysisEventListener<Stage>() {
               
                public void invoke(Stage stage, AnalysisContext analysisContext) {
                    try {
                        if (!"0".equals(stage.getMain())) stage.setMainRarity(itemMap.get(stage.getMain()).getRarity());
                        if (!"0".equals(stage.getMain())) stage.setItemType(itemType_table.getString(stage.getMain()));

//                        JSONObject zone = JSONObject.parseObject(stageZone_table.getString(stage.getZoneId()));
//                        stage.setZoneName(zone.getString("zoneName"));
//                        if(zone.get("openTime")==null){
//                            stage.setOpenTime( new Date(1556676029000L));
//                        }else {
//                            stage.setOpenTime(new Date(Long.parseLong(zone.getString("openTime"))));
//                        }

                        stage.setSpm(stage.getApCost() / stage.getMinClearTime() * 60000);
                        if (!"0".equals(stage.getSecondary()))
                            stage.setSecondaryId(itemMap.get(stage.getSecondary()).getItemId());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }


                    if (stage.getZoneId().contains("act")) {
                        stage.setStageState(1);
                        stage.setIsValue(0);
                        stage.setIsShow(0);
                        if (stage.getZoneId().contains("perm")) {
                            stage.setIsShow(1);
                            stage.setIsValue(1);
                        }
                    } else {
                        stage.setIsValue(1);
                        stage.setIsShow(1);
                        stage.setStageState(0);
                    }


                    list.add(stage);
                }

               
                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                }
            }).sheet().doRead();

        } catch (IOException e) {
            e.printStackTrace();
        }

        saveOrUpdateBatch(list);

    }

    public void exportStageData(List<Stage> list) {
        EasyExcel.write("C:\\Users\\李会山\\Desktop\\stage.xlsx",Stage.class).sheet("Sheet1").doWrite(list);
    }
   
    public void exportStageData(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("stages", "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

            List<Stage> list = stageMapper.selectList(null);

            EasyExcel.write(response.getOutputStream(), Stage.class).sheet("Sheet1").doWrite(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

   
    public Integer updateStageInfo(Stage stage) {
        Integer isShow = stage.getIsShow()==1?0:1;
        stage.setIsShow(isShow);
        int update = stageMapper.update(stage,new QueryWrapper<Stage>().eq("stage_id",stage.getStageId()));
        return isShow;
    }

    public LinkedHashMap<String, List<Stage>> queryStageTable(){
        List<Stage> stageList = stageMapper.selectList(new QueryWrapper<Stage>().notLike("stage_id","tough").orderByDesc("stage_id"));
        List<Stage> zoneList = stageMapper.selectList(new QueryWrapper<Stage>().notLike("stage_id","tough").groupBy("zone_id").orderByDesc("stage_id"));
//        zoneList.forEach(System.out::println);
        Map<String, List<Stage>> collect = stageList.stream().collect(Collectors.groupingBy(Stage::getZoneName));
        LinkedHashMap<String, List<Stage>> result = new LinkedHashMap<>();
        zoneList.forEach(stage->result.put(stage.getZoneName(),collect.get(stage.getZoneName())));

        return result;
    }


    public void readGameData_stageFile(){
        String stage_tableStr = FileUtil.read(ConfigUtil.Item + "stage_table.json");
        String itemType_tableStr = FileUtil.read(ConfigUtil.Item + "itemType_table.json");
        String activity_tableStr = FileUtil.read(ConfigUtil.Item + "activity_table.json");

        if(stage_tableStr!=null);
        List<Item> items = itemMapper.selectList(null);


        Map<String, Item> item_table = items.stream().collect(Collectors.toMap(Item::getItemId, Function.identity()));
        JSONObject stage_table = JSONObject.parseObject(JSONObject.parseObject(stage_tableStr).getString("stages"));
        JSONObject itemType_table = JSONObject.parseObject(itemType_tableStr);
        JSONObject basicInfo = JSONObject.parseObject(JSONObject.parseObject(activity_tableStr).getString("basicInfo"));

        List<Stage> stageList = new ArrayList<>();
        stage_table.forEach((stageId,info)->{
            if((stageId.startsWith("main")||stageId.startsWith("a")||stageId.startsWith("su")||stageId.startsWith("to"))
                    &&!(stageId.contains("#")||stageId.contains("ex")||stageId.contains("st")
                    ||stageId.contains("tr")||stageId.contains("_s")||stageId.contains("_t")
                    ||stageId.contains("bossrush")||stageId.contains("_mo")||stageId.contains("lock"))){
                JSONObject stageInfo = JSONObject.parseObject(String.valueOf(info));
                String stageDropInfo = stageInfo.getString("stageDropInfo");
                String main = "0";
                String secondary  = "0";
                String secondaryId = "0";
                if(stageDropInfo!=null) {
                    JSONArray displayRewards = JSONArray.parseArray(JSONObject.parseObject(stageDropInfo).getString("displayRewards"));
                    for(Object drop:displayRewards){
                        JSONObject dropInfo = JSONObject.parseObject(String.valueOf(drop));
                        int dropType = Integer.parseInt( dropInfo.getString("dropType"));
                        if("MATERIAL".equals(dropInfo.getString("type"))&&item_table.get(dropInfo.getString("id"))!=null) {
                            if (dropType == 2) main = dropInfo.getString("id");
                            if (dropType == 3) secondary = dropInfo.getString("id");
                        }
                    }
                }

                secondaryId = item_table.get(secondary).getItemId();

                Stage stage = Stage.builder().stageId(stageInfo.getString("stageId"))
                        .stageCode(stageInfo.getString("code"))
                        .zoneId(stageInfo.getString("zoneId").replace("_zone1","").replace("_zone2","").replace("_zone3",""))
                        .apCost(Double.parseDouble(stageInfo.getString("apCost")))
                        .main(item_table.get(main).getItemName())
                        .secondary(item_table.get(secondary).getItemName())
                        .mainRarity(item_table.get(main).getRarity())
                        .secondaryId(secondaryId)
                        .itemType( itemType_table.getString(item_table.get(main).getItemName()))
                        .type(stageInfo.getString("stageType"))
                        .build();

                if(stageId.startsWith("a")) {
                    stage.setIsValue(0);
                    stage.setIsShow(0);
                }
                if(stageId.startsWith("m")||stageId.startsWith("s")||stageId.contains("perm")){
                    stage.setIsValue(1);
                    stage.setIsShow(1);
                }
                if(basicInfo.getString(stage.getZoneId())!=null){
//                    System.out.println(stage.getZoneId());
                    JSONObject basicInfoByStageId = JSONObject.parseObject(basicInfo.getString(stage.getZoneId()));
//                    System.out.println("基础信息："+basicInfoByStageId);
                    long startTime = Long.parseLong(basicInfoByStageId.getString("startTime"));
                    stage.setOpenTime(new Date(startTime));
                }

                stageList.add(stage);

                if(!"0".equals( stage.getMain())) System.out.println(stage);
            }
        });

        exportStageData(stageList);
    }
}
