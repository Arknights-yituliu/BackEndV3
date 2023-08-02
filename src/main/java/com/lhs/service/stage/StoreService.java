package com.lhs.service.stage;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.other.HoneyCake;
import com.lhs.entity.stage.StoreAct;
import com.lhs.mapper.HoneyCakeMapper;
import com.lhs.mapper.StoreActMapper;
import com.lhs.mapper.StorePermMapper;
import com.lhs.entity.stage.Item;
import com.lhs.entity.stage.StorePerm;
import com.lhs.service.dev.OSSService;
import com.lhs.vo.stage.ItemCustomValue;
import com.lhs.vo.stage.StoreItem;
import com.lhs.vo.stage.StoreActVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StoreService extends ServiceImpl<StorePermMapper, StorePerm> {

    @Resource
    private StorePermMapper storePermMapper;
    @Resource
    private StoreActMapper storeActMapper;
    @Resource
    private ItemService itemService;
    @Resource
    private HoneyCakeMapper honeyCakeMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private OSSService ossService;

    
    /**
     * 更新常驻商店性价比
     */
    @Scheduled(cron = "0 0 0/1 * * ?")
    @Transactional
    public void updateStorePerm() {
        List<StorePerm> storePerms = storePermMapper.selectList(null);
        Map<String, Item> collect = itemService.getItemListCache("public-"+0.625)
                .stream()
                .collect(Collectors.toMap(Item::getItemName, Function.identity()));

        storePerms.forEach(storePerm -> {
            storePerm.setCostPer(collect.get(storePerm.getItemName()).getItemValueAp() * storePerm.getQuantity() / storePerm.getCost());
            if ("grey".equals(storePerm.getStoreType())) storePerm.setCostPer(storePerm.getCostPer() * 100);
            storePerm.setRarity(collect.get(storePerm.getItemName()).getRarity());
            storePerm.setItemId(collect.get(storePerm.getItemName()).getItemId());
        });
        updateBatchById(storePerms);

        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()); // 设置日期格式
        ossService.upload(JSON.toJSONString(storePerms), "store/" + yyyyMMdd + "/perm " + yyyyMMddHHmm + ".json");

    }


    @RedisCacheable(key = "StorePerm",timeout=86400)
    public Map<String, List<StorePerm>> getStorePerm(){
        List<StorePerm> storePerms = storePermMapper.selectList(null);
        Map<String, List<StorePerm>> resultMap = storePerms.stream().collect(Collectors.groupingBy(StorePerm::getStoreType));
        resultMap.forEach((k, list) -> list.sort(Comparator.comparing(StorePerm::getCostPer).reversed()));
        return  resultMap;
    }

    public String updateActStoreByActName(StoreActVo storeActVo,Boolean level)  {
        List<Item> items = itemService.getItemListCache("public-"+0.625);
        Map<String, Item> itemMap = items.stream().collect(Collectors.toMap(Item::getItemName, Function.identity()));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<StoreItem> storeItemList = storeActVo.getActStore();
        String actName = storeActVo.getActName();
        String actEndDate = storeActVo.getActEndDate();

        Date date = new Date();
        try {
            date = sdf.parse(actEndDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        storeItemList.forEach(a -> {
            a.setItemPPR(itemMap.get(a.getItemName()).getItemValueAp() * a.getItemQuantity() / a.getItemPrice());
            a.setItemId(itemMap.get(a.getItemName()).getItemId());
        });

        storeItemList.sort(Comparator.comparing(StoreItem::getItemPPR).reversed());

        StoreAct act_name = storeActMapper.selectOne(new QueryWrapper<StoreAct>().eq("act_name", actName));
        StoreAct build = StoreAct.builder().actName(actName).endTime(date).result(JSON.toJSONString(storeActVo)).build();
        if(act_name==null){
            storeActMapper.insert(build);
        }else {
            storeActMapper.updateById(build);
        }

        String message = "活动商店已更新";

        if(level) {
            redisTemplate.delete("StoreAct");
            log.info("清空活动商店缓存");
            message = "活动商店已更新，并清空缓存";

        }

        String yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 设置日期格式
        String yyyyMMddHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()); // 设置日期格式
        ossService.upload(JSON.toJSONString(storeActVo), "store/" + yyyyMMdd + "/act " + yyyyMMddHHmm + ".json");
        return message;
    }

    @RedisCacheable(key = "StoreAct",timeout=86400)
    public List<StoreActVo> getStoreAct(){
        List<StoreAct> storeActs = storeActMapper.selectList(null);
        List<StoreActVo> storeActVoList = new ArrayList<>();
        storeActs.forEach(l->{
            String result = l.getResult();
            if(l.getEndTime().getTime()>new Date().getTime()){
                StoreActVo storeActVo = JSONObject.parseObject(result, StoreActVo.class);
                storeActVoList.add(storeActVo);
            }
        });
        return  storeActVoList;
    }

    public List<StoreActVo> selectActStoreHistory() {
        List<StoreAct> storeActs = storeActMapper.selectList(null);
        List<StoreActVo> storeActVoList = new ArrayList<>();
        storeActs.forEach(l->{
            String result = l.getResult();
            StoreActVo storeActVo = JSONObject.parseObject(result, StoreActVo.class);
            storeActVoList.add(storeActVo);
        });
        return storeActVoList;
    }



    public void updateStorePackByJson(String packStr) {

        String fileStr = FileUtil.read(ApplicationConfig.Item + "itemCustomValue.json");
        if (fileStr == null) throw new ServiceException(ResultCode.DATA_NONE);

        List<ItemCustomValue> itemCustomValues = JSONArray.parseArray(fileStr, ItemCustomValue.class);
        Map<String, Double> itemMap = itemCustomValues.stream().collect(Collectors.toMap(ItemCustomValue::getItemName, ItemCustomValue::getItemValue));
        itemService.getItemListCache("public-"+0.625).forEach(item -> itemMap.put(item.getItemName(), item.getItemValueAp()));

        JSONArray packList = JSONArray.parseArray(packStr);

        List<Object> packResultList = new ArrayList<>();

        double standard_gacha = 648 / 55.5;
        double standard_ap = 3.5;

        if (packList != null) {
            packList.forEach(obj -> {
                JSONObject packData = JSONObject.parseObject(obj.toString());

                double apValueToOriginium = 0.0;  //材料理智折合源石

                double packOriginium = 0.0; // 礼包折合源石
                double packDraw = 0.0; // 共计多少抽
                double packRmbPerDraw = 0.0; // 每抽价格
                double packRmbPerOriginium = 0.0; // 每石价格
                double packPPRDraw = 0.0; // 每抽性价比，相比648
                double packPPROriginium = 0.0; // 每石性价比，相比648

                int gachaOrundum = 0;  //合成玉
                int gachaOriginium = 0;  //源石
                int gachaPermit = 0;  //单抽
                int gachaPermit10 = 0;   //十连

                if (packData.get("gachaOrundum") != null) {
                    gachaOrundum = Integer.parseInt(packData.getString("gachaOrundum")); // 合成玉
                }
                if (packData.get("gachaOriginium") != null) {
                    gachaOriginium = Integer.parseInt(packData.getString("gachaOriginium"));// 源石
                }
                if (packData.get("gachaPermit") != null) {
                    gachaPermit = Integer.parseInt(packData.getString("gachaPermit"));// 单抽
                }
                if (packData.get("gachaPermit10") != null) {
                    gachaPermit10 = Integer.parseInt(packData.getString("gachaPermit10"));// 十连
                }

                int packPrice = Integer.parseInt(packData.getString("packPrice"));// 价格


                //计算该理智的材料总理智折合源石


                if (packData.get("packContent") != null) {

                    JSONArray packContents = JSONArray.parseArray(packData.getString("packContent"));
                    for (Object contentObj : packContents) {
                        JSONObject packContent = JSONObject.parseObject(contentObj.toString());

                        apValueToOriginium += itemMap.get(packContent.getString("packContentItem")) / 1.25 * Integer.parseInt(packContent.getString("packContentQuantity"));
                    }

                    if (apValueToOriginium > 0.0) apValueToOriginium = apValueToOriginium / 135;
                }

                packDraw = gachaOrundum / 600.0 + gachaOriginium * 0.3 + gachaPermit + gachaPermit10 * 10;
                packRmbPerDraw = packPrice / packDraw;
                packOriginium = gachaOrundum / 180.0 + gachaOriginium + gachaPermit * 600 / 180.0 + gachaPermit10 * 6000 / 180.0 + apValueToOriginium;
                packRmbPerOriginium = packPrice / packOriginium;
                packPPRDraw = standard_gacha / packRmbPerDraw;
                packPPROriginium = standard_ap / packRmbPerOriginium;

                packData.put("packDraw", packDraw);
                packData.put("packRmbPerDraw", packRmbPerDraw);
                packData.put("packOriginium", packOriginium);
                packData.put("packRmbPerOriginium", packRmbPerOriginium);
                packData.put("packPPRDraw", packPPRDraw);
                packData.put("packPPROriginium", packPPROriginium);
                packResultList.add(packData);

            });

            redisTemplate.opsForValue().set("store/pack", packResultList);

        }
    }

    public void updateHoneyCake(List<HoneyCake> honeyCakeList) {

        for(HoneyCake honeyCake:honeyCakeList){
            System.out.println(honeyCake);
            QueryWrapper<HoneyCake> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", honeyCake.getName());
            HoneyCake honeyCakeOld = honeyCakeMapper.selectOne(queryWrapper);
             if(honeyCakeOld==null){
                 honeyCakeMapper.insert(honeyCake);
             }else {
                 honeyCakeMapper.updateById(honeyCake);
             }
        }
    }



//    @RedisCacheable(key = "HoneyCake",timeout=86400)
    public Map<String,HoneyCake>  getHoneyCake() {
        List<HoneyCake> honeyCakeList = getHoneyCakeList();
        return honeyCakeList.stream().collect(Collectors.toMap(HoneyCake::getName, Function.identity()));
    }

    public List<HoneyCake> getHoneyCakeList() {
        QueryWrapper<HoneyCake> honeyCakeQueryWrapper = new QueryWrapper<>();
        honeyCakeQueryWrapper.orderByDesc("start");
        return honeyCakeMapper.selectList(honeyCakeQueryWrapper);
    }
}
