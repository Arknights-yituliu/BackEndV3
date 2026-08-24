package com.lhs.service.material.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.util.*;
import com.lhs.entity.po.admin.ImageInfo;
import com.lhs.entity.po.material.*;
import com.lhs.entity.vo.material.*;
import com.lhs.mapper.admin.ImageInfoMapper;
import com.lhs.mapper.material.*;
import com.lhs.service.material.StoreService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StoreServiceImpl implements StoreService {


    private final StoreActMapper storeActMapper;





    private final RedisTemplate<String, Object> redisTemplate;


    private final IdGenerator idGenerator;


    private final ImageInfoMapper imageInfoMapper;


    public StoreServiceImpl(
                            StoreActMapper storeActMapper,


                            RedisTemplate<String, Object> redisTemplate,
                            ImageInfoMapper imageInfoMapper) {
        this.storeActMapper = storeActMapper;

        this.redisTemplate = redisTemplate;
        this.imageInfoMapper = imageInfoMapper;
        this.idGenerator = new IdGenerator(1L);

    }


    @Override
    public String updateActivityStoreDataByActivityName(ActivityStoreDataVO activityStoreDataVo) {
        String actName = activityStoreDataVo.getActName();

        ActivityStoreData act_name = storeActMapper.selectOne(new QueryWrapper<ActivityStoreData>().eq("act_name", actName));
        ActivityStoreData activityStoreData = new ActivityStoreData();
        activityStoreData.setActName(actName);
        activityStoreData.setEndTime(new Date(activityStoreDataVo.getEndTime()));
        activityStoreData.setStoreData(JsonMapper.toJSONString(activityStoreDataVo));

        if (act_name == null) {
            storeActMapper.insert(activityStoreData);
        } else {
            storeActMapper.updateById(activityStoreData);
        }

        redisTemplate.delete("Item:ActStoreInfo");

        return "活动商店已更新";
    }

    @RedisCacheable(key="Item:ActStoreInfo")
    @Override
    public List<ActivityStoreDataVO> listActivityStoreData() {

        LambdaQueryWrapper<ActivityStoreData> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.ge(ActivityStoreData::getEndTime, new Date());
        List<ActivityStoreData> activityStoreDataList = storeActMapper.selectList(lambdaQueryWrapper);
        List<ActivityStoreDataVO> activityStoreDataVOList = new ArrayList<>();
        List<ImageInfo> imageInfos = imageInfoMapper.selectList(null);
        Map<String, String> imageInfoMap = imageInfos.stream().collect(Collectors.toMap(ImageInfo::getImageName, ImageInfo::getImageLink));
        for(ActivityStoreData activityStoreData : activityStoreDataList){
            String info = activityStoreData.getStoreData();
            ActivityStoreDataVO activityStoreDataVo = JsonMapper.parseObject(info, ActivityStoreDataVO.class);
            activityStoreDataVo.setImageLink(imageInfoMap.get(activityStoreDataVo.getActName()));
            activityStoreDataVOList.add(activityStoreDataVo);
        }

        return activityStoreDataVOList;
    }



    @Override
    public List<ActivityStoreDataVO> getActivityStoreHistoryData() {
        LambdaQueryWrapper<ActivityStoreData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(ActivityStoreData::getEndTime);
        List<ActivityStoreData> activityStoreData = storeActMapper.selectList(queryWrapper);
        List<ActivityStoreDataVO> activityStoreDataVOList = new ArrayList<>();
        activityStoreData.forEach(activity -> {
            String result = activity.getStoreData();
            ActivityStoreDataVO activityStoreDataVo = JsonMapper.parseObject(result, ActivityStoreDataVO.class);
            LambdaQueryWrapper<ImageInfo> imageInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
            imageInfoLambdaQueryWrapper.eq(ImageInfo::getImageName, activityStoreDataVo.getActName());
            ImageInfo imageInfo = imageInfoMapper.selectOne(imageInfoLambdaQueryWrapper);
            if (imageInfo != null) {
                activityStoreDataVo.setImageLink(imageInfo.getImageLink());
            }
            activityStoreDataVOList.add(activityStoreDataVo);
        });
        return activityStoreDataVOList;
    }







}
