package com.lhs.service.item.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.LogUtil;
import com.lhs.entity.dto.item.StageParamDTO;
import com.lhs.entity.po.dev.HoneyCake;
import com.lhs.entity.po.item.*;
import com.lhs.entity.vo.item.PackContentVO;
import com.lhs.entity.vo.item.PackPromotionRatioVO;
import com.lhs.entity.vo.item.StoreActVO;
import com.lhs.entity.vo.item.StoreItemVO;
import com.lhs.mapper.dev.HoneyCakeMapper;
import com.lhs.mapper.item.*;
import com.lhs.mapper.item.service.PackContentMapperService;
import com.lhs.mapper.item.service.PackPromotionRatioMapperService;
import com.lhs.mapper.item.service.StorePermMapperService;
import com.lhs.service.item.ItemService;
import com.lhs.service.item.StoreService;
import com.lhs.service.util.OSSService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.ParseException;
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

    private final PackPromotionRatioMapper packPromotionRatioMapper;
    private final PackPromotionRatioMapperService packPromotionRatioMapperService;
    private final PackContentMapper packContentMapper;
    private final PackContentMapperService packContentMapperService;

    private final IdGenerator idGenerator;

    private final PackItemMapper packItemMapper;


    public StoreServiceImpl(StorePermMapper storePermMapper, StoreActMapper storeActMapper, ItemService itemService, HoneyCakeMapper honeyCakeMapper, RedisTemplate<String, Object> redisTemplate, OSSService ossService, StorePermMapperService storePermMapperService, PackPromotionRatioMapper packPromotionRatioMapper, PackPromotionRatioMapperService packPromotionRatioMapperService, PackContentMapper packContentMapper, PackContentMapperService packContentMapperService, PackItemMapper packItemMapper) {
        this.storePermMapper = storePermMapper;
        this.storeActMapper = storeActMapper;
        this.itemService = itemService;
        this.honeyCakeMapper = honeyCakeMapper;
        this.redisTemplate = redisTemplate;
        this.ossService = ossService;
        this.storePermMapperService = storePermMapperService;
        this.packPromotionRatioMapper = packPromotionRatioMapper;
        this.packPromotionRatioMapperService = packPromotionRatioMapperService;
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
        LogUtil.info("常驻商店更新成功");
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
    public String updateActStoreByActName(StoreActVO storeActVo, Boolean level) {
        List<Item> items = itemService.getItemListCache(new StageParamDTO().getVersion());
        Map<String, Item> itemMap = items.stream().collect(Collectors.toMap(Item::getItemName, Function.identity()));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<StoreItemVO> storeItemVOList = storeActVo.getActStore();
        String actName = storeActVo.getActName();
        String actEndDate = storeActVo.getActEndDate();

        Date date = new Date();
        try {
            date = sdf.parse(actEndDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        storeItemVOList.forEach(a -> {
            a.setItemPPR(itemMap.get(a.getItemName()).getItemValueAp() * a.getItemQuantity() / a.getItemPrice());
            a.setItemId(itemMap.get(a.getItemName()).getItemId());
        });

        storeItemVOList.sort(Comparator.comparing(StoreItemVO::getItemPPR).reversed());

        StoreAct act_name = storeActMapper.selectOne(new QueryWrapper<StoreAct>().eq("act_name", actName));
        StoreAct build = StoreAct.builder().actName(actName).endTime(date).result(JsonMapper.toJSONString(storeActVo)).build();
        if (act_name == null) {
            storeActMapper.insert(build);
        } else {
            storeActMapper.updateById(build);
        }

        String message = "活动商店已更新";

        if (level) {
            redisTemplate.delete("StoreAct");
            message = "活动商店已更新，并清空缓存";

        }

        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()); // 设置日期格式
        ossService.upload(JsonMapper.toJSONString(storeActVo), "backup/store/" + yyyyMMdd + "/act " + yyyyMMddHHmm + ".json");
        return message;
    }

    @Override
    @RedisCacheable(key = "Item:StoreAct", timeout = 86400)
    public List<StoreActVO> getStoreAct() {
        List<StoreAct> storeActs = storeActMapper.selectList(null);
        List<StoreActVO> storeActVOList = new ArrayList<>();
        storeActs.forEach(l -> {
            String result = l.getResult();
            if (l.getEndTime().getTime() > new Date().getTime()) {
                StoreActVO storeActVo = JsonMapper.parseObject(result, StoreActVO.class);
                storeActVOList.add(storeActVo);
            }
        });
        return storeActVOList;
    }

    @Override
    public List<StoreActVO> selectActStoreHistory() {
        List<StoreAct> storeActs = storeActMapper.selectList(null);
        List<StoreActVO> storeActVOList = new ArrayList<>();
        storeActs.forEach(l -> {
            String result = l.getResult();
            StoreActVO storeActVo = JsonMapper.parseObject(result, StoreActVO.class);
            storeActVOList.add(storeActVo);
        });
        return storeActVOList;
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
    public PackPromotionRatioVO updateStorePackById(PackPromotionRatioVO packPromotionRatioVO) {
        //创建一个po对象存储数据
        PackPromotionRatio packPromotionRatio = new PackPromotionRatio();
        //将VO类的数据传递给po
        packPromotionRatio.copy(packPromotionRatioVO);
        //id生成器


        //判断是新礼包还是旧礼包
        if (packPromotionRatioVO.getNewPack()) {
            //新礼包直接生成一个id保存到数据库
            packPromotionRatio.setId(idGenerator.nextId());
            packPromotionRatioMapper.insert(packPromotionRatio);
        } else {
            //旧礼包需要更新,通过id查询旧礼包的信息
            QueryWrapper<PackPromotionRatio> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", packPromotionRatio.getId());
            PackPromotionRatio packPromotionRatioById = packPromotionRatioMapper.selectOne(queryWrapper);
            if (packPromotionRatioById == null) {
                //如果旧礼包不存在则直接新增
                packPromotionRatio.setId(idGenerator.nextId());
                packPromotionRatioMapper.insert(packPromotionRatio);
            } else {
                //如果旧礼包存在则根据id更新
                packPromotionRatioMapper.updateById(packPromotionRatio);
            }
        }

        //礼包id
        Long packId = packPromotionRatio.getId();

        //礼包没有除四种抽卡资源之外内容直接返回礼包信息
        if (packPromotionRatioVO.getPackContent() == null) {
            return getPackById(packId.toString());
        }

        //礼包的额外内容
        List<PackContentVO> packContentVOList = packPromotionRatioVO.getPackContent();
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


        //构建更新的条件和更新字段
        UpdateWrapper<PackContent> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("pack_id", packId);
        updateWrapper.set("archived", true);
        packContentMapper.update(null, updateWrapper);
        //批量保存
        packContentMapperService.saveBatch(packContentList);

        return getPackById(packId.toString());
    }

    @Override
    public List<PackPromotionRatioVO> getPackPromotionRatioList() {
        List<PackPromotionRatio> packPromotionRatioList = packPromotionRatioMapper.selectList(null);
        QueryWrapper<PackContent> packContentQueryWrapper = new QueryWrapper<>();
        packContentQueryWrapper.eq("archived", false);
        List<PackContent> packContentList = packContentMapper.selectList(packContentQueryWrapper);
        Map<Long, List<PackContent>> collect = packContentList.stream().collect(Collectors.groupingBy(PackContent::getPackId));
        Map<String, Double> itemValueMap = packItemMapper.selectList(null).stream().collect(Collectors.toMap(PackItem::getId, PackItem::getValue));
        List<PackPromotionRatioVO> VOList = new ArrayList<>();
        for (PackPromotionRatio packPromotionRatio : packPromotionRatioList) {
            PackPromotionRatioVO packPromotionRatioVO = getPackVO(packPromotionRatio, collect.get(packPromotionRatio.getId()));
            VOList.add(packPromotionRatioVO);
            packPromotionRatioCalc(packPromotionRatioVO,itemValueMap);
        }
        return VOList;
    }



    @Override
    public PackPromotionRatioVO getPackById(String idStr) {
        long id = Long.parseLong(idStr);
        PackPromotionRatio packPromotionRatio = packPromotionRatioMapper.selectOne(new QueryWrapper<PackPromotionRatio>().eq("id", id));
        QueryWrapper<PackContent> packContentQueryWrapper = new QueryWrapper<>();
        packContentQueryWrapper.eq("pack_id", id).eq("archived", false);
        List<PackContent> packContentList = packContentMapper.selectList(packContentQueryWrapper);
        return getPackVO(packPromotionRatio, packContentList);
    }

    @Override
    public List<PackItem> getItemList() {
        return packItemMapper.selectList(null);

    }

    @Override
    public PackItem saveOrUpdatePackItem(PackItem newPackItem) {
        QueryWrapper<PackItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",newPackItem.getId());
        PackItem packItem = packItemMapper.selectOne(queryWrapper);
        if(packItem==null){
            newPackItem.setId(String.valueOf(idGenerator.nextId()));
            packItemMapper.insert(newPackItem);
        }else {
            packItemMapper.update(newPackItem,queryWrapper);
        }
        return newPackItem;
    }

    @Override
    public void deletePackItemById(String id) {
        QueryWrapper<PackItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",id);
        packItemMapper.delete(queryWrapper);
    }

    @Override
    public void updatePackState(String id, Integer state) {
        UpdateWrapper<PackPromotionRatio> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("state",state);
        updateWrapper.eq("id",id);
        packPromotionRatioMapper.update(null,updateWrapper);
    }


    private PackPromotionRatioVO getPackVO(PackPromotionRatio packPromotionRatio, List<PackContent> packContentList) {
        PackPromotionRatioVO packPromotionRatioVO = new PackPromotionRatioVO();
        packPromotionRatioVO.copy(packPromotionRatio);
        if (packContentList != null) {
            List<PackContentVO> packContentVOList = new ArrayList<>();
            for (PackContent packContent : packContentList) {
                PackContentVO packContentVO = new PackContentVO();
                packContentVO.copy(packContent);
                packContentVOList.add(packContentVO);
                packPromotionRatioVO.setPackContent(packContentVOList);
            }
        }

        return packPromotionRatioVO;
    }

    private void packPromotionRatioCalc(PackPromotionRatioVO packPromotionRatioVO,Map<String,Double> itemValue) {
        double eachDrawPrice = 0.0 ; //每一抽价格
        double eachOriginiumPrice= 0.0 ; //每源石（折算物资后）价格
        double promotionRatioForMoney= 0.0 ; //氪金性价比
        double promotionRatioForComprehensive= 0.0 ; //综合性价比
        double equivalentOriginium = 0.0;
        double drawCount = 0.0;

        double totalOfOrundum = packPromotionRatioVO.getOrundum()+packPromotionRatioVO.getOriginium() * 180
                +packPromotionRatioVO.getTicketGacha()*600+ packPromotionRatioVO.getTicketGacha10()*6000;

        double eachOriginalOriginiumPrice = 648/185.0;
        double eachOriginalDrawPrice = 648.0/185/0.3;

        if(totalOfOrundum>0){
            eachDrawPrice = packPromotionRatioVO.getPrice()/(totalOfOrundum/600);
            equivalentOriginium += totalOfOrundum/180;
            promotionRatioForMoney = eachOriginalDrawPrice/eachDrawPrice;
            drawCount = totalOfOrundum/600;
        }

        List<PackContentVO> packContentVOList = packPromotionRatioVO.getPackContent();


        if(packContentVOList!=null){
            double apCount = 0.0;
            for(PackContentVO packContentVO : packContentVOList){
               if(itemValue.get(packContentVO.getItemId())!=null){
                   apCount+=itemValue.get(packContentVO.getItemId())*packContentVO.getQuantity();
               }
            }
            equivalentOriginium += apCount/135;

            if(equivalentOriginium>0){
                eachOriginiumPrice = packPromotionRatioVO.getPrice()/equivalentOriginium;
                promotionRatioForComprehensive = eachOriginalOriginiumPrice/eachOriginiumPrice;
            }
        }

        packPromotionRatioVO.setDrawCount(drawCount);
        packPromotionRatioVO.setEachDrawPrice(eachDrawPrice);
        packPromotionRatioVO.setEachOriginiumPrice(eachOriginiumPrice);
        packPromotionRatioVO.setPromotionRatioForMoney(promotionRatioForMoney);
        packPromotionRatioVO.setEquivalentOriginium(equivalentOriginium);
        packPromotionRatioVO.setPromotionRatioForComprehensive(promotionRatioForComprehensive);
    }
}
