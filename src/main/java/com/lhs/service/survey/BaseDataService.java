package com.lhs.service.survey;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.entity.survey.CharacterTable;
import com.lhs.mapper.CharacterTableMapper;
import org.springframework.stereotype.Service;


import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BaseDataService {

    private final CharacterTableMapper characterTableMapper;
    private final String githubBotResource = "E:\\Idea_Project\\Arknights-Bot-Resource\\";

    public BaseDataService(CharacterTableMapper characterTableMapper) {
        this.characterTableMapper = characterTableMapper;
    }

    public HashMap<String, Object> getCharacterData() {

        String character_tableText = FileUtil.read(githubBotResource + "gamedata\\excel\\character_table.json");
        String uniequip_tableText = FileUtil.read(githubBotResource + "gamedata\\excel\\uniequip_table.json");
        String skill_tableText = FileUtil.read(githubBotResource + "gamedata\\excel\\skill_table.json");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode characterTable = null;
        JsonNode uniequip_table = null;
        JsonNode skillTable = null;

        try {
            characterTable = objectMapper.readTree(character_tableText);
            uniequip_table = objectMapper.readTree(uniequip_tableText);
            skillTable = objectMapper.readTree(skill_tableText);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


        JsonNode equipDict = uniequip_table.get("equipDict");

        HashMap<String, HashMap<String, Boolean>> modTable = new HashMap<>();

        List<CharacterTable> characterTableList = characterTableMapper.selectList(null);

        Map<String, CharacterTable> characterTableMap = characterTableList.stream()
                .collect(Collectors.toMap(CharacterTable::getCharId, Function.identity()));

        Iterator<Map.Entry<String, JsonNode>> equipDictElements = equipDict.fields();
        
        while (equipDictElements.hasNext()){
            String equipId = equipDictElements.next().getKey();

            JsonNode equipInfo= equipDict.get(equipId);
            if(equipInfo==null) continue;
            if(equipInfo.get("typeName1")==null) continue;
            String typeName1 = equipInfo.get("typeName1").asText();
            if (!"ORIGINAL".equals(typeName1)) {
                String typeName2 = equipInfo.get("typeName2").asText();
                String charId = equipInfo.get("charId").asText();
                if (modTable.get(charId) != null) {
                    HashMap<String, Boolean> hashMap = modTable.get(charId);
                    hashMap.put("mod" + typeName2, true);
                    modTable.put(charId, hashMap);
                } else {
                    HashMap<String, Boolean> hashMap = new HashMap<>();
                    hashMap.put("mod" + typeName2, true);
                    modTable.put(charId, hashMap);
                }
            }
        }



        HashMap<Object, Object> skillTableMap = new HashMap<>();

        Iterator<Map.Entry<String, JsonNode>> skillTableElements = skillTable.fields();

        while (skillTableElements.hasNext()){
            String skillId = skillTableElements.next().getKey();
            String skillName = skillTable.get(skillId).get("levels").get(0).get("name").asText();
            skillTableMap.put(skillId,skillName);
        }

        HashMap<Object, Object> hashMap = new HashMap<>();


        Iterator<Map.Entry<String, JsonNode>> characterTableElements = characterTable.fields();


        while (characterTableElements.hasNext()){
            String charId = characterTableElements.next().getKey();
            if(charId.startsWith("char")){
                HashMap<Object, Object> character = new HashMap<>();
                JsonNode characterData = characterTable.get(charId);
               
                if (characterData.get("itemObtainApproach")==null|| Objects.equals(characterData.get("itemObtainApproach").asText(), "null")) continue;
                System.out.println(characterData.get("itemObtainApproach"));

                JsonNode skills = characterData.get("skills");
                List<HashMap<Object, Object>> skillList = new ArrayList<>();
                for (int i = 0; i < skills.size(); i++) {
                    JsonNode jsonNode = skills.get(i);
                    String skillId = jsonNode.get("skillId").asText();
                    skillId = skillId.replace("[", "_");
                    skillId = skillId.replace("]", "_");
                    HashMap<Object, Object> skill = new HashMap<>();
                    skill.put("iconId", skillId);
                    skill.put("name", skillTableMap.get(skillId));
                    skillList.add(skill);
                }

                String name = characterData.get("name").asText();

                String profession = characterData.get("profession").asText();

                CharacterTable characterTableSimple = characterTableMap.get(charId);

                if (characterTableMap.get(charId) == null) {
                    CharacterTable characterTableNew = new CharacterTable();
                    characterTableNew.setCharId(charId);
                    characterTableNew.setName(name);
                    characterTableNew.setUpdateTime(new Date());
                    characterTableMapper.insert(characterTableNew);
                }


                System.out.println(charId);
                System.out.println(characterTableSimple);

                int rarity = characterData.get("rarity").intValue() + 1;
                character.put("name", name);
                character.put("rarity", rarity);
                character.put("itemObtainApproach", characterTableSimple.getObtainApproach());
                character.put("mod", modTable.get(charId));
                character.put("skill", skillList);
                character.put("date", characterTableMap.get(charId).getUpdateTime());
                character.put("profession", profession);
                hashMap.put(charId, character);

            }
            
        }




        FileUtil.save(ApplicationConfig.Item, "character_table_simple.json", JSON.toJSONString(hashMap));
        FileUtil.save("E:\\VCProject\\frontend-v2-plus\\src\\static\\json\\survey\\", "character_table_simple.json", JSON.toJSONString(hashMap));


        return null;
    }


    public void getPortrait() {
        try {
            // 创建流对象

            String character_tableStr = FileUtil.read(githubBotResource + "gamedata\\excel\\character_table.json");

            JSONObject character_table = JSONObject.parseObject(character_tableStr);
            List<String> list = new ArrayList<>();

            String startPath = githubBotResource + "portrait\\";
            String portrait6 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\portrait-ori-6\\";
            String portrait5 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\portrait-ori-5\\";
            String portrait4 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\portrait-ori-4\\";
            String endPath = portrait4;
            for (String key : character_table.keySet()) {
                if (!key.startsWith("char")) continue;
                JSONObject charData = JSONObject.parseObject(character_table.getString(key));
                int rarity = Integer.parseInt(charData.getString("rarity"));
                System.out.println(key + "：星级：" + rarity + "，文件名：" + startPath + key + ".png");
                File source = new File(startPath + key + "_1.png");

                if (rarity == 5) endPath = portrait6;
                if (rarity == 4) endPath = portrait5;
                if (rarity < 4) endPath = portrait4;

                File tmpFile = new File(endPath);//获取文件夹路径

                if (!tmpFile.exists()) {//判断文件夹是否创建，没有创建则创建新文件夹
                    boolean mkdirs = tmpFile.mkdirs();
                }

                File dest = new File(endPath + key + "_1.png");
                copyFile(source, dest);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        File source = new File("E:\\VCProject\\frontend-v2-plus\\public\\image\\avg_npc_380_1.png");
        File dest = new File("E:\\VCProject\\frontend-v2-plus\\public\\image\\avg_npc_380_1_copy.png");
        copyFile(source, dest);
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


    public void getAvatar() {
        try {
            // 创建流对象

            String character_tableStr = FileUtil.read(githubBotResource + "gamedata\\excel\\character_table.json");

            JSONObject character_table = JSONObject.parseObject(character_tableStr);


            String startPath = githubBotResource + "avatar\\";
            String avatar6 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\avatar-ori-6\\";
            String avatar5 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\avatar-ori-5\\";
            String avatar4 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\avatar-ori-4\\";
            String endPath = avatar4;
            for (String key : character_table.keySet()) {
                if (!key.startsWith("char")) continue;
                JSONObject charData = JSONObject.parseObject(character_table.getString(key));
                int rarity = Integer.parseInt(charData.getString("rarity"));
                System.out.println(key + "：星级：" + rarity + "，文件名：" + startPath + key + ".png");
                File source = new File(startPath + key + ".png");

                if (rarity == 5) endPath = avatar6;
                if (rarity == 4) endPath = avatar5;
                if (rarity < 4) endPath = avatar4;

                File tmpFile = new File(endPath);//获取文件夹路径

                if (!tmpFile.exists()) {//判断文件夹是否创建，没有创建则创建新文件夹
                    boolean mkdirs = tmpFile.mkdirs();
                }

                File dest = new File(endPath + key + ".png");
                copyFile(source, dest);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
