package com.lhs.service.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.dto.maa.BuildingData;
import com.lhs.entity.po.survey.OperatorTable;
import com.lhs.mapper.survey.OperatorTableMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ArknightsGameDataService {

    private final OperatorTableMapper operatorTableMapper;
    private final static String githubBotResource = "C:/IDEAProject/Arknights-Bot-Resource/";

    private final static String GAME_DATA = "C:/IDEAProject/ArknightsGameData/zh_CN/gamedata/";

    private final RedisTemplate<String, Object> redisTemplate;


    public ArknightsGameDataService(OperatorTableMapper operatorTableMapper, RedisTemplate<String, Object> redisTemplate) {
        this.operatorTableMapper = operatorTableMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 返回一个集合 key:模组id,value:模组分支
     *
     * @return Map<模组id, 模组分支>
     */
    @RedisCacheable(key = "Survey:EquipIdAndType", timeout = 86400)
    public Map<String, String> getEquipIdAndType() {
        String read = FileUtil.read(ApplicationConfig.Item + "character_table_simple.json");
        if (read == null) throw new ServiceException(ResultCode.FILE_NOT_EXIST);
        JsonNode characterTableSimple = JsonMapper.parseJSONObject(read);
        Map<String, String> uniEquipIdAndType = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> characterTableSimpleFields = characterTableSimple.fields();
        while (characterTableSimpleFields.hasNext()) {
            String charId = characterTableSimpleFields.next().getKey();
            JsonNode operatorData = characterTableSimple.get(charId);
            if (operatorData.get("equip") == null) continue;
            JsonNode equip = operatorData.get("equip");
            for (JsonNode equipData : equip) {
                String uniEquipId = equipData.get("uniEquipId").asText();
                String typeName2 = equipData.get("typeName2").asText();
                uniEquipIdAndType.put(uniEquipId, typeName2);
            }
        }

        return uniEquipIdAndType;

    }

    /**
     * 返回一个集合 key:干员id_模组分支,value:模组分支
     *
     * @return Map<干员id_模组分支, 模组分支>
     */
    @RedisCacheable(key = "Survey:HasEquipTable", timeout = 86400)
    public Map<String, String> getHasEquipTable() {
        String read = FileUtil.read(ApplicationConfig.Item + "character_table_simple.json");
        JsonNode jsonNode = JsonMapper.parseJSONObject(read);
        Map<String, String> map = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> elements = jsonNode.fields();
        while (elements.hasNext()) {
            String charId = elements.next().getKey();
            JsonNode operator = jsonNode.get(charId);
            if (operator.get("mod") != null) {
                JsonNode mod = operator.get("mod");
                if (mod.get("modX") != null) {
                    map.put(charId + "_X", "X");
                }
                if (mod.get("modY") != null) {
                    map.put(charId + "_Y", "Y");
                }
            }
        }
        return map;
    }

    /**
     * 返回一个干员信息的集合 里面主要用到干员的获取方式和实装时间
     *
     * @return
     */
    @RedisCacheable(key = "Survey:OperatorUpdateTable", timeout = 3000)
    public List<OperatorTable> getOperatorTable() {
        List<OperatorTable> operatorTableList = operatorTableMapper.selectList(null);
        if (operatorTableList == null) throw new ServiceException(ResultCode.DATA_NONE);
        return operatorTableList;
    }


    public void getCharacterData() {
        String character_tableText = FileUtil.read(GAME_DATA + "excel/character_table.json");
        String uniequip_tableText = FileUtil.read(GAME_DATA + "excel/uniequip_table.json");
        String skill_tableText = FileUtil.read(GAME_DATA + "excel/skill_table.json");
        String char_patch_tableText = FileUtil.read(GAME_DATA + "excel/char_patch_table.json");

        JsonNode characterTable = JsonMapper.parseJSONObject(character_tableText);
        JsonNode uniequip_table = JsonMapper.parseJSONObject(uniequip_tableText);
        JsonNode skillTable = JsonMapper.parseJSONObject(skill_tableText);
        JsonNode charPatchTable = JsonMapper.parseJSONObject(char_patch_tableText);

        JsonNode equipDict = uniequip_table.get("equipDict");

        List<OperatorTable> operatorTable = getOperatorTable();

        Map<String, OperatorTable> characterTableMap = operatorTable.stream()
                .collect(Collectors.toMap(OperatorTable::getCharId, Function.identity()));


        Iterator<Map.Entry<String, JsonNode>> equipDictElements = equipDict.fields();
        Map<String, List<Map<String, Object>>> equipListMap = new HashMap<>();

        while (equipDictElements.hasNext()) {
            String equipId = equipDictElements.next().getKey();
            JsonNode equipData = equipDict.get(equipId);
            if (equipId.contains("_001_")) continue;
            if (equipData == null) continue;
            String charId = equipData.get("charId").asText();
            String typeName1 = equipData.get("typeName1").asText();
            String typeName2 = equipData.get("typeName2").asText();
            String uniEquipIcon = equipData.get("uniEquipIcon").asText();
            String typeIcon = equipData.get("typeIcon").asText().toLowerCase();
            String uniEquipName = equipData.get("uniEquipName").asText();

            if (equipListMap.get(charId) != null) {
                List<Map<String, Object>> maps = equipListMap.get(charId);
                Map<String, Object> tempMap = new HashMap<>();
                tempMap.put("charId", charId);
                tempMap.put("uniEquipId", equipId);
                tempMap.put("typeName1", typeName1);
                tempMap.put("typeName2", typeName2);
                tempMap.put("uniEquipIcon", uniEquipIcon);
                tempMap.put("typeIcon", typeIcon);
                tempMap.put("uniEquipName", uniEquipName);
                maps.add(tempMap);
                equipListMap.put(charId, maps);
            } else {
                List<Map<String, Object>> maps = new ArrayList<>();
                Map<String, Object> tempMap = new HashMap<>();
                tempMap.put("charId", charId);
                tempMap.put("uniEquipId", equipId);
                tempMap.put("typeName1", typeName1);
                tempMap.put("typeName2", typeName2);
                tempMap.put("uniEquipIcon", uniEquipIcon);
                tempMap.put("typeIcon", typeIcon);
                tempMap.put("uniEquipName", uniEquipName);
                maps.add(tempMap);
                equipListMap.put(charId, maps);
            }

        }

        HashMap<Object, String> skillTableMap = new HashMap<>();

        Iterator<Map.Entry<String, JsonNode>> skillTableElements = skillTable.fields();

        while (skillTableElements.hasNext()) {
            String skillId = skillTableElements.next().getKey();
            String iconId = skillTable.get(skillId).get("iconId").asText();
            String skillName = skillTable.get(skillId).get("levels").get(0).get("name").asText();
            skillTableMap.put(skillId, skillName);

            skillTableMap.put(skillId + "icon", iconId);
            if (Objects.equals(iconId, "null")) {
                skillTableMap.put(skillId + "icon", skillId);
            }
        }

        Map<String, Object> table_simple = new HashMap<>();


        //升变干员
        JsonNode patchChars = charPatchTable.get("patchChars");
        Iterator<Map.Entry<String, JsonNode>> patchCharsFields = patchChars.fields();

        while (patchCharsFields.hasNext()) {
            String charId = patchCharsFields.next().getKey();
            JsonNode characterData = patchChars.get(charId);
            Map<Object, Object> character = new HashMap<>();

            List<HashMap<Object, Object>> skillList = new ArrayList<>();
            JsonNode skills = characterData.get("skills");
            for (int i = 0; i < skills.size(); i++) {
                JsonNode jsonNode = skills.get(i);
                String skillId = jsonNode.get("skillId").asText();
                String iconId = skillTableMap.get(skillId + "icon");
                iconId = iconId.replace("[", "_");
                iconId = iconId.replace("]", "_");
                HashMap<Object, Object> skill = new HashMap<>();
                skill.put("iconId", iconId);
                skill.put("name", skillTableMap.get(skillId));
                skillList.add(skill);
            }

            String name = characterData.get("name").asText();

            String profession = characterData.get("profession").asText();
            int rarity = getRarity(characterData.get("rarity").asText());
            String subProfessionId = characterData.get("subProfessionId").asText();

            OperatorTable operatorTableSimple = characterTableMap.get(charId);

            character.put("name", name);
            character.put("charId", charId);
            character.put("rarity", rarity);
            character.put("itemObtainApproach", operatorTableSimple.getObtainApproach());
            character.put("equip", equipListMap.get(charId));
            character.put("skill", skillList);
            character.put("date", characterTableMap.get(charId).getUpdateTime().getTime());
            character.put("profession", profession);
            character.put("subProfessionId", subProfessionId);
            character.put("own", false);
            character.put("level", 0);
            character.put("elite", 0);
            character.put("potential", 0);
            character.put("mainSkill", 0);
            character.put("skill1", 0);
            character.put("skill2", 0);
            character.put("skill3", 0);
            character.put("modX", 0);
            character.put("modY", 0);
            character.put("modD", 0);
            character.put("show", rarity == 6);
            table_simple.put(charId, character);
        }

        Iterator<Map.Entry<String, JsonNode>> characterTableElements = characterTable.fields();

        while (characterTableElements.hasNext()) {
            String charId = characterTableElements.next().getKey();
            if (charId.startsWith("char")) {
                Map<Object, Object> character = new HashMap<>();
                JsonNode characterData = characterTable.get(charId);

                if (characterData.get("itemObtainApproach") == null || Objects.equals(characterData.get("itemObtainApproach").asText(), "null"))
                    continue;
//                System.out.println(characterData.get("itemObtainApproach"));

                JsonNode skills = characterData.get("skills");
                List<HashMap<Object, Object>> skillList = new ArrayList<>();
                for (int i = 0; i < skills.size(); i++) {
                    JsonNode jsonNode = skills.get(i);
                    String skillId = jsonNode.get("skillId").asText();
                    String iconId = skillTableMap.get(skillId + "icon");
                    iconId = iconId.replace("[", "_");
                    iconId = iconId.replace("]", "_");
                    HashMap<Object, Object> skill = new HashMap<>();
                    skill.put("iconId", iconId);
                    skill.put("name", skillTableMap.get(skillId));
                    skillList.add(skill);
                }

                String name = characterData.get("name").asText();

                String profession = characterData.get("profession").asText();
                int rarity = getRarity(characterData.get("rarity").asText());
                ;
                String subProfessionId = characterData.get("subProfessionId").asText();

                OperatorTable operatorTableSimple = characterTableMap.get(charId);
                String itemObtainApproach = "常驻干员";
                long updateTime = System.currentTimeMillis();
                if (operatorTableSimple != null) {
                    itemObtainApproach = operatorTableSimple.getObtainApproach();
                    updateTime = operatorTableSimple.getUpdateTime().getTime();
                }


                if (characterTableMap.get(charId) == null) {
                    OperatorTable operatorTableNew = new OperatorTable();
                    operatorTableNew.setCharId(charId);
                    operatorTableNew.setName(name);
                    operatorTableNew.setRarity(rarity);
                    operatorTableNew.setUpdateTime(new Date());
                    operatorTableNew.setObtainApproach("常驻干员");
                    operatorTableMapper.insert(operatorTableNew);
                }


                character.put("name", name);
                character.put("charId", charId);
                character.put("rarity", rarity);
                character.put("itemObtainApproach", itemObtainApproach);
                character.put("equip", equipListMap.get(charId));
                character.put("skill", skillList);
                character.put("date", updateTime);
                character.put("profession", profession);
                character.put("subProfessionId", subProfessionId);
                character.put("own", false);
                character.put("level", 0);
                character.put("elite", 0);
                character.put("potential", 0);
                character.put("mainSkill", 0);
                character.put("skill1", 0);
                character.put("skill2", 0);
                character.put("skill3", 0);
                character.put("modX", 0);
                character.put("modY", 0);
                character.put("modD", 0);
                character.put("show", rarity == 6);
//                System.out.println(name + "{ }" + operatorTableSimple.getUpdateTime() + "{ }" + operatorTableSimple.getUpdateTime().getTime());

                table_simple.put(charId, character);
            }
        }

        List<Object> list = new ArrayList<>();
        for (String id : table_simple.keySet()) {
            list.add(table_simple.get(id));
        }

        FileUtil.save(ApplicationConfig.Item, "character_table_simple.json", JsonMapper.toJSONString(table_simple));
        FileUtil.save("C:\\VCProject\\frontend-v2-plus\\src\\static\\json\\survey\\", "character_table_simple.json", JsonMapper.toJSONString(table_simple));

        FileUtil.save("C:\\VCProject\\frontend-v2-plus\\src\\static\\json\\survey\\", "character_list.json", JsonMapper.toJSONString(list));
    }

    private Integer getRarity(String str) {
        return Integer.parseInt(str.replace("TIER_", ""));
    }

    public HashMap<String, Object> getOperatorApCost() {
        String character_tableText = FileUtil.read(GAME_DATA + "excel/character_table.json");
        String uniequip_tableText = FileUtil.read(GAME_DATA + "excel/uniequip_table.json");
        String skill_tableText = FileUtil.read(GAME_DATA + "excel/skill_table.json");

        JsonNode characterTable = JsonMapper.parseJSONObject(character_tableText);
        JsonNode equipTable = JsonMapper.parseJSONObject(uniequip_tableText);
        JsonNode skillTable = JsonMapper.parseJSONObject(skill_tableText);

        HashMap<Object, String> skillTableMap = new HashMap<>();

        Iterator<Map.Entry<String, JsonNode>> skillTableElements = skillTable.fields();

        while (skillTableElements.hasNext()) {
            String skillId = skillTableElements.next().getKey();
            String iconId = skillTable.get(skillId).get("iconId").asText();
            String skillName = skillTable.get(skillId).get("levels").get(0).get("name").asText();
            skillTableMap.put(skillId, skillName);

            skillTableMap.put(skillId + "icon", iconId);
            if (Objects.equals(iconId, "null")) {
                skillTableMap.put(skillId + "icon", skillId);
            }
        }

        Iterator<Map.Entry<String, JsonNode>> characterTableElements = characterTable.fields();


        JsonNode equipDict = equipTable.get("equipDict");
        Iterator<Map.Entry<String, JsonNode>> equipDictElement = equipDict.fields();

        Map<Object, Object> equipDataMap = new HashMap<>();
        while (equipDictElement.hasNext()) {
            String key = equipDictElement.next().getKey();
            JsonNode jsonNode = equipDict.get(key);
            if (key.contains("_001")) continue;
            JsonNode itemCost = jsonNode.get("itemCost");
            String charId = jsonNode.get("charId").asText();
            String typeName2 = jsonNode.get("typeName2").asText();
            List<Map<String, Integer>> itemCostMapList = new ArrayList<>();
            for (JsonNode rank : itemCost) {
//                System.out.println(rank);
                Map<String, Integer> itemCostMap = new HashMap<>();
                for (JsonNode cost : rank) {
//                    System.out.println(cost);
                    String id = cost.get("id").asText();
                    int count = cost.get("count").intValue();
                    itemCostMap.put(id, count);
                }
                itemCostMapList.add(itemCostMap);
            }
//            System.out.println(charId+"."+typeName2+"---"+itemCostMapList);
            equipDataMap.put(charId + "." + typeName2, itemCostMapList);
        }

        HashMap<String, Object> operatorMap = new HashMap<>();
        while (characterTableElements.hasNext()) {
            String charId = characterTableElements.next().getKey();
            if (charId.startsWith("char")) {
                HashMap<String, Object> operator = new HashMap<>();
                JsonNode characterData = characterTable.get(charId);
                if (characterData.get("itemObtainApproach") == null || Objects.equals(characterData.get("itemObtainApproach").asText(), "null"))
                    continue;

                JsonNode phases = characterData.get("phases");
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

                JsonNode allSkillLvlup = characterData.get("allSkillLvlup");
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

                JsonNode skills = characterData.get("skills");
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
                if (equipDataMap.get(charId + "." + "X") != null) {
                    System.out.println(equipDataMap.get(charId + "." + "X"));
                    operator.put("modX", equipDataMap.get(charId + "." + "X"));
                }
                if (equipDataMap.get(charId + "." + "Y") != null) {
                    operator.put("modY", equipDataMap.get(charId + "." + "Y"));
                }
                operatorMap.put(charId, operator);
            }


        }

//        FileUtil.save(ApplicationConfig.Item, "operator_item_cost_table.json", JsonMapper.toJSONString(operatorMap));
        FileUtil.save("C:\\VCProject\\frontend-v2-plus\\src\\static\\json\\survey\\", "operator_item_cost_table.json", JsonMapper.toJSONString(operatorMap));

        return null;
    }


    public void getBuildingTable() {
        String read = FileUtil.read(GAME_DATA + "excel/building_data.json");
        String read1 = FileUtil.read(GAME_DATA + "excel/character_table.json");


        List<OperatorTable> operatorTable = getOperatorTable();

        Map<String, OperatorTable> characterTableMap = operatorTable.stream()
                .collect(Collectors.toMap(OperatorTable::getCharId, Function.identity()));

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
                    if (name.equals("假日威龙陈")) {
                        System.out.println(description);
                    }
                    buildingData.setTimestamp(characterTableMap.get(charId).getUpdateTime().getTime());
                    buildingData.setDescription(replaceDescription(description));
                    buildingData.setRoomType(roomType);
                    buildingData.setName(name);
                    buildingDataList.add(buildingData);

                }
            }
        }


//        Map<String, List<BuildingData>> collect = buildingDataList.stream()
//                .collect(Collectors.groupingBy(BuildingData::getRoomType));
         buildingDataList.sort(Comparator.comparing(BuildingData::getTimestamp).reversed());
        FileUtil.save("C:/VCProject/frontend-v2-plus/src/static/json/build/", "building_table.json", JsonMapper.toJSONString(buildingDataList));

    }

    private Integer getPhase(String text) {
        return Integer.parseInt(text.replace("PHASE_", ""));
    }


    public void getPortrait() {
        try {
            // 创建流对象

            String character_tableStr = FileUtil.read(GAME_DATA + "excel/character_table.json");
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode characterTable = objectMapper.readTree(character_tableStr);

            String startPath = githubBotResource + "portrait\\";
            String portrait6 = "C:\\VCProject\\resources\\portrait-ori-6\\";
            String portrait5 = "C:\\VCProject\\resources\\portrait-ori-5\\";
            String portrait4 = "C:\\VCProject\\resources\\portrait-ori-4\\";
            String endPath = portrait4;

            Iterator<Map.Entry<String, JsonNode>> fields = characterTable.fields();

            while (fields.hasNext()) {
                String charId = fields.next().getKey();
                if (!charId.startsWith("char")) continue;
                JsonNode charData = characterTable.get(charId);
                int rarity = getRarity(charData.get("rarity").asText());
                System.out.println(charId + "：星级：" + rarity + "，文件名：" + startPath + charId + ".png  到 " + endPath + charId + ".png");
                File source = new File(startPath + charId + "_1.png");

                if (rarity == 6) endPath = portrait6;
                if (rarity == 5) endPath = portrait5;
                if (rarity < 5) endPath = portrait4;

                File tmpFile = new File(endPath);//获取文件夹路径

                if (!tmpFile.exists()) {//判断文件夹是否创建，没有创建则创建新文件夹
                    boolean mkdirs = tmpFile.mkdirs();
                }

                File dest = new File(endPath + charId + "_1.png");
                copyFile(source, dest);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void getAvatar() {
        try {
            // 创建流对象

            String character_tableStr = FileUtil.read(GAME_DATA + "excel/character_table.json");
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode characterTable = objectMapper.readTree(character_tableStr);


            String startPath = githubBotResource + "avatar\\";
            String avatar6 = "C:\\VCProject\\resources\\avatar-ori-6\\";
            String avatar5 = "C:\\VCProject\\resources\\avatar-ori-5\\";
            String avatar4 = "C:\\VCProject\\resources\\avatar-ori-4\\";
            String endPath = avatar4;

            Iterator<Map.Entry<String, JsonNode>> fields = characterTable.fields();

            while (fields.hasNext()) {
                String charId = fields.next().getKey();
                if (!charId.startsWith("char")) continue;
                JsonNode charData = characterTable.get(charId);
                int rarity = getRarity(charData.get("rarity").asText());
                System.out.println(charId + "：星级：" + rarity + "，文件名：" + startPath + charId + ".png  到 " + endPath + charId + ".png");
                File source = new File(startPath + charId + ".png");

                if (rarity == 6) endPath = avatar6;
                if (rarity == 5) endPath = avatar5;
                if (rarity < 5) endPath = avatar4;

                File tmpFile = new File(endPath);//获取文件夹路径

                if (!tmpFile.exists()) {//判断文件夹是否创建，没有创建则创建新文件夹
                    boolean mkdirs = tmpFile.mkdirs();
                }

                File dest = new File(endPath + charId + ".png");
                copyFile(source, dest);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(File source, File dest) {

        try {
            FileInputStream is = new FileInputStream(source);
            FileOutputStream os = new FileOutputStream(dest);
            FileChannel sourceChannel = is.getChannel();
            FileChannel destChannel = os.getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), destChannel);
            sourceChannel.close();
            destChannel.close();
            is.close();
            os.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private String replaceDescription(String str) {
        Map<String, String> classMap = new HashMap<String, String>();

        Map<String, String> spliceClassMap = new HashMap<>();
        spliceClassMap.put("<@cc.vup>", "<span class='cc-vup'>");
        spliceClassMap.put("<@cc.kw>", "<span class='cc-kw'>");
        spliceClassMap.put("<@cc.vdown>", "<span class='cc-vdown'>");
        spliceClassMap.put("<@cc.rem>", "<span class='cc-rem'>");
        spliceClassMap.put("</>", "</span>");

        classMap.put("<$cc.tag.durin>", "<span class='cc-tag-durin'>");
        classMap.put("<$cc.tag.op>", "<span class='cc-tag-op'>"); //作业平台
        classMap.put("<$cc.tag.mh>", "<span class='cc-tag-mh'>");
        classMap.put("<$cc.tag.knight>", "<span class='cc-tag-knight'>"); //骑士

        classMap.put("<$cc.bd_b1>", "<span class='cc-bd-b1'>"); //人间烟火
        classMap.put("<$cc.bd_a1>", "<span class='cc-bd-a1'>"); //感知信息
        classMap.put("<$cc.bd_A>", "<span class='cc-bd-A'>"); //感知信息
        classMap.put("<$cc.bd_C>", "<span class='cc-bd-C'>"); //巫术结晶
        classMap.put("<$cc.bd_B>", "<span class='cc-bd-B'>"); //人间烟火
        classMap.put("<$cc.bd_malist>", "<span class='cc-bd-malist'>"); //工程机器人
        classMap.put("<$cc.bd_a1_a1>", "<span class='cc-bd-a1-a1'>");  //记忆碎片
        classMap.put("<$cc.bd_a1_a2>", "<span class='cc-bd-a1-a2'>");  //梦境
        classMap.put("<$cc.bd_a1_a3>", "<span class='cc-bd-a1-a3'>");  //小节
        classMap.put("<$cc.bd_ash>", "<span class='cc-bd-ash'>"); //彩六
        classMap.put("<$cc.bd_tachanka>", "<span class='cc-bd-tachanka'>");  //彩六
        classMap.put("<$cc.bd_felyne>", "<span class='cc-bd-felyne'>");  //彩六


        classMap.put("<$cc.t.strong2>", "<span class='cc-t-strong2'>"); //强调
        classMap.put("<$cc.t.flow_gold>", "<span class='cc-t-flow-gold'>");//赤金线

        classMap.put("<$cc.c.room3>", "<span class='cc-c-room3'>");
        classMap.put("<$cc.c.room1>", "<span class='cc-c-room1'>");
        classMap.put("<$cc.c.room2>", "<span class='cc-c-room2'>");
        classMap.put("<$cc.c.skill>", "<span class='cc-c-skill'>");
        classMap.put("<$cc.c.abyssal2_3>", "<span class='cc-c-abyssal2_3'>");
        classMap.put("<$cc.c.abyssal2_1>", "<span class='cc-c-abyssal2_1'>"); //深海猎人
        classMap.put("<$cc.c.abyssal2_2>", "<span class='cc-c-abyssal2_2'>"); //深海猎人
        classMap.put("<$cc.c.sui2_1>", "<span class='cc-c-sui2_1'>"); //深海猎人
        classMap.put("<$cc.c.sui2_2>", "<span class='cc-c-sui2_2'>"); //深海猎人

        classMap.put("<$cc.m.var1>", "<span class='cc-m-var1'>");


        classMap.put("<$cc.sk.manu1>", "<span class='cc-sk-manu1'>"); //标准化类技能
        classMap.put("<$cc.sk.manu2>", "<span class='cc-sk-manu2'>"); //莱茵科技类技能
        classMap.put("<$cc.sk.manu3>", "<span class='cc-sk-manu3'>"); //红松骑士团类
        classMap.put("<$cc.sk.manu4>", "<span class='cc-sk-manu4'>"); //金属工艺类技能


        classMap.put("<$cc.w.ncdeer1>", "<span class='cc-bd-ncdeer1'>");  //九色鹿
        classMap.put("<$cc.w.ncdeer2>", "<span class='cc-bd-ncdeer2'>");  //九色鹿

        classMap.put("<$cc.g.glasgow>", "<span class='cc-g-glasgow'>"); //格拉斯哥帮
        classMap.put("<$cc.g.bs>", "<span class='cc-g-bs'>"); //黑钢
        classMap.put("<$cc.g.sm>", "<span class='cc-g-sm'>"); //萨米
        classMap.put("<$cc.g.lgd>", "<span class='cc-g-lgd'>"); //近卫局
        classMap.put("<$cc.g.lda>", "<span class='cc-g-lda'>"); //鲤氏
        classMap.put("<$cc.g.sp>", "<span class='cc-g-sp'>"); //异格
        classMap.put("<$cc.g.R6>", "<span class='cc-g-R6'>"); //彩六
        classMap.put("<$cc.g.ussg>", "<span class='cc-g-ussg'>"); //彩六
        classMap.put("<$cc.g.abyssal>", "<span class='cc-g-abyssal'>"); //深海猎人
        classMap.put("<$cc.g.knight>", "<span class='cc-g-lda'>"); //骑士
        classMap.put("<$cc.g.mh>", "<span class='cc-g-mh'>"); //怪物猎人
        classMap.put("<$cc.g.op>", "<span class='cc-g-op'>"); //异格
        classMap.put("<$cc.g.rh>", "<span class='cc-g-rh'>"); //彩六
        classMap.put("<$cc.g.psk>", "<span class='cc-g-psk'>"); //怪物猎人
        classMap.put("<$cc.g.karlan>", "<span class='cc-g-karlan'>"); //喀兰
        classMap.put("<$cc.g.sui>", "<span class='cc-g-sui'>"); //岁

        classMap.put("<$cc.gvial>", "<span class='cc-gvial'>"); //彩六
        classMap.put("<$cc.t.accmuguard1>","<span class='cc-t-accmuguard1'>");


        classMap.put("<$cc.bd.costdrop>", "<span class='cc-bd-costdrop'>"); //心情落差


        for (String key : spliceClassMap.keySet()) {
            str = str.replace(key, spliceClassMap.get(key));
        }

//        for (String key : classMap.keySet()) {
//            str = str.replace(key, "<span class='cc-base'>");
//        }

        String pattern = "<\\$cc[^>]+>";
        String replacement = "<span class='cc-base'>";
        Pattern compile = Pattern.compile(pattern);
        Matcher matcher = compile.matcher(str);
        str = matcher.replaceAll(replacement);
        System.out.println(str);

        return str;
    }
}
