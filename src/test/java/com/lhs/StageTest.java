package com.lhs;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lhs.common.util.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.entity.stage.Item;
import com.lhs.entity.stage.Stage;
import com.lhs.mapper.ItemMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootTest
public class StageTest {

    @Resource
    private ItemMapper itemMapper;

    @Test
     void readGameData_stageFile(){
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
                    JSONObject basicInfoByStageId = JSONObject.parseObject(basicInfo.getString(stage.getZoneId()));
                    long startTime = Long.parseLong(basicInfoByStageId.getString("startTime"));
                    stage.setOpenTime(new Date(startTime));
                }

                stageList.add(stage);

                if(!"0".equals( stage.getMain())) System.out.println(stage);
            }
        });

        EasyExcel.write("C:\\Users\\李会山\\Desktop\\stage.xlsx",Stage.class).sheet("Sheet1").doWrite(stageList);
    }


}
