package com.lhs.service;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.SurveyDataChar;
import com.lhs.entity.SurveyUser;
import com.lhs.mapper.SurveyDataMapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;

@Service
public class SurveyService {

    @Resource
    private SurveyDataMapper surveyDataMapper;

    public HashMap<Object, Object> register(String ipAddress,String userName){
        int end3 = new Random().nextInt(999);
        long time = new Date().getTime();
        long id  = time * 1000 + end3;
        SurveyUser surveyUser = surveyDataMapper.selectSurveyUserByUserName(userName);
        if(surveyUser!=null) throw  new ServiceException(ResultCode.USER_HAS_EXISTED);
        String charTable = surveyDataMapper.selectConfigByKey("charTable");
         surveyUser = SurveyUser.builder()
                .id(id)
                .userName(userName)
                .createTime(new Date())
                .status(1)
                .ip(ipAddress)
                .charTable(charTable)
                .build();
        Integer row = surveyDataMapper.insertSurveyUser(surveyUser);
        if(row<1) throw new ServiceException(ResultCode.SYSTEM_INNER_ERROR);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("userName",userName);
        hashMap.put("id",id);
        hashMap.put("status",1);
        return  hashMap;
    }


    public HashMap<Object, Object> uploadCharacterTable(String userName, List<SurveyDataChar> surveyDataCharList) {
        SurveyUser surveyUser = surveyDataMapper.selectSurveyUserByUserName(userName);
        if(surveyUser==null) throw  new ServiceException(ResultCode.USER_NOT_EXIST);
        surveyDataMapper.updateSurveyUser(surveyUser);
        String charTable = surveyUser.getCharTable();
        Long uid = surveyUser.getId();
        int rowsAffected = 0;
        List<SurveyDataChar>   insertList = new ArrayList<>();
        for(SurveyDataChar surveyDataChar:surveyDataCharList){
            String id = uid +"_"+ surveyDataChar.getCharId();
            SurveyDataChar surveyDataCharById = surveyDataMapper.selectSurveyDataCharById(charTable, id);
            surveyDataChar.setId(id);
            surveyDataChar.setUid(uid);
            if(surveyDataCharById ==null){
                insertList.add(surveyDataChar);
                rowsAffected++;
            }else {
                if(!surveyDataCharEquals(surveyDataChar,surveyDataCharById)){
                    rowsAffected++;
                    surveyDataMapper.updateSurveyDataCharById(charTable,surveyDataChar);
                }
            }
        }

        if(insertList.size()>0)  surveyDataMapper.insertBatchSurveyDataChar(charTable,insertList);

        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("rowsAffected", rowsAffected);
        hashMap.put("uid", uid);
        return hashMap;
    }

    private  boolean surveyDataCharEquals(SurveyDataChar newData , SurveyDataChar oldData){
        return Objects.equals(newData.getLevel(), oldData.getLevel())
                && Objects.equals(newData.getPhase(), oldData.getPhase())
                && Objects.equals(newData.getPotential(), oldData.getPotential())
                && Objects.equals(newData.getSkill1(), oldData.getSkill1())
                && Objects.equals(newData.getSkill2(), oldData.getSkill2())
                && Objects.equals(newData.getSkill3(), oldData.getSkill3())
                && Objects.equals(newData.getModX(), oldData.getModX())
                && Objects.equals(newData.getModY(), oldData.getModY());
    }

}
