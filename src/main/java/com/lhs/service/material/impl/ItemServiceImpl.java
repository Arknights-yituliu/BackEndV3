package com.lhs.service.material.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.material.CompositeTableDTO;
import com.lhs.entity.dto.material.FloatingValueItem;
import com.lhs.entity.dto.material.ItemCostDTO;
import com.lhs.entity.dto.material.StageParamDTO;
import com.lhs.entity.po.material.Item;
import com.lhs.entity.po.material.ItemIterationValue;
import com.lhs.entity.po.material.WorkShopProducts;
import com.lhs.mapper.material.ItemIterationValueMapper;
import com.lhs.mapper.material.ItemMapper;
import com.lhs.mapper.material.WorkShopProductsMapper;
import com.lhs.mapper.material.service.ItemMapperService;
import com.lhs.service.material.ItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemMapper itemMapper;

    private final ItemIterationValueMapper itemIterationValueMapper;

    private final WorkShopProductsMapper workShopProductsMapper;

    private final ItemMapperService itemMapperService;

    private final IdGenerator idGenerator;

    public ItemServiceImpl(ItemMapper itemMapper,
                           ItemIterationValueMapper itemIterationValueMapper,
                           WorkShopProductsMapper workShopProductsMapper, ItemMapperService itemMapperService) {
        this.itemMapper = itemMapper;
        this.itemIterationValueMapper = itemIterationValueMapper;
        this.workShopProductsMapper = workShopProductsMapper;
        this.itemMapperService = itemMapperService;
        this.idGenerator = new IdGenerator(1L);
    }


//    private final static double LMD_VALUE = 0.0036;

    /**
     * //根据蓝材料对应的常驻最高关卡效率En和旧蓝材料价值Vn计算新的蓝材料价值Vn+1  ，  Vn+1= Vn*1/En
     *
     * @param items 材料信息表Vn
     * @return 新的材料信息表Vn+1
     */
    @Transactional
    @Override
    public List<Item> ItemValueCal(List<Item> items, StageParamDTO stageParamDTO) {

        Double lmdValue = stageParamDTO.getLMDValue();
        Double EXPValue = stageParamDTO.getEXPValue();
        String version = stageParamDTO.getVersion();

        //上次迭代计算出的副产物价值
        Map<String, Double> itemIterationValue = getItemIterationValue(version);

        //加工站副产物价值
        Map<String, Double> workShopProductsValue = getWorkShopProductsValue(version);

        //加工站材料合成表
        List<CompositeTableDTO> compositeTableDTO = getCompositeTable();

        Map<String, Item> itemValueMap = new HashMap<>();

        for (Item item : items) {
            if("FIXED".equals(item.getVersion())||"CHIP".equals(item.getVersion())){
                continue;
            }

            item.setId(idGenerator.nextId());
            item.setVersion(version);

            String itemId = item.getItemId();
            //设置经验书系数，经验书价值 = 龙门币价值 * 经验书系数
            if (itemId.equals("2004")) {
                item.setItemValueAp(EXPValue * 2000);
            }
            if (itemId.equals("2003")) {
                item.setItemValueAp(EXPValue * 1000);
            }
            if (itemId.equals("2002")) {
                item.setItemValueAp(EXPValue * 400);
            }
            if (itemId.equals("2001")) {
                item.setItemValueAp(EXPValue * 200);
            }
            if (itemId.equals("4001")) {
                item.setItemValueAp(lmdValue);
            }
            //在itemValueMap 设置新的材料价值 新材料价值 = 旧材料价值/该材料主线最优关的关卡效率
            if (itemIterationValue.get(itemId) != null) {
                item.setItemValueAp(item.getItemValueAp() / itemIterationValue.get(itemId));
            }
            itemValueMap.put(itemId, item);
        }

        //根据加工站合成表计算新价值
        compositeTableDTO.forEach(table -> {
            Item item = itemValueMap.get(table.getId());

            Integer rarity = item.getRarity();
            double itemValueNew = 0.0;
            if (table.getResolve()) {
                //灰，绿色品质是向下拆解   灰，绿色材料 = （蓝材料价值 + 副产物 - 龙门币）/合成蓝材料的所需灰绿材料数量
                for (ItemCostDTO itemCostDTO : table.getPathway()) {
                    itemValueNew = (itemValueMap.get(itemCostDTO.getId()).getItemValueAp() +
                            workShopProductsValue.get("rarity_" + rarity) - 0.36 * rarity) / itemCostDTO.getCount();
                }
            } else {
                //紫，金色品质是向上合成    紫，金色材料 =  合成所需蓝材料价值之和  + 龙门币 - 副产物
                for (ItemCostDTO itemCostDTO : table.getPathway()) {
                    itemValueNew += itemValueMap.get(itemCostDTO.getId()).getItemValueAp() * itemCostDTO.getCount();
                }
                itemValueNew = itemValueNew + 0.36 * (rarity - 1) - workShopProductsValue.get("rarity_" + (rarity - 1));
            }

            item.setItemValueAp(itemValueNew);  //存入新材料价值
        });

        QueryWrapper<Item> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.eq("version", version);
        int delete = itemMapper.delete(itemQueryWrapper);
        items = items.stream().filter(e->version.equals(e.getVersion())).toList();
        saveByProductValue(items, version);  //保存新的材料价值表的加工站副产物平均产出价值
        itemMapperService.saveBatch(items);  //更新材料表

        return items;
    }


    /**
     * 保存材料价值迭代系数
     *
     * @param iterationValueList 材料价值迭代系数
     */
    @Override
    public void saveItemIterationValue(List<ItemIterationValue> iterationValueList) {
        long time = new Date().getTime();
        for (ItemIterationValue iterationValue : iterationValueList) {
            iterationValue.setId(time++);
            itemIterationValueMapper.insert(iterationValue);
        }
    }

    /**
     * 删除材料价值迭代系数
     *
     * @param version 材料价值版本
     */
    @Override
    public void deleteItemIterationValue(String version) {
        QueryWrapper<ItemIterationValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("version", version);
        itemIterationValueMapper.delete(queryWrapper);
    }



    /**
     * 获取材料表（外部API用，有缓存）
     *
     * @param stageParamDTO 关卡参数，用于拼接版本号
     * @return 材料信息表
     */
    @RedisCacheable(key = "Item:itemValue", params = "version")
    @Override
    public List<Item> getItemListCache(StageParamDTO stageParamDTO) {
        LambdaQueryWrapper<Item> itemQueryWrapper = new LambdaQueryWrapper<>();
        itemQueryWrapper.in(Item::getVersion, stageParamDTO.getVersion()).orderByDesc(Item::getItemValueAp);
        List<Item> itemList = itemMapper.selectList(itemQueryWrapper);
        List<Item> floatingValueItemList = updateFloatingValueItem(stageParamDTO);
        itemList.addAll(floatingValueItemList);
        return itemList;
    }

    /**
     * 获取材料信息表
     *
     * @param stageParamDTO 关卡计算参数
     * @return 材料信息表
     */
    @Override
    public List<Item> getItemList(StageParamDTO stageParamDTO) {
        LambdaQueryWrapper<Item> itemQueryWrapper = new LambdaQueryWrapper<>();
        itemQueryWrapper.eq(Item::getVersion, stageParamDTO.getVersion());
        List<Item> itemList = itemMapper.selectList(itemQueryWrapper);
        if(itemList.isEmpty()){
            itemQueryWrapper.clear();
            itemQueryWrapper.eq(Item::getVersion, "ORIGINAL");
            itemList = itemMapper.selectList(itemQueryWrapper);
        }

        List<Item> floatingValueItemList = updateFloatingValueItem(stageParamDTO);
        itemList.addAll(floatingValueItemList);
        return itemList;
    }

    /**
     * 获取基础材料价值表
     *
     * @return 基础材料价值表
     */
    @Override
    public List<Item> getBaseItemList() {
        LambdaQueryWrapper<Item> itemQueryWrapper = new LambdaQueryWrapper<>();
        itemQueryWrapper.eq(Item::getVersion, "ORIGINAL");
        return itemMapper.selectList(itemQueryWrapper);
    }





    @Override
    public List<Item> updateFloatingValueItem(StageParamDTO stageParamDTO) {
        LambdaQueryWrapper<Item> itemLambdaQueryWrapper = new LambdaQueryWrapper<>();
        itemLambdaQueryWrapper.in(Item::getVersion, "FIXED","CHIP");
        List<Item> itemList = itemMapper.selectList(itemLambdaQueryWrapper);

        //龙门币价值
        Double LMDValue = stageParamDTO.getLMDValue();
        //经验书价值
        Double EXPValue = stageParamDTO.getEXPValue();

        FloatingValueItem floatingValueItem = new FloatingValueItem();
        Map<String, Double> floatingValueItemMap = floatingValueItem.initValue(LMDValue, EXPValue, true);

        for(Item item:itemList){
            String itemId = item.getItemId();
            if(floatingValueItemMap.get(itemId)!=null){
                item.setItemValueAp(floatingValueItemMap.get(itemId));
            }
        }

        return itemList;
    }




    private Map<String, Double> getItemIterationValue(String version) {
        //读取上次迭代计算出的副产物价值
        QueryWrapper<ItemIterationValue> iterationValueQueryWrapper = new QueryWrapper<>();
        iterationValueQueryWrapper.eq("version", version);
        List<ItemIterationValue> itemIterationValueList = itemIterationValueMapper.selectList(iterationValueQueryWrapper);

        //找不到读基础版本
        if (itemIterationValueList.isEmpty()) {
            iterationValueQueryWrapper.clear();
            iterationValueQueryWrapper.eq("version", "ORIGINAL");
            itemIterationValueList = itemIterationValueMapper.selectList(iterationValueQueryWrapper);
        }

        return itemIterationValueList.stream()
                .collect(Collectors.toMap(ItemIterationValue::getItemId, ItemIterationValue::getIterationValue));
    }

    private Map<String, Double> getWorkShopProductsValue(String version) {

        LambdaQueryWrapper<WorkShopProducts> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkShopProducts::getVersion, version);
        List<WorkShopProducts> workShopProductsList = workShopProductsMapper.selectList(queryWrapper);

        //找不到读基础版本
        if (workShopProductsList.isEmpty()) {
            LambdaQueryWrapper<WorkShopProducts> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(WorkShopProducts::getVersion, "ORIGINAL");
            workShopProductsList = workShopProductsMapper.selectList(queryWrapper1);
        }
        return workShopProductsList.stream()
                .collect(Collectors.toMap(WorkShopProducts::getItemRank, WorkShopProducts::getExpectValue));
    }

    private List<CompositeTableDTO> getCompositeTable() {
        String compositeTableText = FileUtil.read(ConfigUtil.Item + "composite_table.v2.json");
        //读取加工站材料合成表
        if (compositeTableText == null) throw new ServiceException(ResultCode.DATA_NONE);

        return JsonMapper.parseJSONArray(compositeTableText, new TypeReference<List<CompositeTableDTO>>() {
        });
    }

    /**
     * 保存加工站副产品期望价值
     *
     * @param items   新材料信息表Vn+1
     * @param version 版本
     */
    private void saveByProductValue(List<Item> items, String version) {
        double knockRating = 0.2; //副产物爆率

        QueryWrapper<WorkShopProducts> workShopProductsQueryWrapper = new QueryWrapper<>();
        workShopProductsQueryWrapper.eq("version", version);
        int delete = workShopProductsMapper.delete(workShopProductsQueryWrapper);

        Map<Integer, List<Item>> collect = items.stream()
                .filter(item -> item.getWeight() > 0)
                .collect(Collectors.groupingBy(Item::getRarity));

        long time = new Date().getTime();

        for (Integer rarity : collect.keySet()) {
            List<Item> list = collect.get(rarity);
            //副产物期望 = 所有材料的期望价值（材料价值 * 材料出率 /100）之和 * 副产物爆率
            double expectValue = list.stream().mapToDouble(item -> item.getItemValueAp() * item.getWeight()).sum() * knockRating;
            Logger.info(rarity + "级材料副产物期望：" + expectValue / knockRating);
            WorkShopProducts workShopProducts = new WorkShopProducts();
            workShopProducts.setItemRank("rarity_" + rarity);  //副产物等级
            workShopProducts.setId(time++);
            workShopProducts.setVersion(version); //版本号
            workShopProducts.setExpectValue(expectValue); //副产物期望价值
            workShopProductsMapper.insert(workShopProducts); //存入表
        }
    }


}
