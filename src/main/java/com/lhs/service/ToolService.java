package com.lhs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.mapper.VisitsMapper;
import com.lhs.entity.Visits;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ToolService {

    @Autowired
    private VisitsMapper visitsMapper;

    public void updateVisits(String path) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Visits visitResult = visitsMapper.selectOne(new QueryWrapper<Visits>()
                .eq("date", today));

        if(visitResult==null){
            visitResult = new Visits();
            visitResult.init();
            visitResult.update(path);
            visitsMapper.insert(visitResult);
        }else {
            visitResult.update(path);
            visitsMapper.updateById(visitResult);
        }
    }


    public HashMap<String, List<Object>> selectVisits(Date start,Date end){

        QueryWrapper<Visits> create_time = new QueryWrapper<Visits>().ge("create_time", start).le("create_time", end);
        List<Visits> visitsList = visitsMapper.selectList(create_time);
        HashMap<String, List<Object>> hashMap = new HashMap<>();
        for(Visits visits:visitsList){
//            System.out.println(visits);
            JSONObject visitsJson = JSONObject.parseObject(JSON.toJSONString(visits));
            Set<String> paths = JSONObject.parseObject(visitsJson.toString()).keySet();
            for(String path:paths){
                if(hashMap.get(path)!=null){
                    List<Object> visitsResult = hashMap.get(path);
                    visitsResult.add(visitsJson.getString(path));
                    hashMap.put(path,visitsResult);
                }else {
                    List<Object> visitsResult = new ArrayList<>();
                    hashMap.put(path,visitsResult);
                }
            }
        }

        return hashMap;

    }
}
