package com.lhs.service.item.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.item.StageParamDTO;
import com.lhs.entity.po.admin.HoneyCake;
import com.lhs.entity.po.item.*;
import com.lhs.entity.vo.item.*;
import com.lhs.mapper.admin.HoneyCakeMapper;
import com.lhs.mapper.item.*;
import com.lhs.mapper.item.service.StorePermMapperService;
import com.lhs.service.item.ItemService;
import com.lhs.service.item.StoreService;
import com.lhs.service.util.COSService;
import com.lhs.service.util.OSSService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    private final OSSService ossService;

    private final StorePermMapperService storePermMapperService;

    private final PackInfoMapper packInfoMapper;
    private final IdGenerator idGenerator;

    private final COSService cosService;
    public StoreServiceImpl(StorePermMapper storePermMapper, StoreActMapper storeActMapper, ItemService itemService,
                            HoneyCakeMapper honeyCakeMapper, RedisTemplate<String, Object> redisTemplate,
                            OSSService ossService, StorePermMapperService storePermMapperService,
                            PackInfoMapper packInfoMapper,COSService cosService) {
        this.storePermMapper = storePermMapper;
        this.storeActMapper = storeActMapper;
        this.itemService = itemService;
        this.honeyCakeMapper = honeyCakeMapper;
        this.redisTemplate = redisTemplate;
        this.ossService = ossService;
        this.storePermMapperService = storePermMapperService;
        this.packInfoMapper = packInfoMapper;
        this.idGenerator = new IdGenerator(1L);
        this.cosService = cosService;
    }

    /**
     * 更新常驻商店性价比
     */
    @Override
    public void updateStorePerm() {
        List<StorePerm> storePermList = storePermMapper.selectList(null);
        Map<String, Item> collect = itemService.getItemListCache(new StageParamDTO().getVersion())
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
        ossService.upload(JsonMapper.toJSONString(storePermList), "backup/store/" + yyyyMMdd + "/perm " + yyyyMMddHHmm + ".json");
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

    @Override
    public String updateActivityStoreDataByActivityName(ActivityStoreDataVO activityStoreDataVo, Boolean developerLevel) {
        List<Item> items = itemService.getItemListCache(new StageParamDTO().getVersion());
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
                .imageLink(activityStoreDataVo.getImageLink())
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
            List<ActivityStoreDataVO> activityStoreDataVOList = getActivityStoreDataNoCache();
            redisTemplate.opsForValue().set("Item:StoreAct", activityStoreDataVOList, 6, TimeUnit.HOURS);

            message = "活动商店已更新，并清空缓存";
        }

        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()); // 设置日期格式
        ossService.upload(JsonMapper.toJSONString(activityStoreDataVo), "backup/store/" + yyyyMMdd + "/act " + yyyyMMddHHmm + ".json");
        return message;
    }

    @Override
    public String uploadActivityBackgroundImage(MultipartFile file) {
        if (file == null) {
            throw new ServiceException(ResultCode.FILE_IS_NULL);
        }

        file.getOriginalFilename();

        String fileName = "";
        if (file.getOriginalFilename() != null) {
            String[] split = file.getOriginalFilename().split("\\.");
            fileName = idGenerator.nextId() + "." + split[1];
        } else {
            throw new ServiceException(ResultCode.FILE_IS_NULL);
        }

        cosService.uploadFile(file,"/image/store/"+fileName);

        return "https://cos.yituliu.cn/image/store/" + fileName;
    }


    public List<ActivityStoreDataVO> getActivityStoreDataNoCache() {
        QueryWrapper<ActivityStoreData> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("end_time", new Date());
        List<ActivityStoreData> activityStoreData = storeActMapper.selectList(queryWrapper);
        List<ActivityStoreDataVO> activityStoreDataVOList = new ArrayList<>();
        activityStoreData.forEach(e -> {
            String result = e.getResult();
            ActivityStoreDataVO activityStoreDataVo = JsonMapper.parseObject(result, ActivityStoreDataVO.class);
            activityStoreDataVo.setImageLink(e.getImageLink());
            activityStoreDataVOList.add(activityStoreDataVo);
        });
        return activityStoreDataVOList;
    }

    @Override
    @RedisCacheable(key = "Item:StoreAct")
    public List<ActivityStoreDataVO> getActivityStoreData() {
        QueryWrapper<ActivityStoreData> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("end_time", new Date());
        List<ActivityStoreData> activityStoreData = storeActMapper.selectList(queryWrapper);
        List<ActivityStoreDataVO> activityStoreDataVOList = new ArrayList<>();
        activityStoreData.forEach(e -> {
            String result = e.getResult();
            ActivityStoreDataVO activityStoreDataVo = JsonMapper.parseObject(result, ActivityStoreDataVO.class);
            activityStoreDataVo.setImageLink(e.getImageLink());
            activityStoreDataVOList.add(activityStoreDataVo);
        });
        return activityStoreDataVOList;
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
            activityStoreDataVo.setImageLink(activity.getImageLink());
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
