package com.lhs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.util.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.stage.StoreAct;
import com.lhs.mapper.StoreActMapper;
import com.lhs.mapper.StorePermMapper;
import com.lhs.entity.stage.Item;
import com.lhs.entity.stage.StorePerm;
import com.lhs.service.dto.ItemCustomValueVo;
import com.lhs.service.dto.StoreActJsonVo;
import com.lhs.service.dto.StoreItem;
import com.lhs.service.vo.StoreActVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 更新常驻商店性价比
     */
    @Transactional
    public void updateStorePerm() {

        List<StorePerm> storePerms = storePermMapper.selectList(null);
        Map<String, Item> collect = itemService.queryItemListById(0.625, 200).stream().collect(Collectors.toMap(Item::getItemName, Function.identity()));
        System.out.println(collect);
        storePerms.forEach(storePerm -> {
            storePerm.setCostPer(collect.get(storePerm.getItemName()).getItemValueAp() * storePerm.getQuantity() / storePerm.getCost());
            if ("grey".equals(storePerm.getStoreType())) storePerm.setCostPer(storePerm.getCostPer() * 100);
            storePerm.setRarity(collect.get(storePerm.getItemName()).getRarity());
            storePerm.setItemId(collect.get(storePerm.getItemName()).getItemId());
        });
        Map<String, List<StorePerm>> resultMap = storePerms.stream().collect(Collectors.groupingBy(StorePerm::getStoreType));
        resultMap.forEach((k, list) -> list.sort(Comparator.comparing(StorePerm::getCostPer).reversed()));
//        List<List<StorePerm>> resultList = Arrays.asList(resultMap.get("green"),resultMap.get("yellow"),resultMap.get("orange"),resultMap.get("purple"),resultMap.get("grey"));
        redisTemplate.opsForValue().set("store/perm", resultMap);
        String saveDate = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date());

        FileUtil.save(ConfigUtil.Backup, "permStore " + saveDate + ".json", JSON.toJSONString(resultMap));
        updateBatchById(storePerms);
    }


    @RedisCacheable(key = "StorePerm")
    public Map<String, List<StorePerm>> getStorePerm(){
        List<StorePerm> storePerms = storePermMapper.selectList(null);
        Map<String, List<StorePerm>> resultMap = storePerms.stream().collect(Collectors.groupingBy(StorePerm::getStoreType));
        resultMap.forEach((k, list) -> list.sort(Comparator.comparing(StorePerm::getCostPer).reversed()));
        return  resultMap;
    }

    public void updateActStoreByJson(MultipartFile file) {

        List<Item> items = itemService.queryItemListById(0.625, 200);
        Map<String, Item> itemMap = items.stream().collect(Collectors.toMap(Item::getItemName, Function.identity()));
        JSONArray storeActDataList = JSONArray.parseArray(FileUtil.read(file));

        JSONArray storeActDataVo = new JSONArray();
        if (storeActDataList != null) {
            storeActDataList.forEach(obj -> {
                JSONObject storeActData = JSONObject.parseObject(obj.toString());
                List<StoreActJsonVo> actStores = JSONArray.parseArray(storeActData.getString("actStore"), StoreActJsonVo.class);
                actStores.forEach(actStore -> {
                    actStore.setItemPPR(itemMap.get(actStore.getItemName()).getItemValueAp() * actStore.getItemQuantity() / actStore.getItemPrice());
                    actStore.setItemId(itemMap.get(actStore.getItemName()).getItemId());
                });
                actStores.sort(Comparator.comparing(StoreActJsonVo::getItemPPR).reversed());
                storeActData.put("actStore", actStores);
                storeActDataVo.add(storeActData);
            });
        }
        String saveDate = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date());

        redisTemplate.opsForValue().set("store/act", storeActDataVo);
        FileUtil.save(ConfigUtil.Backup, "actStore " + saveDate + ".json", JSON.toJSONString(storeActDataVo));
    }


    public void updateStorePackByJson(String packStr) {

        String fileStr = FileUtil.read(ConfigUtil.Item + "itemCustomValue.json");
        if (fileStr == null) throw new ServiceException(ResultCode.DATA_NONE);

        List<ItemCustomValueVo> itemCustomValues = JSONArray.parseArray(fileStr, ItemCustomValueVo.class);
        Map<String, Double> itemMap = itemCustomValues.stream().collect(Collectors.toMap(ItemCustomValueVo::getItemName, ItemCustomValueVo::getItemValue));
        itemService.queryItemListById(0.625, 1000).forEach(item -> itemMap.put(item.getItemName(), item.getItemValueAp()));

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
            String saveDate = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date());

            redisTemplate.opsForValue().set("store/pack", packResultList);
            FileUtil.save(ConfigUtil.Backup, "packStore_" + saveDate + ".json", JSON.toJSONString(packResultList));

        }
    }


    public void updateActStoreByActName(StoreActVo storeActVo)  {
        List<Item> items = itemService.queryItemListById(0.625, 200);
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

        String saveDate = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date());

        FileUtil.save(ConfigUtil.Backup, "actStore " + saveDate + ".json", JSON.toJSONString(storeActVo));
    }

    @RedisCacheable(key = "StoreAct")
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


}
