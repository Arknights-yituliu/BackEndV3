package com.lhs.service.survey;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.entity.survey.CharacterTable;
import com.lhs.mapper.CharacterTableMapper;
import org.springframework.stereotype.Service;


import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BaseDataService {

    private final CharacterTableMapper characterTableMapper;
    private final String githubBotResource = "E:\\Idea_Project\\Arknights-Bot-Resource\\";

    public BaseDataService(CharacterTableMapper characterTableMapper) {
        this.characterTableMapper = characterTableMapper;
    }

    public HashMap<String, Object> getCharacterData() {

        String character_tableStr = FileUtil.read(githubBotResource+"gamedata\\excel\\character_table.json");
        String uniequip_tableStr = FileUtil.read(githubBotResource+"gamedata\\excel\\uniequip_table.json");
        String skill_tableStr = FileUtil.read(githubBotResource+"gamedata\\excel\\skill_table.json");

        JSONObject character_table = JSONObject.parseObject(character_tableStr);
        JSONObject uniequip_table = JSONObject.parseObject(uniequip_tableStr);
        JSONObject equipDict = JSONObject.parseObject(uniequip_table.getString("equipDict"));
        JSONObject skill_table = JSONObject.parseObject(skill_tableStr);
        HashMap<String, HashMap<String, Boolean>> modTable = new HashMap<>();

        //干员的自定义id
        List<CharacterTable> characterTableList = characterTableMapper.selectList(null);
        //干员的自定义id
        Map<String, Date> charIdAndId = characterTableList.stream()
                .collect(Collectors.toMap(CharacterTable::getCharId, CharacterTable::getUpdateTime));

        equipDict.forEach((k, v) -> {
            JSONObject modJson = JSONObject.parseObject(String.valueOf(v));
            String typeName1 = modJson.getString("typeName1");
            if (!typeName1.equals("ORIGINAL")) {
                String typeName2 = modJson.getString("typeName2");

                String charId = modJson.getString("charId");
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
        });


        HashMap<Object, Object> skillTable = new HashMap<>();

        skill_table.forEach((k,v)->{
            Object level = JSONArray.parseArray(JSONObject.parseObject(String.valueOf(v)).getString("levels")).get(0);
            JSONObject jsonObject = JSONObject.parseObject(String.valueOf(level));
            Object name = jsonObject.get("name");
            skillTable.put(k,name);
        });

        HashMap<Object, Object> hashMap = new HashMap<>();

        for(String charId : character_table.keySet()) {

            if (charId.startsWith("char")) {
                HashMap<Object, Object> character = new HashMap<>();
                String charInfo = character_table.getString(charId);
                JSONObject characterJson = JSONObject.parseObject(charInfo);
                if (characterJson.get("itemObtainApproach") == null) continue;

                JSONArray skills = JSONArray.parseArray(characterJson.getString("skills"));
                List<HashMap<Object, Object>> skillList = new ArrayList<>();
                for (int i = 0; i < skills.size(); i++) {
                    JSONObject jsonObject = JSONObject.parseObject(String.valueOf(skills.get(i)));
                    String skillId = jsonObject.getString("skillId");
                    skillId = skillId.replace("[", "_");
                    skillId = skillId.replace("]", "_");
                    HashMap<Object, Object> skill = new HashMap<>();
                    skill.put("iconId", skillId);
                    skill.put("name", skillTable.get(skillId));
                    skillList.add(skill);
                }

                String name = characterJson.getString("name");
                String profession = characterJson.getString("profession");
                String itemUsage = characterJson.getString("itemUsage");
                String itemDesc = characterJson.getString("itemDesc");

                int itemObtainApproach = 1;
                String itemObtainApproachStr = characterJson.getString("itemObtainApproach");
                if("活动获得".equals(itemObtainApproachStr)){
                    itemObtainApproach = 0;
                }

                character.put("name", name);
//                character.put("itemUsage", itemUsage);
//                character.put("itemDesc", itemDesc);
                character.put("rarity", Integer.parseInt(characterJson.getString("rarity")) + 1);
                character.put("itemObtainApproach",itemObtainApproach);
                character.put("mod", modTable.get(charId));
                character.put("skill", skillList);
                character.put("date", charIdAndId.get(charId));
                character.put("profession", profession);
                hashMap.put(charId, character);
            }
        }


        FileUtil.save(ApplicationConfig.Item, "character_table_simple.json", JSON.toJSONString(hashMap));
        FileUtil.save("E:\\VCProject\\frontend-v2-plus\\src\\static\\json\\survey\\", "character_table_simple.json", JSON.toJSONString(hashMap));


        return null;
    }


    public  void getPortrait(){
        try {
            // 创建流对象

            String character_tableStr = FileUtil.read(githubBotResource+"gamedata\\excel\\character_table.json");

            JSONObject character_table = JSONObject.parseObject(character_tableStr);
            List<String> list  = new ArrayList<>();

            String startPath =  githubBotResource+"portrait\\";
            String portrait6 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\portrait-ori-6\\";
            String portrait5 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\portrait-ori-5\\";
            String portrait4 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\portrait-ori-4\\";
            String endPath = portrait4;
            for (String key : character_table.keySet()) {
                if(!key.startsWith("char")) continue;
                JSONObject charData = JSONObject.parseObject(character_table.getString(key));
                int rarity = Integer.parseInt(charData.getString("rarity"));
                System.out.println(key+"：星级："+rarity+"，文件名："+startPath+key+".png");
                File source = new File(startPath+key+"_1.png");

                if(rarity==5) endPath=portrait6;
                if(rarity==4) endPath=portrait5;
                if(rarity<4) endPath=portrait4;

                File tmpFile = new File(endPath);//获取文件夹路径

                if(!tmpFile.exists()){//判断文件夹是否创建，没有创建则创建新文件夹
                    boolean mkdirs = tmpFile.mkdirs();
                }

                File dest = new File(endPath + key+"_1.png");
                copyFile(source,dest);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        File source = new File("E:\\VCProject\\frontend-v2-plus\\public\\image\\avg_npc_380_1.png");
        File dest = new File("E:\\VCProject\\frontend-v2-plus\\public\\image\\avg_npc_380_1_copy.png");
        copyFile(source,dest);
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


    public  void getAvatar(){
        try {
            // 创建流对象

            String character_tableStr = FileUtil.read(githubBotResource+"gamedata\\excel\\character_table.json");

            JSONObject character_table = JSONObject.parseObject(character_tableStr);


            String startPath = githubBotResource+"avatar\\";
            String avatar6 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\avatar-ori-6\\";
            String avatar5 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\avatar-ori-5\\";
            String avatar4 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\avatar-ori-4\\";
            String endPath = avatar4;
            for (String key : character_table.keySet()) {
                if(!key.startsWith("char")) continue;
                JSONObject charData = JSONObject.parseObject(character_table.getString(key));
                int rarity = Integer.parseInt(charData.getString("rarity"));
                System.out.println(key+"：星级："+rarity+"，文件名："+startPath+key+".png");
                File source = new File(startPath+key+".png");

                if(rarity==5) endPath=avatar6;
                if(rarity==4) endPath=avatar5;
                if(rarity<4) endPath=avatar4;

                File tmpFile = new File(endPath);//获取文件夹路径

                if(!tmpFile.exists()){//判断文件夹是否创建，没有创建则创建新文件夹
                    boolean mkdirs = tmpFile.mkdirs();
                }

                File dest = new File(endPath + key+".png");
                copyFile(source,dest);

            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
