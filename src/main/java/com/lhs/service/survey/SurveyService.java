package com.lhs.service.survey;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.survey.SurveyUser;
import com.lhs.mapper.SurveyUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class SurveyService {

    @Resource
    private SurveyUserMapper surveyUserMapper;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 调查表注册
     *
     * @param ipAddress ip
     * @param userName  用户id
     * @return 成功消息
     */
    public HashMap<Object, Object> register(String ipAddress, String userName) {
//        Long incrId = redisTemplate.opsForValue().increment("SurveyID");   //从redis拿到自增id
        Date date = new Date();  //存入的时间
        String userNameAndEnd = null;
        SurveyUser surveyUser = null;

        for (int i = 0; i < 5; i++) {
            if (i < 3) {
                userNameAndEnd = userName + randomEnd4(4); //用户id后四位后缀  #xxxx
                surveyUser = surveyUserMapper.selectSurveyUserByUserName(userNameAndEnd);
                if (surveyUser == null) break;  //未注册就跳出开始注册
                log.warn("发生用户id碰撞");
            } else {
                userNameAndEnd = userName + randomEnd4(5);
                surveyUser = surveyUserMapper.selectSurveyUserByUserName(userNameAndEnd);
                if (surveyUser == null) break;  //未注册就跳出开始注册
                log.warn("用户id碰撞过多扩充位数");
            }
        }

        if (surveyUser != null) throw new ServiceException(ResultCode.USER_ID_ERROR);

        String idStr = date.getTime()+ ApplicationConfig.MachineId+randomEnd4_id();
        long id = Long.parseLong(idStr);

        surveyUser = SurveyUser.builder()
                .id(id)
                .userName(userNameAndEnd)
                .createTime(date)
                .updateTime(date)
                .status(1)
                .ip(ipAddress)
                .build();

        Integer row = surveyUserMapper.insertSurveyUser(surveyUser);
        if (row < 1) throw new ServiceException(ResultCode.SYSTEM_INNER_ERROR);

        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("userName", userNameAndEnd);
        hashMap.put("token", AES.encrypt(surveyUser.getUserName()+"."+id, ApplicationConfig.Secret));
        hashMap.put("status",1);

        return hashMap;
    }

    private String randomEnd4(Integer digit) {
        int random = new Random().nextInt(9999);
        String end4 = String.format("%0" + digit + "d", random);
        return "#" + end4;
    }

    private String randomEnd4_id(){
        int random = new Random().nextInt(99);
        return String.format("%02d", random);
    }



    /**
     * 调查表登录
     * @param ipAddress  ip
     * @param userName  用户id
     * @return 成功消息
     */
    public HashMap<Object, Object> login(String ipAddress, String userName) {
        SurveyUser surveyUser = surveyUserMapper.selectSurveyUserByUserName(userName);  //查询用户
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        if (surveyUser.getStatus()==0) throw new ServiceException(ResultCode.USER_ACCOUNT_FORBIDDEN);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("userName", userName);
        hashMap.put("token", AES.encrypt(userName+"."+surveyUser.getId(), ApplicationConfig.Secret));
        hashMap.put("status",surveyUser.getStatus());
        return hashMap;
    }


    public SurveyUser getSurveyUserByUserName(String userName){
        SurveyUser surveyUser = surveyUserMapper.selectSurveyUserByUserName(userName); //查询用户
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        return surveyUser;
    }

    public SurveyUser getSurveyUserById(Long id){
        SurveyUser surveyUser = surveyUserMapper.selectSurveyUserById(id); //查询用户
        if (surveyUser == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        return surveyUser;
    }


    public void updateSurveyUser(SurveyUser surveyUser){
        surveyUserMapper.updateSurveyUser(surveyUser);   //更新用户表
    }

    public List<Long> selectSurveyUserIds() {
        return  surveyUserMapper.selectSurveyUserIds();
    }


    public void updateConfigByKey(String value, String key) {
        surveyUserMapper.updateConfigByKey(value,key);
    }

    public String selectConfigByKey(String key) {
       return    surveyUserMapper.selectConfigByKey(key);
    }

    public Integer getTableIndex(Long id){
         if(id<30000) return 1;
         if(id<60000) return 2;
        return 1;
    }



    public SurveyUser registerByMaa(String ipAddress) {
        Long incrId = redisTemplate.opsForValue().increment("incrUId");
        SurveyUser surveyUser = surveyUserMapper.selectLastSurveyUserIp(ipAddress);

        if(surveyUser!=null) return surveyUser;

        Date date = new Date();
        long id = incrId;
        String userNameAndEnd = "MAA#" + incrId;
        surveyUser = SurveyUser.builder()
                .id(id)
                .userName(userNameAndEnd)
                .createTime(date)
                .updateTime(date)
                .status(1)
                .ip(ipAddress)
                .build();
        Integer row = surveyUserMapper.insertSurveyUser(surveyUser);
        if (row < 1) throw new ServiceException(ResultCode.SYSTEM_INNER_ERROR);

        return surveyUser;
    }

    @RedisCacheable(key = "CharacterTableSimple",timeout = -1)
    public JSONObject getCharacterTable(){
        String read = FileUtil.read(ApplicationConfig.Backup + "character_table_simple.json");
        return JSONObject.parseObject(read);
    }

   public  Long getUid(String token){
       String decrypt = AES.decrypt(token, ApplicationConfig.Secret);
       String idStr = decrypt.split("\\.")[1];
       return Long.valueOf(idStr);
   }

    private String githubBotResource = "E:\\BOT_img\\botResource\\Arknights-Bot-Resource\\";

    public HashMap<String, Object> getCharacterData() {

        String character_tableStr = FileUtil.read(githubBotResource+"gamedata\\excel\\character_table.json");
        String uniequip_tableStr = FileUtil.read(githubBotResource+"gamedata\\excel\\uniequip_table.json");
        String skill_tableStr = FileUtil.read(githubBotResource+"gamedata\\excel\\skill_table.json");
        String insertedTimeStr = FileUtil.read(ApplicationConfig.Backup+"insertedTime.json");
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
                character.put("itemUsage", itemUsage);
                character.put("itemDesc", itemDesc);
                character.put("rarity", Integer.parseInt(characterJson.getString("rarity")) + 1);
                character.put("itemObtainApproach",itemObtainApproach);
                character.put("mod", modTable.get(charId));
                character.put("skill", skillList);
                character.put("date", insertedTimeMap.get(name));
                character.put("profession", profession);
                hashMap.put(charId, character);
            }
        }


        FileUtil.save(ApplicationConfig.Item, "character_table_simple.json", JSON.toJSONString(hashMap));
        FileUtil.save("E:\\VCProject\\frontend-v2-plus\\src\\static\\json\\survey\\", "character_table_simple.json", JSON.toJSONString(hashMap));


        return null;
    }


    public  void getAvatar(){
        try {
            // 创建流对象

            String character_tableStr = FileUtil.read(githubBotResource+"gamedata\\excel\\character_table.json");

            JSONObject character_table = JSONObject.parseObject(character_tableStr);
            List<String> list  = new ArrayList<>();

            String startPath = "E:\\VCProject\\frontend-v2-plus\\public\\image\\avatar\\";
            String avatar6 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\avatar-ori-6\\";
            String avatar5 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\avatar-ori-5\\";
            String avatar4 = "E:\\VCProject\\frontend-v2-plus\\public\\image\\avatar-ori-4\\";
            String endPath = avatar4;
            for (String key : character_table.keySet()) {
                if(!key.startsWith("char")) continue;
                JSONObject charData = JSONObject.parseObject(character_table.getString(key));
                int rarity = Integer.parseInt(charData.getString("rarity"));
                System.out.println(key+"：星级："+rarity);
                File startFile = new File(startPath+key+".png");
                if(rarity==5) endPath=avatar6;
                if(rarity==4) endPath=avatar5;
                if(rarity<4) endPath=avatar4;

                if (startFile.renameTo(new File(endPath + key+".png"))) {
                    System.out.println("文件移动成功！文件名：《{"+key+"}》 目标路径：{"+endPath+"}");
                } else {
                    System.out.println("文件移动失败！文件名：《{"+key+"}》 目标路径：{"+endPath+"}");
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
