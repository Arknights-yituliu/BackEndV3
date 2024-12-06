package com.lhs.service.material.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.admin.HoneyCake;
import com.lhs.entity.po.admin.ImageInfo;
import com.lhs.entity.po.material.*;
import com.lhs.entity.vo.material.*;
import com.lhs.mapper.admin.HoneyCakeMapper;
import com.lhs.mapper.admin.ImageInfoMapper;
import com.lhs.mapper.material.*;
import com.lhs.mapper.material.service.StorePermMapperService;
import com.lhs.service.material.ItemService;
import com.lhs.service.material.StoreService;
import com.lhs.service.util.COSService;
import com.lhs.service.util.DataCacheService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StoreServiceImpl implements StoreService {


    private final StorePermMapper storePermMapper;

    private final StoreActMapper storeActMapper;

    private final ItemService itemService;

    private final HoneyCakeMapper honeyCakeMapper;

    private final RedisTemplate<String, Object> redisTemplate;


    private final StorePermMapperService storePermMapperService;

    private final PackInfoMapper packInfoMapper;
    private final IdGenerator idGenerator;

    private final COSService cosService;
    private final ImageInfoMapper imageInfoMapper;

    private final DataCacheService dataCacheService;

    public StoreServiceImpl(StorePermMapper storePermMapper, StoreActMapper storeActMapper, ItemService itemService,
                            HoneyCakeMapper honeyCakeMapper, RedisTemplate<String, Object> redisTemplate,
                            StorePermMapperService storePermMapperService, PackInfoMapper packInfoMapper,
                            COSService cosService, ImageInfoMapper imageInfoMapper, DataCacheService dataCacheService) {
        this.storePermMapper = storePermMapper;
        this.storeActMapper = storeActMapper;
        this.itemService = itemService;
        this.honeyCakeMapper = honeyCakeMapper;
        this.redisTemplate = redisTemplate;
        this.storePermMapperService = storePermMapperService;
        this.packInfoMapper = packInfoMapper;
        this.imageInfoMapper = imageInfoMapper;
        this.dataCacheService = dataCacheService;
        this.idGenerator = new IdGenerator(1L);
        this.cosService = cosService;
    }

    /**
     * 更新常驻商店性价比
     */
    @Override
    public void updateStorePerm() {
        List<StorePerm> storePermList = storePermMapper.selectList(null);
        Map<String, Item> collect = itemService.getItemListCache(new StageConfigDTO())
                .stream()
                .collect(Collectors.toMap(Item::getItemName, Function.identity()));

        for (StorePerm storePerm : storePermList) {
            storePerm.setCostPer(collect.get(storePerm.getItemName()).getItemValueAp() * storePerm.getQuantity() / storePerm.getCost());
            if ("grey".equals(storePerm.getStoreType())) storePerm.setCostPer(storePerm.getCostPer() * 100);
            storePerm.setRarity(collect.get(storePerm.getItemName()).getRarity());
            storePerm.setItemId(collect.get(storePerm.getItemName()).getItemId());
        }

        storePermMapperService.updateBatchById(storePermList);

        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()); // 设置日期格式

        Logger.info("常驻商店更新成功");
    }


    @Override
    @RedisCacheable(key = "Item:StorePerm", timeout = 86400)
    public Map<String, List<StorePerm>> getStorePerm() {
        List<StorePerm> storePerms = storePermMapper.selectList(null);
        Map<String, List<StorePerm>> resultMap = storePerms.stream().collect(Collectors.groupingBy(StorePerm::getStoreType));
        resultMap.forEach((k, list) -> list.sort(Comparator.comparing(StorePerm::getCostPer).reversed()));
        return resultMap;
    }


    @RedisCacheable(key = "Item:StorePermV2", timeout = 86400)
    @Override
    public Map<String, List<StorePermVO>> getStorePermMap(StageConfigDTO stageConfigDTO) {

        Map<String, List<StorePermVO>> storePermTable = readStorePermTable();
        Map<String, Item> itemMapCache = itemService.getItemMapCache(stageConfigDTO);
        for(String type:storePermTable.keySet()){
            List<StorePermVO> storePermVOList = storePermTable.get(type);
            for(StorePermVO item:storePermVOList){
                double value = itemMapCache.get(item.getItemId()).getItemValueAp();
                int quantity = item.getQuantity();
                double cost = item.getCost();
                double costPer = value*quantity/cost;
                if ("grey".equals(type)){
                    costPer=costPer*100;
                }
                item.setCostPer(costPer);
            }
        }


        return storePermTable;
    }

    public Map<String,List<StorePermVO>> readStorePermTable() {
        Object o = redisTemplate.opsForValue().get("StorePermTable");

        String text;
        if (o != null) {
            text = String.valueOf(o);
        } else {
            String read = FileUtil.read(ConfigUtil.Config + "store_perm_table.json");
            if (read == null) {
                throw new ServiceException(ResultCode.FILE_NOT_EXIST);
            }
            redisTemplate.opsForValue().set("StorePermTable", read);
            text = read;
        }

        return JsonMapper.parseObject(text, new TypeReference<>() {
        });
    }



    @Override
    public String updateActivityStoreDataByActivityName(ActivityStoreDataVO activityStoreDataVo, Boolean developerLevel) {
        List<Item> items = itemService.getItemListCache(new StageConfigDTO());
        Map<String, Item> itemMap = items.stream().collect(Collectors.toMap(Item::getItemName, Function.identity()));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<StoreItemVO> storeItemVOList = activityStoreDataVo.getActStore();
        String actName = activityStoreDataVo.getActName();

        storeItemVOList.forEach(a -> {
            a.setItemPPR(itemMap.get(a.getItemName()).getItemValueAp() * a.getItemQuantity() / a.getItemPrice());
            a.setItemId(itemMap.get(a.getItemName()).getItemId());
        });

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

        String message = "活动商店已更新";

        if (developerLevel) {
            StageConfigDTO stageConfigDTO = new StageConfigDTO();
            List<ActivityStoreDataVO> activityStoreDataVOList = getActivityStoreDataNoCache(stageConfigDTO);
            redisTemplate.opsForValue().set("Item:StoreAct", activityStoreDataVOList, 6, TimeUnit.HOURS);

            message = "活动商店已更新，并清空缓存";
        }

        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()); // 设置日期格式

        return message;
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
            for(StoreItemVO item:actStore){
                double value = itemMapCache.get(item.getItemId()).getItemValueAp();
                int quantity = item.getItemQuantity();
                double price = item.getItemPrice();
                double ppr = value*quantity/price;
                item.setItemPPR(ppr);
            }
            activityStoreDataVo.setImageLink(imageInfoMap.get(activityStoreDataVo.getActName()));
            activityStoreDataVOList.add(activityStoreDataVo);
        });
        return activityStoreDataVOList;
    }

    @Override
    @RedisCacheable(key = "Item:StoreAct")
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


    @Override
    public void updateHoneyCake(List<HoneyCake> honeyCakeList) {

        for (HoneyCake honeyCake : honeyCakeList) {
            System.out.println(honeyCake);
            QueryWrapper<HoneyCake> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", honeyCake.getName());
            HoneyCake honeyCakeOld = honeyCakeMapper.selectOne(queryWrapper);
            if (honeyCakeOld == null) {
                honeyCakeMapper.insert(honeyCake);
            } else {
                honeyCakeMapper.updateById(honeyCake);
            }
        }
    }


    @Override
    //    @RedisCacheable(key = "HoneyCake",timeout=86400)
    public Map<String, HoneyCake> getHoneyCake() {
        List<HoneyCake> honeyCakeList = getHoneyCakeList();
        return honeyCakeList.stream().collect(Collectors.toMap(HoneyCake::getName, Function.identity()));
    }

    @Override
    public List<HoneyCake> getHoneyCakeList() {
        QueryWrapper<HoneyCake> honeyCakeQueryWrapper = new QueryWrapper<>();
        honeyCakeQueryWrapper.orderByDesc("start");
        return honeyCakeMapper.selectList(honeyCakeQueryWrapper);
    }


}
