package com.lhs.service.item.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.item.StageParamDTO;
import com.lhs.entity.po.dev.HoneyCake;
import com.lhs.entity.po.item.*;
import com.lhs.entity.vo.item.*;
import com.lhs.mapper.dev.HoneyCakeMapper;
import com.lhs.mapper.item.*;
import com.lhs.mapper.item.service.PackContentMapperService;
import com.lhs.mapper.item.service.PackInfoMapperService;
import com.lhs.mapper.item.service.StorePermMapperService;
import com.lhs.service.item.ItemService;
import com.lhs.service.item.StoreService;
import com.lhs.service.util.OSSService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
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
    private final PackInfoMapperService packInfoMapperService;
    private final PackContentMapper packContentMapper;
    private final PackContentMapperService packContentMapperService;

    private final IdGenerator idGenerator;

    private final PackItemMapper packItemMapper;


    public StoreServiceImpl(StorePermMapper storePermMapper, StoreActMapper storeActMapper, ItemService itemService,
                            HoneyCakeMapper honeyCakeMapper, RedisTemplate<String, Object> redisTemplate,
                            OSSService ossService, StorePermMapperService storePermMapperService,
                            PackInfoMapper packInfoMapper, PackInfoMapperService packInfoMapperService,
                            PackContentMapper packContentMapper, PackContentMapperService packContentMapperService,
                            PackItemMapper packItemMapper) {
        this.storePermMapper = storePermMapper;
        this.storeActMapper = storeActMapper;
        this.itemService = itemService;
        this.honeyCakeMapper = honeyCakeMapper;
        this.redisTemplate = redisTemplate;
        this.ossService = ossService;
        this.storePermMapperService = storePermMapperService;
        this.packInfoMapper = packInfoMapper;
        this.packInfoMapperService = packInfoMapperService;
        this.packContentMapper = packContentMapper;
        this.packContentMapperService = packContentMapperService;
        this.idGenerator = new IdGenerator(1L);
        this.packItemMapper = packItemMapper;
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
            redisTemplate.delete("StoreAct");
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
        InputStream inputStream = null;

        try {
            inputStream = file.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        file.getOriginalFilename();

        String fileName = "";
        if (file.getOriginalFilename() != null) {
            String[] split = file.getOriginalFilename().split("\\.");
            fileName = idGenerator.nextId() + "." + split[1];
        } else {
            throw new ServiceException(ResultCode.FILE_IS_NULL);
        }

        ossService.uploadFileInputStream(inputStream, "image/store/" + fileName);


        return "https://yituliu.oss-cn-shanghai.aliyuncs.com/image/store/" + fileName;
    }

    @Override
    @RedisCacheable(key = "Item:StoreAct", timeout = 86400)
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
        List<ActivityStoreData> activityStoreData = storeActMapper.selectList(null);
        List<ActivityStoreDataVO> activityStoreDataVOList = new ArrayList<>();
        activityStoreData.forEach(l -> {
            String result = l.getResult();
            ActivityStoreDataVO activityStoreDataVo = JsonMapper.parseObject(result, ActivityStoreDataVO.class);
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

    @Override
    public PackInfoVO updateStorePackById(PackInfoVO packInfoVO) {
        Date currentDate = new Date();
        //创建一个po对象存储数据
        PackInfo packInfo = new PackInfo();
        //将VO类的数据传递给po
        packInfo.copy(packInfoVO);
        packInfo.setCreateTime(currentDate);


        //判断是新礼包还是旧礼包
        if (packInfoVO.getNewPack()) {
            //新礼包直接生成一个id保存到数据库
            packInfo.setId(idGenerator.nextId());
            packInfoMapper.insert(packInfo);
        } else {
            //旧礼包需要更新,通过id查询旧礼包的信息
            QueryWrapper<PackInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", packInfo.getId());
            PackInfo packInfoById = packInfoMapper.selectOne(queryWrapper);
            if (packInfoById == null) {
                //如果旧礼包不存在则直接新增
                packInfo.setId(idGenerator.nextId());
                packInfoMapper.insert(packInfo);
            } else {
                //如果旧礼包存在则根据id更新
                packInfoMapper.updateById(packInfo);
            }
        }

        //礼包id
        Long packId = packInfo.getId();

        //礼包没有除四种抽卡资源之外内容直接返回礼包信息
        if (packInfoVO.getPackContent() == null) {
            return getPackById(packId.toString());
        }

        //礼包的额外内容
        List<PackContentVO> packContentVOList = packInfoVO.getPackContent();
        //创建一个礼包额外内容的po类的集合
        List<PackContent> packContentList = new ArrayList<>();
        //将vo类的内容拷贝到po,同时生成礼包id
        for (PackContentVO packContentVO : packContentVOList) {
            PackContent packContent = new PackContent();
            packContent.copy(packContentVO);
            packContent.setPackId(packId);
            packContent.setArchived(false);
            packContent.setId(idGenerator.nextId());
            packContentList.add(packContent);
        }

        UpdateWrapper<PackContent> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("pack_id", packId)
                .set("archived", true);
        packContentMapper.update(null, updateWrapper);

        //批量保存
        packContentMapperService.saveBatch(packContentList);

        redisTemplate.delete("Item:PackData");

        return getPackById(packId.toString());
    }

    @Scheduled(cron = "0 0 0 1/7 * ?")
    public void deleteArchivedPackContentData() {
        QueryWrapper<PackContent> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("archived", true);
        int delete = packContentMapper.delete(queryWrapper);
        Logger.info("删除了" + delete + "条礼包内容归档数据");
    }

    @RedisCacheable(key = "Item:PackData")
    @Override
    public List<PackInfoVO> listPackInfoBySaleStatus(Integer saleStatus) {
        QueryWrapper<PackInfo> packInfoQueryWrapper = new QueryWrapper<>();
        packInfoQueryWrapper.ge("end", new Date());

        if (saleStatus != null) {
            packInfoQueryWrapper.eq("sale_status", saleStatus);
        }
        List<PackInfo> packInfoList = packInfoMapper.selectList(packInfoQueryWrapper);
        List<Long> packIdList = packInfoList.stream().map(PackInfo::getId).collect(Collectors.toList());
        QueryWrapper<PackContent> packContentQueryWrapper = new QueryWrapper<>();
        packContentQueryWrapper.eq("archived", false);
        packContentQueryWrapper.in("pack_id", packIdList);

        List<PackContent> packContentList = packContentMapper.selectList(packContentQueryWrapper);

        Map<Long, List<PackContent>> collect = packContentList.stream().collect(Collectors.groupingBy(PackContent::getPackId));
        Map<String, Double> itemValueMap = packItemMapper.selectList(null)
                .stream().collect(Collectors.toMap(PackItem::getId, PackItem::getValue));

        List<PackInfoVO> VOList = new ArrayList<>();
        for (PackInfo packInfo : packInfoList) {
            PackInfoVO packInfoVO = getPackInfoVO(packInfo, collect.get(packInfo.getId()));
            VOList.add(packInfoVO);
            packPromotionRatioCalc(packInfoVO, itemValueMap);
        }
        return VOList;
    }


    @Override
    public List<PackInfoVO> listAllPackInfoData() {
        List<PackInfo> packInfoList = packInfoMapper.selectList(null);
        QueryWrapper<PackContent> packContentQueryWrapper = new QueryWrapper<>();
        packContentQueryWrapper.eq("archived", false);
        List<PackContent> packContentList = packContentMapper.selectList(packContentQueryWrapper);
        Map<Long, List<PackContent>> collect = packContentList.stream()
                .collect(Collectors.groupingBy(PackContent::getPackId));
        Map<String, Double> itemValueMap = packItemMapper.selectList(null).stream().collect(Collectors.toMap(PackItem::getId, PackItem::getValue));
        List<PackInfoVO> VOList = new ArrayList<>();
        for (PackInfo packInfo : packInfoList) {
            PackInfoVO packInfoVO = getPackInfoVO(packInfo, collect.get(packInfo.getId()));
            VOList.add(packInfoVO);
            packPromotionRatioCalc(packInfoVO, itemValueMap);
        }
        return VOList;
    }


    @Override
    public PackInfoVO getPackById(String idStr) {
        long id = Long.parseLong(idStr);
        PackInfo packInfo = packInfoMapper.selectOne(new QueryWrapper<PackInfo>().eq("id", id));
        QueryWrapper<PackContent> packContentQueryWrapper = new QueryWrapper<>();
        packContentQueryWrapper.eq("pack_id", id).eq("archived", false);
        List<PackContent> packContentList = packContentMapper.selectList(packContentQueryWrapper);
        return getPackInfoVO(packInfo, packContentList);
    }

    @Override
    public List<PackItem> listPackItem() {
        return packItemMapper.selectList(null);

    }

    @Override
    public PackItem saveOrUpdatePackItem(PackItem newPackItem) {
        QueryWrapper<PackItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", newPackItem.getId());
        PackItem packItem = packItemMapper.selectOne(queryWrapper);
        if (packItem == null) {
            newPackItem.setId(String.valueOf(idGenerator.nextId()));
            packItemMapper.insert(newPackItem);
        } else {
            packItemMapper.update(newPackItem, queryWrapper);
        }
        return newPackItem;
    }

    @Override
    public void deletePackItemById(String id) {
        QueryWrapper<PackItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        packItemMapper.delete(queryWrapper);
    }

    @Override
    public void updatePackState(String id, Integer state) {
        UpdateWrapper<PackInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("sale_status", state);
        updateWrapper.eq("id", id);
        int update = packInfoMapper.update(null, updateWrapper);
    }

    @Override
    public void uploadImage(MultipartFile file, Long id) {

        if (file.isEmpty()) {
            throw new ServiceException(ResultCode.FILE_IS_NULL);
        }

        QueryWrapper<PackInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        PackInfo packInfo = packInfoMapper.selectOne(queryWrapper);

        if (packInfo == null) {
            throw new ServiceException(ResultCode.DATA_NONE);
        }

        String fileName = null;
        if (file.getOriginalFilename() != null) {
            String[] split = file.getOriginalFilename().split("\\.");
            fileName = packInfo.getOfficialName() + "." + idGenerator.nextId() + "." + split[1];
        }


        String filePath = ConfigUtil.Resources + "image/store/" + fileName;
        File saveFile = new File(filePath);

        try {
            file.transferTo(saveFile);
        } catch (IOException exception) {
            Logger.error(exception.getMessage());
        }

        UpdateWrapper<PackInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("image_name", fileName).eq("id", id);
        packInfoMapper.update(null, updateWrapper);

        try {
            ossService.uploadFileInputStream(new FileInputStream(saveFile), "image/store/" + fileName);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String clearPackCache() {
        Boolean delete = redisTemplate.delete("Item:PackData");
        if (Boolean.FALSE.equals(delete)) {
            throw new ServiceException(ResultCode.REDIS_CLEAR_CACHE_ERROR);
        }
        return "缓存清除成功";
    }


    private PackInfoVO getPackInfoVO(PackInfo packInfo, List<PackContent> packContentList) {
        PackInfoVO packInfoVO = new PackInfoVO();
        packInfoVO.copy(packInfo);
        packInfoVO.setNewPack(false);
        if (packContentList != null) {
            List<PackContentVO> packContentVOList = new ArrayList<>();
            for (PackContent packContent : packContentList) {
                PackContentVO packContentVO = new PackContentVO();
                packContentVO.copy(packContent);
                packContentVOList.add(packContentVO);
                packInfoVO.setPackContent(packContentVOList);
            }
        }

        return packInfoVO;
    }

    private void packPromotionRatioCalc(PackInfoVO packInfoVO, Map<String, Double> itemValue) {
        double drawPrice = 0.0; //每一抽价格
        double packedOriginiumPrice = 0.0; //每源石（折算物资后）价格
        double drawEfficiency = 0.0; //氪金性价比
        double packEfficiency = 0.0; //综合性价比
        double packedOriginium = 0.0;
        double draws = 0.0;

        double totalOfOrundum = packInfoVO.getOrundum() + packInfoVO.getOriginium() * 180
                + packInfoVO.getGachaTicket() * 600 + packInfoVO.getTenGachaTicket() * 6000;

        double eachOriginalOriginiumPrice = 648 / 185.0;
        double eachOriginalDrawPrice = 648.0 / 185 / 0.3;

        if (totalOfOrundum > 0) {
            //计算共计多少抽
            draws = totalOfOrundum / 600;
            //计算等效多少源石 1源石 = 180合成玉
            packedOriginium += totalOfOrundum / 180;
            //计算每一抽的价格
            drawPrice = packInfoVO.getPrice() / draws;
            //计算抽卡性价比
            drawEfficiency = eachOriginalDrawPrice / drawPrice;
            //计算每个源石的价格
            packedOriginiumPrice = packInfoVO.getPrice() / packedOriginium;
            //计算综合性价比
            packEfficiency = eachOriginalOriginiumPrice / packedOriginiumPrice;
        }

        List<PackContentVO> packContentVOList = packInfoVO.getPackContent();
        //当这个礼包的物品不为空时
        if (packContentVOList != null) {
            double apCount = 0.0;
            for (PackContentVO packContentVO : packContentVOList) {
                if (itemValue.get(packContentVO.getItemId()) != null) {
                    apCount += itemValue.get(packContentVO.getItemId()) * packContentVO.getQuantity();
                }
            }
            packedOriginium += apCount / 135;
            if (packedOriginium > 0) {
                packedOriginiumPrice = packInfoVO.getPrice() / packedOriginium;
                packEfficiency = eachOriginalOriginiumPrice / packedOriginiumPrice;
            }
        }

        packInfoVO.setDraws(draws);
        packInfoVO.setDrawPrice(drawPrice);
        packInfoVO.setPackedOriginiumPrice(packedOriginiumPrice);
        packInfoVO.setDrawEfficiency(drawEfficiency);
        packInfoVO.setPackedOriginium(packedOriginium);
        packInfoVO.setPackEfficiency(packEfficiency);
    }
}
