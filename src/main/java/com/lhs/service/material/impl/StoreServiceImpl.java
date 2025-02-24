package com.lhs.service.material.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.util.*;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.admin.ImageInfo;
import com.lhs.entity.po.material.*;
import com.lhs.entity.vo.material.*;
import com.lhs.mapper.admin.ImageInfoMapper;
import com.lhs.mapper.material.*;
import com.lhs.service.material.ItemService;
import com.lhs.service.material.StoreService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StoreServiceImpl implements StoreService {


    private final StoreActMapper storeActMapper;

    private final ItemService itemService;



    private final RedisTemplate<String, Object> redisTemplate;


    private final IdGenerator idGenerator;


    private final ImageInfoMapper imageInfoMapper;


    public StoreServiceImpl(
                            StoreActMapper storeActMapper,
                            ItemService itemService,

                            RedisTemplate<String, Object> redisTemplate,
                            ImageInfoMapper imageInfoMapper) {
        this.storeActMapper = storeActMapper;
        this.itemService = itemService;
        this.redisTemplate = redisTemplate;
        this.imageInfoMapper = imageInfoMapper;
        this.idGenerator = new IdGenerator(1L);

    }


    @Override
    public String updateActivityStoreDataByActivityName(ActivityStoreDataVO activityStoreDataVo) {
        Map<String, Item> itemMap = itemService.getItemMapCache(new StageConfigDTO());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<StoreItemVO> storeItemVOList = activityStoreDataVo.getActStore();
        String actName = activityStoreDataVo.getActName();

        for (StoreItemVO vo : storeItemVOList) {
            Item item = itemMap.get(vo.getItemId());
            if (item == null) {
                LogUtils.error(vo.getItemName() + "不存在");
                continue;
            }
            vo.setItemPPR(item.getItemValueAp() * vo.getItemQuantity() / vo.getItemPrice());
            vo.setItemId(item.getItemId());
        }

        storeItemVOList.sort(Comparator.comparing(StoreItemVO::getItemPPR).reversed());

        ActivityStoreData act_name = storeActMapper.selectOne(new QueryWrapper<ActivityStoreData>().eq("act_name", actName));
        ActivityStoreData build = ActivityStoreData.builder()
                .actName(actName)
                .endTime(new Date(activityStoreDataVo.getEndTime()))
                .result(JsonMapper.toJSONString(activityStoreDataVo))
                .build();
        if (act_name == null) {
            storeActMapper.insert(build);
        } else {
            storeActMapper.updateById(build);
        }


        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        List<ActivityStoreDataVO> activityStoreDataVOList = getActivityStoreDataNoCache(stageConfigDTO);
        redisTemplate.opsForValue().set("Item:StoreAct", activityStoreDataVOList, 6, TimeUnit.HOURS);


        return "活动商店已更新";
    }


    public List<ActivityStoreDataVO> getActivityStoreDataNoCache(StageConfigDTO stageConfigDTO) {
        QueryWrapper<ActivityStoreData> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("end_time", new Date());
        List<ActivityStoreData> activityStoreData = storeActMapper.selectList(queryWrapper);
        List<ActivityStoreDataVO> activityStoreDataVOList = new ArrayList<>();

        Map<String, Item> itemMapCache = itemService.getItemMapCache(stageConfigDTO);
        List<ImageInfo> imageInfos = imageInfoMapper.selectList(null);
        Map<String, String> imageInfoMap = imageInfos.stream().collect(Collectors.toMap(ImageInfo::getImageName, ImageInfo::getImageLink));

        activityStoreData.forEach(e -> {
            String result = e.getResult();
            ActivityStoreDataVO activityStoreDataVo = JsonMapper.parseObject(result, ActivityStoreDataVO.class);
            List<StoreItemVO> actStore = activityStoreDataVo.getActStore();
            for (StoreItemVO item : actStore) {
                double value = itemMapCache.get(item.getItemId()).getItemValueAp();
                int quantity = item.getItemQuantity();
                double price = item.getItemPrice();
                double ppr = value * quantity / price;
                item.setItemPPR(ppr);
            }
            activityStoreDataVo.setImageLink(imageInfoMap.get(activityStoreDataVo.getActName()));
            activityStoreDataVOList.add(activityStoreDataVo);
        });
        return activityStoreDataVOList;
    }

    @Override
    @RedisCacheable(key = "Item:StoreAct", paramOrMethod = "getVersionCode")
    public List<ActivityStoreDataVO> getActivityStoreData(StageConfigDTO stageConfigDTO) {
        return getActivityStoreDataNoCache(stageConfigDTO);
    }

    @Override
    public List<ActivityStoreDataVO> getActivityStoreHistoryData() {
        LambdaQueryWrapper<ActivityStoreData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(ActivityStoreData::getEndTime);
        List<ActivityStoreData> activityStoreData = storeActMapper.selectList(queryWrapper);
        List<ActivityStoreDataVO> activityStoreDataVOList = new ArrayList<>();
        activityStoreData.forEach(activity -> {
            String result = activity.getResult();
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
