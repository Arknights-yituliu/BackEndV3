package com.lhs.service.util.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lhs.common.util.*;
import com.lhs.entity.dto.hypergryph.GameDataFormatFilePath;
import com.lhs.entity.dto.maa.BuildingData;
import com.lhs.service.util.ArknightsGameDataService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ArknightsGameDataServiceImpl implements ArknightsGameDataService {



    // 测试路径
//    private final static String GAME_DATA = "C:/Users/22509/Downloads/";
//    private final static String JSON_BUILD = "C:/Users/22509/Downloads/";

    private final RedisTemplate<String, Object> redisTemplate;


    public ArknightsGameDataServiceImpl( RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }



    @Override
    public void saveOperatorDataTag(String tag) {
        redisTemplate.opsForValue().set("Tag:OperatorData", tag);
    }

    @Override
    public String getOperatorDataTag() {
        Object value = redisTemplate.opsForValue().get("Tag:OperatorData");
        if (value == null) {
            return "114514";
        }
        return String.valueOf(value);
    }




    @Override
    public void getAvatar(GameDataFormatFilePath gameDataFormatFilePath) {
        try {
            // 创建流对象

            String character_tableStr = FileUtil.read(gameDataFormatFilePath.getArknightsGameResourcePath() + "excel/character_table.json");
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode characterTable = objectMapper.readTree(character_tableStr);


            String startPath = gameDataFormatFilePath.getArknightsGameResourceAvatarPath() ;


            Iterator<Map.Entry<String, JsonNode>> fields = characterTable.fields();

            while (fields.hasNext()) {

                String charId = fields.next().getKey();
                if (!charId.startsWith("char")) continue;
                JsonNode charData = characterTable.get(charId);
                int rarity = getRarity(charData.get("rarity").asText());
                File source = new File(startPath + charId + ".png");


                String  endPath = gameDataFormatFilePath.getImageOutputPath();

                File tmpFile = new File(endPath);//获取文件夹路径

                if (!tmpFile.exists()) {//判断文件夹是否创建，没有创建则创建新文件夹
                    boolean mkdirs = tmpFile.mkdirs();
                }

                File dest = new File(endPath + charId + ".png");
                System.out.println(endPath + charId + ".png");
                FileUtil.copyFile(source, dest);

            }

        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
        }
    }



    @Override
    public void getOperatorInfoSimpleTable(GameDataFormatFilePath gameDataFormatFilePath) {
        //干员信息
        String character_tableText = FileUtil.read(gameDataFormatFilePath.getArknightsGameDataPath() + "excel/character_table.json");
        JsonNode characterTable = JsonMapper.parseJSONObject(character_tableText);
        Iterator<Map.Entry<String, JsonNode>> characterTableFields = characterTable.fields();


        //生变干员信息
        String char_patch_tableText = FileUtil.read(gameDataFormatFilePath.getArknightsGameDataPath() + "excel/char_patch_table.json");
        JsonNode charPatchTable = JsonMapper.parseJSONObject(char_patch_tableText);
        JsonNode patchChars = charPatchTable.get("patchChars");
        Iterator<Map.Entry<String, JsonNode>> patchCharsFields = patchChars.fields();


        Map<String, Object> operatorInfoSimpleMap = new HashMap<>();
        Map<String, Object> itemCostMap = new HashMap<>();



        Map<Object, String> skillMap = getSkillMap(gameDataFormatFilePath.getArknightsGameDataPath());
        Map<String, List<Map<String, Object>>> equipInfoMap = getEquipInfoMap(gameDataFormatFilePath.getArknightsGameDataPath());

        while (characterTableFields.hasNext()) {
            String charId = characterTableFields.next().getKey();

            if (!charId.startsWith("char")) {
                continue;
            }

            JsonNode data = characterTable.get(charId);

            if (data.get("itemObtainApproach") == null ||
                    Objects.equals(data.get("itemObtainApproach").asText(), "null")) {
                continue;
            }

            Map<String, Object> operatorInfo = getOperatorInfo(charId, data, skillMap, equipInfoMap,0);
            Map<String, Object> operatorItemCost = getOperatorItemCost(charId, data, skillMap, equipInfoMap);
            itemCostMap.put(charId, operatorItemCost);
            operatorInfoSimpleMap.put(charId, operatorInfo);
        }

        while (patchCharsFields.hasNext()) {
            String charId = patchCharsFields.next().getKey();
            JsonNode data = patchChars.get(charId);

            Map<String, Object> operatorInfo = getOperatorInfo(charId, data, skillMap, equipInfoMap,0);
            Map<String, Object> operatorItemCost = getOperatorItemCost(charId, data, skillMap, equipInfoMap);

            operatorItemCost.put("elite",new ArrayList<>());
            operatorItemCost.put("allSkill",new ArrayList<>());
            itemCostMap.put(charId, operatorItemCost);
            operatorInfoSimpleMap.put(charId, operatorInfo);
        }


        FileUtil.saveJsonFile(gameDataFormatFilePath.getJsonOutputPath() + "src/static/json/operator/",
                "character_table_simple.json", JsonMapper.toJSONString(operatorInfoSimpleMap));


        FileUtil.saveJsonFile(gameDataFormatFilePath.getJsonOutputPath() + "src/static/json/operator/",
                "operator_item_cost_table.json", JsonMapper.toJSONString(itemCostMap));

    }

    @Override
    public void getOperatorInfoSimpleTableByGameResource(GameDataFormatFilePath gameDataFormatFilePath) {
        //干员信息
        String character_tableText = FileUtil.read(gameDataFormatFilePath.getArknightsGameResourcePath() + "excel/character_table.json");
        JsonNode characterTable = JsonMapper.parseJSONObject(character_tableText);
        Iterator<Map.Entry<String, JsonNode>> characterTableFields = characterTable.fields();


        //生变干员信息
        String char_patch_tableText = FileUtil.read(gameDataFormatFilePath.getArknightsGameResourcePath() + "excel/char_patch_table.json");
        JsonNode charPatchTable = JsonMapper.parseJSONObject(char_patch_tableText);
        JsonNode patchChars = charPatchTable.get("patchChars");
        Iterator<Map.Entry<String, JsonNode>> patchCharsFields = patchChars.fields();


        Map<String, Object> operatorInfoSimpleMap = new HashMap<>();
        Map<String, Object> itemCostMap = new HashMap<>();



        Map<Object, String> skillMap = getSkillMap(gameDataFormatFilePath.getArknightsGameResourcePath());
        Map<String, List<Map<String, Object>>> equipInfoMap = getEquipInfoMap(gameDataFormatFilePath.getArknightsGameResourcePath());

        while (characterTableFields.hasNext()) {
            String charId = characterTableFields.next().getKey();

            if (!charId.startsWith("char")) {
                continue;
            }

            JsonNode data = characterTable.get(charId);

            if (data.get("itemObtainApproach") == null ||
                    Objects.equals(data.get("itemObtainApproach").asText(), "null")) {
                continue;
            }

            Map<String, Object> operatorInfo = getOperatorInfo(charId, data, skillMap, equipInfoMap,1);
            Map<String, Object> operatorItemCost = getOperatorItemCost(charId, data, skillMap, equipInfoMap);
            itemCostMap.put(charId, operatorItemCost);
            operatorInfoSimpleMap.put(charId, operatorInfo);
        }

        while (patchCharsFields.hasNext()) {
            String charId = patchCharsFields.next().getKey();
            JsonNode data = patchChars.get(charId);

            Map<String, Object> operatorInfo = getOperatorInfo(charId, data, skillMap, equipInfoMap,1);
            Map<String, Object> operatorItemCost = getOperatorItemCost(charId, data, skillMap, equipInfoMap);

            operatorItemCost.put("elite",new ArrayList<>());
            operatorItemCost.put("allSkill",new ArrayList<>());
            itemCostMap.put(charId, operatorItemCost);
            operatorInfoSimpleMap.put(charId, operatorInfo);
        }


        FileUtil.saveJsonFile(gameDataFormatFilePath.getJsonOutputPath() + "src/static/json/operator/",
                "character_table_simple.json", JsonMapper.toJSONString(operatorInfoSimpleMap));


        FileUtil.saveJsonFile(gameDataFormatFilePath.getJsonOutputPath() + "src/static/json/operator/",
                "operator_item_cost_table.json", JsonMapper.toJSONString(itemCostMap));

    }


    @Override
    public void getBuildingTable(GameDataFormatFilePath gameDataFormatFilePath) {
        //读取基建相关解包文件
        String read = FileUtil.read(gameDataFormatFilePath.getArknightsGameResourcePath() + "excel/building_data.json");
        String read1 = FileUtil.read(gameDataFormatFilePath.getArknightsGameResourcePath() + "excel/character_table.json");


        //获取干员部分信息，测试时需分离
        JsonNode building = JsonMapper.parseJSONObject(read);
        JsonNode characters = JsonMapper.parseJSONObject(read1);
        JsonNode buffs = building.get("buffs");
        JsonNode chars = building.get("chars");
        List<BuildingData> buildingDataList = new ArrayList<>();
        for (JsonNode jsonNode : chars) {
            String charId = jsonNode.get("charId").asText();
            JsonNode character = characters.get(charId);
            String name = character.get("name").asText();
            JsonNode buffChar = jsonNode.get("buffChar");
            for (JsonNode buffCharElement : buffChar) {
                JsonNode buffData = buffCharElement.get("buffData");
                for (JsonNode buffDataElement : buffData) {
                    String buffId = buffDataElement.get("buffId").asText();
                    JsonNode cond = buffDataElement.get("cond");
                    BuildingData buildingData = new BuildingData();
                    buildingData.setCharId(charId);
                    buildingData.setLevel(cond.get("level").asInt());
                    buildingData.setPhase(getPhase(cond.get("phase").asText()));
                    JsonNode buff = buffs.get(buffId);
                    if (buff == null) continue;


                    String buffName = buff.get("buffName").asText();
                    String buffColor = buff.get("buffColor").asText();
                    String textColor = buff.get("textColor").asText();
                    String description = buff.get("description").asText();
                    String roomType = buff.get("buffIcon").asText();
                    buildingData.setBuffName(buffName);
                    buildingData.setBuffColor(buffColor);
                    buildingData.setTextColor(textColor);
//                    if (name.equals("假日威龙陈")) {
//                        System.out.println(description);
//                    }

                    buildingData.setDescription(replaceDescription(description));
                    buildingData.setRoomType(roomType);
                    buildingData.setName(name);
                    buildingDataList.add(buildingData);

                }
            }
        }


//        Map<String, List<BuildingData>> collect = buildingDataList.stream()
//                .collect(Collectors.groupingBy(BuildingData::getRoomType));
//        buildingDataList.sort(Comparator.comparing(BuildingData::getTimestamp).reversed());
        Collections.reverse(buildingDataList); //解包出来的数据，新干员永远在末尾处，直接对列表进行倒序可以让最新的干员位于表格渲染的最上位置
        FileUtil.saveJsonFile(gameDataFormatFilePath.getJsonOutputPath() + "src/static/json/build/", "building_table.json", JsonMapper.toJSONString(buildingDataList));
        // 测试输出路径
//        FileUtil.save(JSON_BUILD, "building_table.json", JsonMapper.toJSONString(buildingDataList));
    }


    @Override
    public void getBuildingTableByGameResource(GameDataFormatFilePath gameDataFormatFilePath) {
        //读取基建相关解包文件
        String read = FileUtil.read(gameDataFormatFilePath.getArknightsGameResourcePath()+ "excel/building_data.json");
        String read1 = FileUtil.read(gameDataFormatFilePath.getArknightsGameResourcePath() + "excel/character_table.json");


        //获取干员部分信息，测试时需分离
        JsonNode building = JsonMapper.parseJSONObject(read);
        JsonNode characters = JsonMapper.parseJSONObject(read1);
        JsonNode buffs = building.get("buffs");
        JsonNode chars = building.get("chars");
        List<BuildingData> buildingDataList = new ArrayList<>();
        for (JsonNode jsonNode : chars) {
            String charId = jsonNode.get("charId").asText();
            JsonNode character = characters.get(charId);

            String name = character.get("name").asText();
            JsonNode buffChar = jsonNode.get("buffChar");
            for (JsonNode buffCharElement : buffChar) {
                JsonNode buffData = buffCharElement.get("buffData");
                for (JsonNode buffDataElement : buffData) {
                    String buffId = buffDataElement.get("buffId").asText();
                    JsonNode cond = buffDataElement.get("cond");
                    BuildingData buildingData = new BuildingData();
                    buildingData.setCharId(charId);
                    buildingData.setLevel(cond.get("level").asInt());
                    buildingData.setPhase(getPhase(cond.get("phase").asText()));
                    JsonNode buff = buffs.get(buffId);
                    if (buff == null) continue;


                    String buffName = buff.get("buffName").asText();
                    String buffColor = buff.get("buffColor").asText();
                    String textColor = buff.get("textColor").asText();
                    String description = buff.get("description").asText();
                    String roomType = buff.get("buffIcon").asText();
                    buildingData.setBuffName(buffName);
                    buildingData.setBuffColor(buffColor);
                    buildingData.setTextColor(textColor);
//                    if (name.equals("假日威龙陈")) {
//                        System.out.println(description);
//                    }

                    buildingData.setDescription(replaceDescription(description));
                    buildingData.setRoomType(roomType);
                    buildingData.setName(name);
                    buildingDataList.add(buildingData);

                }
            }
        }


//        Map<String, List<BuildingData>> collect = buildingDataList.stream()
//                .collect(Collectors.groupingBy(BuildingData::getRoomType));
//        buildingDataList.sort(Comparator.comparing(BuildingData::getTimestamp).reversed());
        Collections.reverse(buildingDataList); //解包出来的数据，新干员永远在末尾处，直接对列表进行倒序可以让最新的干员位于表格渲染的最上位置
        FileUtil.saveJsonFile(gameDataFormatFilePath.getJsonOutputPath() + "src/static/json/build/", "building_table.json", JsonMapper.toJSONString(buildingDataList));
        // 测试输出路径
//        FileUtil.save(JSON_BUILD, "building_table.json", JsonMapper.toJSONString(buildingDataList));
    }

    private Map<String, List<Map<String, Object>>> getEquipInfoMap(String filePath) {

        Map<String, List<Map<String, Object>>> equipInfoMap = new HashMap<>();

        String uniequip_tableText = FileUtil.read(filePath + "excel/uniequip_table.json");
        JsonNode uniequip_table = JsonMapper.parseJSONObject(uniequip_tableText);
        JsonNode equipDict = uniequip_table.get("equipDict");
        Iterator<Map.Entry<String, JsonNode>> equipDictElements = equipDict.fields();


        while (equipDictElements.hasNext()) {
            String equipId = equipDictElements.next().getKey();
            JsonNode equipData = equipDict.get(equipId);

            // 提前跳过无效数据
            if (equipId.contains("_001_") || equipData == null) {
                continue;
            }

            String charId = equipData.get("charId").asText();
            if(!Objects.equals(equipData.get("tmplId").asText(), "null")){

                charId = equipData.get("tmplId").asText();
            }

            Map<String, Object> equipMap = createEquipMap(equipData, equipId, charId);

            // 使用computeIfAbsent优化Map操作
            equipInfoMap.computeIfAbsent(charId, k -> new ArrayList<>()).add(equipMap);
        }



        return equipInfoMap;
    }


    // 提取模组信息
    private Map<String, Object> createEquipMap(JsonNode equipData, String equipId, String charId) {
        Map<String, Object> tempMap = new HashMap<>(8); // 指定初始容量

        tempMap.put("charId", charId);
        tempMap.put("uniEquipId", equipId);
        tempMap.put("typeName1", getTextOrEmpty(equipData, "typeName1"));
        tempMap.put("typeName2", getTextOrEmpty(equipData, "typeName2"));
        tempMap.put("uniEquipIcon", getTextOrEmpty(equipData, "uniEquipIcon"));
        tempMap.put("typeIcon", getTextOrEmpty(equipData, "typeIcon").toLowerCase());
        tempMap.put("uniEquipName", getTextOrEmpty(equipData, "uniEquipName"));
        tempMap.put("itemCost", parseItemCost(equipData.get("itemCost")));

        return tempMap;
    }

    // 提取方法：安全获取文本字段
    private String getTextOrEmpty(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null ? fieldNode.asText() : "";
    }


    // 提取方法：解析物品消耗
    private List<Map<String, Object>> parseItemCost(JsonNode itemCost) {

        System.out.println(itemCost);
        List<Map<String, Object>> itemCostMapList = new ArrayList<>();
        for (JsonNode rank : itemCost) {
            Map<String, Object> itemCostMap = new HashMap<>();
            for (JsonNode cost : rank) {
                String id = cost.get("id").asText();
                int count = cost.get("count").intValue();
                itemCostMap.put(id, count);
            }
            itemCostMapList.add(itemCostMap);
        }
        return itemCostMapList;
    }


    private Map<Object, String> getSkillMap(String filePath) {
        String skill_tableText = FileUtil.read(filePath + "excel/skill_table.json");
        JsonNode skillTable = JsonMapper.parseJSONObject(skill_tableText);
        Map<Object, String> skillMap = new HashMap<>();
        //将技能数据转为一个可迭代的键值对
        Iterator<Map.Entry<String, JsonNode>> skillTableElements = skillTable.fields();
        while (skillTableElements.hasNext()) {
            //技能id
            String skillId = skillTableElements.next().getKey();
            //技能图标id
            String iconId = skillTable.get(skillId).get("iconId").asText();
            //技能名称
            String skillName = skillTable.get(skillId).get("levels").get(0).get("name").asText();
            //将技能id和技能名称添加到技能名称映射表中
            skillMap.put(skillId, skillName);
            //将技能图标和技能图标id添加到技能图标映射表中
            skillMap.put(skillId + "icon", iconId);
            if (Objects.equals(iconId, "null")) {
                skillMap.put(skillId + "icon", skillId);
            }
        }

        return skillMap;
    }

    private Map<String, Object> getOperatorInfo(String charId, JsonNode data, Map<Object, String> skillMap,
                                                Map<String, List<Map<String, Object>>> equipMap,Integer index) {

        Map<String, Object> operatorInfo = new HashMap<>();

        //获取干员的技能信息
        JsonNode skills = data.get("skills");
        List<HashMap<Object, Object>> skillList = new ArrayList<>();
        for (int i = 0; i < skills.size(); i++) {
            JsonNode jsonNode = skills.get(i);
            //获取技能id
            String skillId = jsonNode.get("skillId").asText();
            //获取技能图标
            String iconId = skillMap.get(skillId + "icon");
            //将部分特殊符号替换
            iconId = iconId.replace("[", "x5b");
            iconId = iconId.replace("]", "x5d");
            HashMap<Object, Object> skill = new HashMap<>();
            //保存技能的图标和名称
            skill.put("iconId", iconId);
            skill.put("name", skillMap.get(skillId));
            skillList.add(skill);
        }

        //干员中文名称
        String name = data.get("name").asText();
        if ("char_1001_amiya2".equals(charId)) {
            name = "阿米娅（近卫）";
        }
        if ("char_1037_amiya3".equals(charId)) {
            name = "阿米娅（医疗）";
        }

        //职业
        String profession = data.get("profession").asText();
        //星级
        int rarity = getRarity(data.get("rarity").asText())+index;
        //分支
        String subProfessionId = data.get("subProfessionId").asText();


//        System.out.println( equipMap.get(charId));

        operatorInfo.put("name", name);
        operatorInfo.put("charId", charId);
        operatorInfo.put("rarity", rarity);
        operatorInfo.put("equip", equipMap.get(charId));
        operatorInfo.put("skill", skillList);
        operatorInfo.put("profession", profession);
        operatorInfo.put("subProfessionId", subProfessionId);
        operatorInfo.put("own", false);
        operatorInfo.put("level", 0);
        operatorInfo.put("elite", 0);
        operatorInfo.put("potential", 0);
        operatorInfo.put("mainSkill", 0);
        operatorInfo.put("skill1", 0);
        operatorInfo.put("skill2", 0);
        operatorInfo.put("skill3", 0);
        operatorInfo.put("modX", 0);
        operatorInfo.put("modY", 0);
        operatorInfo.put("modD", 0);
        operatorInfo.put("show", rarity == 6);


        return operatorInfo;
    }

    private Map<String, Object> getOperatorItemCost(String charId, JsonNode data, Map<Object, String> skillMap, Map<String, List<Map<String, Object>>> equipMap) {
        Map<String, Object> operator = new HashMap<>();
        JsonNode phases = data.get("phases");
        List<HashMap<String, Integer>> eliteList = new ArrayList<>();
        for (JsonNode phase : phases) {
            if (phase.get("evolveCost") != null) {
                JsonNode evolveCost = phase.get("evolveCost");
                HashMap<String, Integer> itemCostMap = new HashMap<>();
                for (JsonNode cost : evolveCost) {
                    String id = cost.get("id").asText();
                    int count = cost.get("count").intValue();
                    itemCostMap.put(id, count);
                }
                eliteList.add(itemCostMap);
            }
        }

        JsonNode allSkillLvlup = data.get("allSkillLvlup");
        List<HashMap<String, Integer>> allSkillList = new ArrayList<>();
        for (JsonNode allSkill : allSkillLvlup) {
            JsonNode lvlUpCost = allSkill.get("lvlUpCost");
            HashMap<String, Integer> itemCostMap = new HashMap<>();
            for (JsonNode cost : lvlUpCost) {
                String id = cost.get("id").asText();
                int count = cost.get("count").intValue();
                itemCostMap.put(id, count);
            }
            allSkillList.add(itemCostMap);
        }

        JsonNode skills = data.get("skills");
        List<List<Map<String, Integer>>> skillsList = new ArrayList<>();
        for (int i = 0; i < skills.size(); i++) {
            List<Map<String, Integer>> itemCostList = new ArrayList<>();
            JsonNode levelUpCostCond = skills.get(i).get("levelUpCostCond");
            for (int j = 0; j < levelUpCostCond.size(); j++) {
                JsonNode levelUpCost = levelUpCostCond.get(j).get("levelUpCost");
                Map<String, Integer> itemCostMap = new HashMap<>();
                for (JsonNode cost : levelUpCost) {
                    String id = cost.get("id").asText();
                    int count = cost.get("count").intValue();
                    itemCostMap.put(id, count);
                }
                itemCostList.add(itemCostMap);
            }
            skillsList.add(itemCostList);
        }

        operator.put("allSkill", allSkillList);
        operator.put("skills", skillsList);
        operator.put("elite", eliteList);

        if (equipMap.get(charId) != null) {
            List<Map<String, Object>> list = equipMap.get(charId);
//            System.out.println(list);
            for (Map<String, Object> item : list) {
                String typeName2 = String.valueOf(item.get("typeName2"));
                Object o = item.get("itemCost");
//                System.out.println(o);
                operator.put("mod" + typeName2, o);
            }
        }

        return operator;
    }



    private Integer getPhase(String text) {
        return Integer.parseInt(text.replace("PHASE_", ""));
    }

    private Integer getRarity(String str) {
        return Integer.parseInt(str.replace("TIER_", ""));
    }

    private String replaceDescription(String str) {
//        Map<String, String> classMap = new HashMap<String, String>();
//
//        classMap.put("<$cc.tag.durin>", "<span class='cc-tag-durin'>");
//        classMap.put("<$cc.tag.op>", "<span class='cc-tag-op'>"); //作业平台
//        classMap.put("<$cc.tag.mh>", "<span class='cc-tag-mh'>");
//        classMap.put("<$cc.tag.knight>", "<span class='cc-tag-knight'>"); //骑士
//
//        classMap.put("<$cc.bd_b1>", "<span class='cc-bd-b1'>"); //人间烟火
//        classMap.put("<$cc.bd_a1>", "<span class='cc-bd-a1'>"); //感知信息
//        classMap.put("<$cc.bd_A>", "<span class='cc-bd-A'>"); //感知信息
//        classMap.put("<$cc.bd_C>", "<span class='cc-bd-C'>"); //巫术结晶
//        classMap.put("<$cc.bd_B>", "<span class='cc-bd-B'>"); //人间烟火
//        classMap.put("<$cc.bd_malist>", "<span class='cc-bd-malist'>"); //工程机器人
//        classMap.put("<$cc.bd_a1_a1>", "<span class='cc-bd-a1-a1'>");  //记忆碎片
//        classMap.put("<$cc.bd_a1_a2>", "<span class='cc-bd-a1-a2'>");  //梦境
//        classMap.put("<$cc.bd_a1_a3>", "<span class='cc-bd-a1-a3'>");  //小节
//        classMap.put("<$cc.bd_ash>", "<span class='cc-bd-ash'>"); //彩六
//        classMap.put("<$cc.bd_tachanka>", "<span class='cc-bd-tachanka'>");  //彩六
//        classMap.put("<$cc.bd_felyne>", "<span class='cc-bd-felyne'>");  //彩六
//
//
//        classMap.put("<$cc.t.strong2>", "<span class='cc-t-strong2'>"); //强调
//        classMap.put("<$cc.t.flow_gold>", "<span class='cc-t-flow-gold'>");//赤金线
//
//        classMap.put("<$cc.c.room3>", "<span class='cc-c-room3'>");
//        classMap.put("<$cc.c.room1>", "<span class='cc-c-room1'>");
//        classMap.put("<$cc.c.room2>", "<span class='cc-c-room2'>");
//        classMap.put("<$cc.c.skill>", "<span class='cc-c-skill'>");
//        classMap.put("<$cc.c.abyssal2_3>", "<span class='cc-c-abyssal2_3'>");
//        classMap.put("<$cc.c.abyssal2_1>", "<span class='cc-c-abyssal2_1'>"); //深海猎人
//        classMap.put("<$cc.c.abyssal2_2>", "<span class='cc-c-abyssal2_2'>"); //深海猎人
//        classMap.put("<$cc.c.sui2_1>", "<span class='cc-c-sui2_1'>"); //深海猎人
//        classMap.put("<$cc.c.sui2_2>", "<span class='cc-c-sui2_2'>"); //深海猎人
//
//        classMap.put("<$cc.m.var1>", "<span class='cc-m-var1'>");
//
//
//        classMap.put("<$cc.sk.manu1>", "<span class='cc-sk-manu1'>"); //标准化类技能
//        classMap.put("<$cc.sk.manu2>", "<span class='cc-sk-manu2'>"); //莱茵科技类技能
//        classMap.put("<$cc.sk.manu3>", "<span class='cc-sk-manu3'>"); //红松骑士团类
//        classMap.put("<$cc.sk.manu4>", "<span class='cc-sk-manu4'>"); //金属工艺类技能
//
//
//        classMap.put("<$cc.w.ncdeer1>", "<span class='cc-bd-ncdeer1'>");  //九色鹿
//        classMap.put("<$cc.w.ncdeer2>", "<span class='cc-bd-ncdeer2'>");  //九色鹿
//
//        classMap.put("<$cc.g.glasgow>", "<span class='cc-g-glasgow'>"); //格拉斯哥帮
//        classMap.put("<$cc.g.bs>", "<span class='cc-g-bs'>"); //黑钢
//        classMap.put("<$cc.g.sm>", "<span class='cc-g-sm'>"); //萨米
//        classMap.put("<$cc.g.lgd>", "<span class='cc-g-lgd'>"); //近卫局
//        classMap.put("<$cc.g.lda>", "<span class='cc-g-lda'>"); //鲤氏
//        classMap.put("<$cc.g.sp>", "<span class='cc-g-sp'>"); //异格
//        classMap.put("<$cc.g.R6>", "<span class='cc-g-R6'>"); //彩六
//        classMap.put("<$cc.g.ussg>", "<span class='cc-g-ussg'>"); //彩六
//        classMap.put("<$cc.g.abyssal>", "<span class='cc-g-abyssal'>"); //深海猎人
//        classMap.put("<$cc.g.knight>", "<span class='cc-g-lda'>"); //骑士
//        classMap.put("<$cc.g.mh>", "<span class='cc-g-mh'>"); //怪物猎人
//        classMap.put("<$cc.g.op>", "<span class='cc-g-op'>"); //异格
//        classMap.put("<$cc.g.rh>", "<span class='cc-g-rh'>"); //彩六
//        classMap.put("<$cc.g.psk>", "<span class='cc-g-psk'>"); //怪物猎人
//        classMap.put("<$cc.g.karlan>", "<span class='cc-g-karlan'>"); //喀兰
//        classMap.put("<$cc.g.sui>", "<span class='cc-g-sui'>"); //岁
//
//        classMap.put("<$cc.gvial>", "<span class='cc-gvial'>"); //彩六
//        classMap.put("<$cc.t.accmuguard1>", "<span class='cc-t-accmuguard1'>");
//
//
//        classMap.put("<$cc.bd.costdrop>", "<span class='cc-bd-costdrop'>"); //心情落差

        str = replaceDescriptionBase(str);

        // 匹配术语标签
        Pattern pattern = Pattern.compile("<\\$cc\\.(.*?)>");
        Matcher matcher = pattern.matcher(str);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String replacement = "<span data-termId='cc-" + matcher.group(1).replace('.', '-') + "' class='cc-base'>";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        str = sb.toString();

        return str;
    }

    private String replaceDescriptionBase(String description) {
        // 匹配常规标签
        Map<String, String> spliceClassMap = new HashMap<>();
        spliceClassMap.put("<@cc.vup>", "<span class='cc-vup'>");
        spliceClassMap.put("<@cc.kw>", "<span class='cc-kw'>");
        spliceClassMap.put("<@cc.vdown>", "<span class='cc-vdown'>");
        spliceClassMap.put("<@cc.rem>", "<span gamedata_const class='cc-rem'>");
        spliceClassMap.put("</>", "</span>");
        spliceClassMap.put("\n", "<br>");

        for (Map.Entry<String, String> entry : spliceClassMap.entrySet()) {
            description = description.replace(entry.getKey(), entry.getValue());
        }

        // 删除多余的“<”或“>”
        description = HTMLUtil.removeExcessParentheses(description);

        return description;
    }


    @Override
    public void getTermDescriptionTable(GameDataFormatFilePath gameDataFormatFilePath) {
        String read = FileUtil.read(gameDataFormatFilePath.getArknightsGameDataPath() + "excel/gamedata_const.json");
        // 测试路径
//        String read = FileUtil.read(GAME_DATA + "gamedata_const.json");
        JsonNode rootNode = JsonMapper.parseJSONObject(read);
        JsonNode termDescriptionDict = rootNode.get("termDescriptionDict");

        if (termDescriptionDict != null) {
            Map<String, Map<String, String>> termDescriptionMap = createTermDescriptionJson(termDescriptionDict);
            FileUtil.saveJsonFile(gameDataFormatFilePath.getJsonOutputPath() + "src/static/json/build/", "term_description.json", JsonMapper.toJSONString(termDescriptionMap));
            // 测试输出路径
//            FileUtil.saveJsonFile(JSON_BUILD, "term_description.json", JsonMapper.toJSONString(termDescriptionMap));
        }
    }

    private Map<String, Map<String, String>> createTermDescriptionJson(JsonNode termDescriptionDict) {
        Map<String, Map<String, String>> termDescriptionMap = new HashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fields = termDescriptionDict.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String termId = entry.getKey();

            if (termId.startsWith("cc")) { // 仅添加键名以cc开头的节点
                JsonNode termData = entry.getValue();
                String termName = termData.get("termName").asText();
                String description = replaceDescriptionBase(termData.get("description").asText());

                // 将术语中的术语标签替换为常规标签，防止渲染错误（术语提示文本框中无法再提示术语）
                Pattern pattern = Pattern.compile("<\\$cc\\.(.*?)>");
                Matcher matcher = pattern.matcher(description);
                StringBuilder sb = new StringBuilder();

                while (matcher.find()) {
                    String replacement = "<span class='cc-base'>";
                    matcher.appendReplacement(sb, replacement);
                }
                matcher.appendTail(sb);
                description = sb.toString();

                Map<String, String> termEntry = new HashMap<>();
                termId = termId.replace(".", "-");
                termEntry.put("termName", termName);
                termEntry.put("description", description);

                termDescriptionMap.put(termId, termEntry);
            }
        }

        return termDescriptionMap;
    }

    @Override
    public void getSandboxFoodsTable(GameDataFormatFilePath gameDataFormatFilePath) {
        String read = FileUtil.read(gameDataFormatFilePath.getArknightsGameDataPath() + "excel/sandbox_perm_table.json");
//        String read = FileUtil.read(GAME_DATA + "sandbox_perm_table.json");
        JsonNode rootNode = JsonMapper.parseJSONObject(read);
        JsonNode v2FoodsDetail = rootNode.get("detail").get("SANDBOX_V2").get("sandbox_1");
        JsonNode v2ItemData = rootNode.get("itemData");

        if (v2FoodsDetail != null) {
            Map<String, Object> foodsMap = createFoodsDataJson(v2FoodsDetail.get("foodMatData"), v2FoodsDetail.get("foodData"), v2ItemData);
            FileUtil.saveJsonFile(gameDataFormatFilePath.getJsonOutputPath() + "src/static/json/build/", "sandbox_foods_v2.json", JsonMapper.toJSONString(foodsMap));
        }
    }

    private Map<String, Object> createFoodsDataJson(JsonNode foodMatDataNode, JsonNode foodDataNode, JsonNode itemData) {
        Map<String, Object> foodsDataMap = new HashMap<>();
        Map<String, Map<String, Object>> foodMatData = new HashMap<>();
        List<Map<String, Object>> foodData = new ArrayList<>();
        foodsDataMap.put("foodMatData", foodMatData); //食材信息表
        foodsDataMap.put("foodData", foodData); //食物信息表

        //获取食材信息
        Iterator<Map.Entry<String, JsonNode>> foodMatDataFields = foodMatDataNode.fields();

        while (foodMatDataFields.hasNext()) {
            Map<String, Object> foodMatInformation = new HashMap<>(); //每一个食材的详细信息
            Map.Entry<String, JsonNode> foodMat = foodMatDataFields.next();
            String foodMatId = foodMat.getKey();
            JsonNode foodMatItem = itemData.get(foodMatId);
            JsonNode foodMatValue = foodMat.getValue();

            //食物属性
            foodMatInformation.put("name", foodMatItem.get("itemName").asText());
            foodMatInformation.put("buffDesc", foodMatValue.hasNonNull("buffDesc") ? foodMatValue.get("buffDesc").asText() : "");
            foodMatInformation.put("obtainApproach", foodMatItem.get("obtainApproach").asText().replace("\n", "<br>"));

            Map<String, Map<String, Object>> foodMatData1 = (Map<String, Map<String, Object>>) foodsDataMap.get("foodMatData");
            foodMatData1.put(foodMatId, foodMatInformation);
        }

        // 获取食物信息
        Iterator<Map.Entry<String, JsonNode>> foodDataFields = foodDataNode.fields();

        while (foodDataFields.hasNext()) {
            Map<String, Object> foodInformation = new HashMap<>(); //每一个食物的详细信息
            Map.Entry<String, JsonNode> food = foodDataFields.next();
            String foodId = food.getKey();
            JsonNode foodItem = itemData.get(foodId);
            JsonNode foodValue = food.getValue();

            //食物编号
            foodInformation.put("id", foodId);

            //食物属性
            List<String> foodAttributes = new ArrayList<>();
            JsonNode attributes = foodValue.get("attributes");
            for (JsonNode attribute : attributes) {
                foodAttributes.add(attribute.asText());
            }
            foodInformation.put("attributes", foodAttributes);

            //食物配方
            JsonNode recipes = foodValue.get("recipes");
            List<Map<String, Object>> foodRecipes = new ArrayList<>();
            for (JsonNode recipe : recipes) {
                Map<String, Object> foodRecipe = new HashMap<>();
                foodRecipe.put("foodId", recipe.get("foodId").asText());
                JsonNode mats = recipe.get("mats");
                List<String> matList = new ArrayList<>();
                for (JsonNode mat : mats) {
                    matList.add(mat.asText());
                }
                foodRecipe.put("mats", matList);
                foodRecipes.add(foodRecipe);
            }
            foodInformation.put("recipes", foodRecipes);

            //食物变体
            JsonNode variants = foodValue.get("variants");
            List<Map<String, String>> foodVariants = new ArrayList<>();
            for (JsonNode variant : variants) {
                Map<String, String> foodVariant = new HashMap<>();
                foodVariant.put("name", variant.get("name").asText());
                foodVariant.put("usage", variant.get("usage").asText());
                foodVariants.add(foodVariant);
            }
            foodInformation.put("variants", foodVariants);

            //食物名称
            foodInformation.put("name", foodItem.get("itemName").asText());

            //食物用途（展示增强后的效果）
            foodInformation.put("usage", variants.get(variants.size() - 2).get("usage").asText());

            //使用调味料后持续时长（添加2个调味料获得增强效果，1个调味料延长5天）
            int duration = foodValue.get("duration").asInt();
            if (duration == -1) {
                if (!recipes.isEmpty()) {
                    foodInformation.put("duration", "持续时间无限");
                } else {
                    foodInformation.put("duration", "持续时间无限，无法使用调味料进行效果增强");
                }
            } else {
                foodInformation.put("duration", duration + 10);
            }

            List<Map<String, Object>> foodData1 = (List<Map<String, Object>>) foodsDataMap.get("foodData");
            foodData1.add(foodInformation);
        }
        return foodsDataMap;
    }
}
