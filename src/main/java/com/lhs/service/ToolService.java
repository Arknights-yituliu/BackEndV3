package com.lhs.service;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;

import com.lhs.common.util.ConfigUtil;
import com.lhs.common.util.FileUtil;
import jdk.nashorn.internal.scripts.JO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service
public class ToolService {
    @Value("${aliyun.AccessKeyID}")
    private String AccessKeyId;
    @Value("${aliyun.AccessKeySecret}")
    private String AccessKeySecret;

    public void ossUpload(String content, String objectName) {
        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
        String endpoint = "https://oss-cn-beijing.aliyuncs.com";
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = AccessKeyId;
        String accessKeySecret = AccessKeySecret;
        // 填写Bucket名称，例如examplebucket。
        String bucketName = "yituliu-bak";
        // 填写Object完整路径，例如exampledir/exampleobject.txt。Object完整路径中不能包含Bucket名称。

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(content.getBytes()));
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }


    public HashMap<String, Object> getCharacterData() {

        String character_tableStr = FileUtil.read("E:\\BOT_img\\botResource\\Arknights-Bot-Resource\\gamedata\\excel\\character_table.json");
        String uniequip_tableStr = FileUtil.read("E:\\BOT_img\\botResource\\Arknights-Bot-Resource\\gamedata\\excel\\uniequip_table.json");
        String skill_tableStr = FileUtil.read("E:\\BOT_img\\botResource\\Arknights-Bot-Resource\\gamedata\\excel\\skill_table.json");
        String insertedTimeStr = FileUtil.read(ConfigUtil.Backup+"insertedTime.json");
        JSONObject character_table = JSONObject.parseObject(character_tableStr);
        JSONObject uniequip_table = JSONObject.parseObject(uniequip_tableStr);
        JSONObject equipDict = JSONObject.parseObject(uniequip_table.getString("equipDict"));
        JSONObject skill_table = JSONObject.parseObject(skill_tableStr);
        HashMap<String, HashMap<String, Boolean>> modTable = new HashMap<>();

        HashMap<String,Long> insertedTimeMap = new HashMap<>();
        JSONArray.parseArray(insertedTimeStr).forEach(e->{
            String insertedTime = JSONObject.parseObject(String.valueOf(e)).getString("insertedTime");
            String name = JSONObject.parseObject(String.valueOf(e)).getString("name");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
            try {
                Date parse = dateFormat.parse(insertedTime);
                insertedTimeMap.put(name,parse.getTime());
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }


        });

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
        character_table.forEach((k, v) -> {
            if (k.startsWith("char")) {
                HashMap<Object, Object> character = new HashMap<>();
                JSONObject characterJson = JSONObject.parseObject(String.valueOf(v));

                JSONArray skills = JSONArray.parseArray(characterJson.getString("skills"));
                List<HashMap<Object, Object> > skillList = new ArrayList<>();
                for (int i = 0; i < skills.size(); i++) {
                    JSONObject jsonObject = JSONObject.parseObject(String.valueOf(skills.get(i)));
                    String skillId = jsonObject.getString("skillId");
                    skillId =skillId.replace("[","_");
                    skillId =skillId.replace("]","_");
                    HashMap<Object, Object> skill = new HashMap<>();
                    skill.put("iconId",skillId);
                    skill.put("name",skillTable.get(skillId));
                    skillList.add(skill);
                }
                String name = characterJson.getString("name");
                String profession = characterJson.getString("profession");
                character.put("name",name );
                character.put("rarity", Integer.parseInt(characterJson.getString("rarity")) + 1);
                character.put("mod", modTable.get(k));
                character.put("skill",skillList);
                character.put("date",insertedTimeMap.get(name));
                character.put("profession",profession);
                hashMap.put(k, character);
            }
        });


        FileUtil.save(ConfigUtil.Item, "characterBasicInfo.json", JSON.toJSONString(hashMap));
        FileUtil.save("E:\\VCProject\\frontend-v2-plus\\src\\static\\json\\survey\\", "characterBasicInfo.json", JSON.toJSONString(hashMap));


        return null;
    }


}
