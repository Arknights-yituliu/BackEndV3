package com.lhs.service.util.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.dto.hypergryph.GameDataFormatFilePath;
import com.lhs.service.util.ArknightsGameDataV2Service;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ArknightsGameDataV2ServiceImpl implements ArknightsGameDataV2Service {

    @Override
    public void getOperatorInfoSimpleTableV2(GameDataFormatFilePath gameDataFormatFilePath) {
        //干员信息
        String character_tableText = FileUtil.read(gameDataFormatFilePath.getArknightsGameResourcePath() + "excel/character_table.json");
        JsonNode characterTable = JsonMapper.parseJSONObject(character_tableText);
        Iterator<Map.Entry<String, JsonNode>> characterTableFields = characterTable.fields();

        //生变干员信息
        String char_patch_tableText = FileUtil.read(gameDataFormatFilePath.getArknightsGameResourcePath() + "excel/char_patch_table.json");
        JsonNode charPatchTable = JsonMapper.parseJSONObject(char_patch_tableText);
        JsonNode patchChars = charPatchTable.get("patchChars");

        Iterator<Map.Entry<String, JsonNode>> patchCharsFields = patchChars.fields();


        Map<String, Object> operatorInfoMap = new HashMap<>();

        Map<String, Map<String, String>> skillNameAndIcon = getSkillNameAndIcon(gameDataFormatFilePath.getArknightsGameResourcePath());
        Map<String, List<Map<String, Object>>> equipInfoMap = getEquipInfoMap(gameDataFormatFilePath.getArknightsGameResourcePath());


        while (characterTableFields.hasNext()) {
            String charId = characterTableFields.next().getKey();
            if (!charId.startsWith("char")) {
                continue;
            }
            JsonNode operatorInfoNode = characterTable.get(charId);

            //获取方式为空跳过
            if (operatorInfoNode.get("itemObtainApproach") == null ||
                    Objects.equals(operatorInfoNode.get("itemObtainApproach").asText(), "null")) {
                continue;
            }
            Map<String, Object> operatorInfo = createOperatorInfo(charId, operatorInfoNode, skillNameAndIcon, equipInfoMap, 0);
            operatorInfoMap.put(charId, operatorInfo);
        }


        while (patchCharsFields.hasNext()) {
            String charId = patchCharsFields.next().getKey();
            if (!charId.startsWith("char")) {
                continue;
            }
            JsonNode operatorInfoNode = patchChars.get(charId);

            //获取方式为空跳过
            if (operatorInfoNode.get("itemObtainApproach") == null ||
                    Objects.equals(operatorInfoNode.get("itemObtainApproach").asText(), "null")) {
                continue;
            }
            Map<String, Object> operatorInfo = createOperatorInfo(charId, operatorInfoNode, skillNameAndIcon, equipInfoMap, 0);
            operatorInfoMap.put(charId, operatorInfo);
        }

        FileUtil.saveJsonFile(gameDataFormatFilePath.getJsonOutputPath() + "src/static/json/operator/",
                "character_table_simple.v2.json", JsonMapper.toJSONString(operatorInfoMap));

    }


    private Map<String, Object> createOperatorInfo(String charId,
                                                   JsonNode operatorInfoNode,
                                                   Map<String, Map<String, String>> skillNameAndIcon,
                                                   Map<String, List<Map<String, Object>>> equipMap,
                                                   Integer index) {
        Map<String, Object> operatorInfo = new HashMap<>();
        //获取干员名称
        String name = operatorInfoNode.get("name").asText();
        if ("char_1001_amiya2".equals(charId)) {
            name = "阿米娅（近卫）";
        }
        if ("char_1037_amiya3".equals(charId)) {
            name = "阿米娅（医疗）";
        }

        //获取干员职业
        String profession = operatorInfoNode.get("profession").asText();
        //获取干员星级
        int rarity = getRarity(operatorInfoNode.get("rarity").asText()) + index;
        //获取干员分支
        String subProfessionId = operatorInfoNode.get("subProfessionId").asText();

        //获取技能专精相关数据
        Object skillInfo = getSkillInfo(operatorInfoNode, skillNameAndIcon);

        //获取通用技能相关数据
        Object allSkillInfo = getAllSkillInfo(operatorInfoNode);

        Object eliteInfo = getEliteInfo(operatorInfoNode);


        operatorInfo.put("name", name);
        operatorInfo.put("charId", charId);
        operatorInfo.put("rarity", rarity);
        operatorInfo.put("elite",eliteInfo);
        operatorInfo.put("equip", equipMap.get(charId));
        operatorInfo.put("skills", skillInfo);
        operatorInfo.put("allSkill", allSkillInfo);
        operatorInfo.put("profession", profession);
        operatorInfo.put("subProfessionId", subProfessionId);


        return operatorInfo;
    }

    private Object getAllSkillInfo(JsonNode operatorInfoNode) {
        JsonNode allSkillLvlup = operatorInfoNode.get("allSkillLvlup");
        List<Object> allSkill = new ArrayList<>();
        for (JsonNode nodeElement : allSkillLvlup) {
            JsonNode lvlUpCost = nodeElement.get("lvlUpCost");
            Map<String, Integer> itemCostMap = new HashMap<>();
            for (JsonNode cost : lvlUpCost) {
                String id = cost.get("id").asText();
                int count = cost.get("count").intValue();
                itemCostMap.put(id, count);
            }
            allSkill.add(itemCostMap);
        }
        return allSkill;
    }

    private Object getEliteInfo(JsonNode operatorInfoNode) {
        JsonNode phases = operatorInfoNode.get("phases");
        List<HashMap<String, Integer>> elite = new ArrayList<>();
        for (JsonNode phase : phases) {
            if (phase.get("evolveCost") != null) {
                JsonNode evolveCost = phase.get("evolveCost");
                HashMap<String, Integer> itemCostMap = new HashMap<>();
                for (JsonNode cost : evolveCost) {
                    String id = cost.get("id").asText();
                    int count = cost.get("count").intValue();
                    itemCostMap.put(id, count);
                }
                elite.add(itemCostMap);
            }
        }
        return elite;
    }


    private Object getSkillInfo(JsonNode operatorInfoNode, Map<String, Map<String, String>> skillNameAndIcon) {
        JsonNode skillsNode = operatorInfoNode.get("skills");
        List<Object> skillInfo = new ArrayList<>();
        for (int i = 0; i < skillsNode.size(); i++) {
            JsonNode jsonNode = skillsNode.get(i);
            //获取技能id
            String skillId = jsonNode.get("skillId").asText();
            //获取技能图标
            Map<String, String> nameAndIcon = skillNameAndIcon.get(skillId);
            //将部分特殊符号替换
            HashMap<Object, Object> skill = new HashMap<>();
            //保存技能的图标和名称
            JsonNode levelUpCostCondNode = jsonNode.get("levelUpCostCond");
            List<Object> skillLevelUpCost = new ArrayList<>();
            for (JsonNode levelUpCostCondNodeElement : levelUpCostCondNode) {
                JsonNode levelUpCostNode = levelUpCostCondNodeElement.get("levelUpCost");
                List<Map<String, Object>> levelUpCost = new ArrayList<>();
                for (JsonNode levelUpCostNodeElement : levelUpCostNode) {
                    String itemId = levelUpCostNodeElement.get("id").asText();
                    int itemCount = levelUpCostNodeElement.get("count").asInt();
                    levelUpCost.add(Map.of("id", itemId, "count", itemCount));
                }
                skillLevelUpCost.add(levelUpCost);
            }

            skill.put("skillId", skillId);
            skill.put("skillName", nameAndIcon.get("name"));
            skill.put("skillIcon", nameAndIcon.get("icon"));
            skill.put("skillLevelUpCost", skillLevelUpCost);
            skillInfo.add(skill);
        }

        return skillInfo;
    }


    private Map<String, Map<String, String>> getSkillNameAndIcon(String filePath) {
        String skill_tableText = FileUtil.read(filePath + "excel/skill_table.json");
        JsonNode skillTable = JsonMapper.parseJSONObject(skill_tableText);
        Map<String, Map<String, String>> skillMap = new HashMap<>();
        //将技能数据转为一个可迭代的键值对
        Iterator<Map.Entry<String, JsonNode>> skillTableElements = skillTable.fields();
        while (skillTableElements.hasNext()) {
            String skillId = skillTableElements.next().getKey();
            JsonNode skillData = skillTable.get(skillId);

            if (skillData == null) continue;

            // 减少重复的get()调用
            String iconId = getSafeText(skillData, "iconId", skillId);
            String skillName = getSkillName(skillData);

            // 处理图标ID
            iconId = processIconId(iconId);

            // 构建技能信息映射
            Map<String, String> skillNameAndIcon = Map.of(
                    "name", skillName,
                    "icon", iconId
            );

            skillMap.put(skillId, skillNameAndIcon);

        }

        return skillMap;
    }

    // 辅助方法：安全获取文本字段
    private String getSafeText(JsonNode node, String fieldName, String defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        String text = fieldNode.asText();
        return "null".equals(text) ? defaultValue : text;
    }

    // 辅助方法：获取技能名称
    private String getSkillName(JsonNode skillData) {
        JsonNode levels = skillData.get("levels");
        if (levels != null && levels.isArray() && !levels.isEmpty()) {
            JsonNode firstLevel = levels.get(0);
            if (firstLevel != null) {
                JsonNode nameNode = firstLevel.get("name");
                if (nameNode != null) {
                    return nameNode.asText();
                }
            }
        }
        return ""; // 或者返回默认值
    }

    // 辅助方法：处理图标ID
    private String processIconId(String iconId) {
        return iconId.replace("[", "x5b").replace("]", "x5d");
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
            if (!Objects.equals(equipData.get("tmplId").asText(), "null")) {

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
        tempMap.put("typeName1", equipData.get("typeName1").asText());
        tempMap.put("typeName2", equipData.get("typeName2").asText());
        tempMap.put("uniEquipIcon", equipData.get("uniEquipIcon").asText());
        tempMap.put("typeIcon", equipData.get("typeIcon").asText().toLowerCase());
        tempMap.put("uniEquipName", equipData.get("uniEquipName").asText());
        tempMap.put("itemCost", parseItemCost(equipData.get("itemCost")));

        return tempMap;
    }


    // 提取方法：解析物品消耗
    private List<Map<String, Object>> parseItemCost(JsonNode itemCost) {


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


    private Integer getPhase(String text) {
        return Integer.parseInt(text.replace("PHASE_", ""));
    }

    private Integer getRarity(String str) {
        return Integer.parseInt(str.replace("TIER_", ""));
    }

}
