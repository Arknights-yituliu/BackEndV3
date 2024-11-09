package com.lhs.service.material;

import com.alibaba.excel.EasyExcel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileUtil;

import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.po.material.ItemIterationValue;
import com.lhs.entity.po.material.WorkShopProducts;
import com.lhs.entity.dto.material.StageParamDTO;
import com.lhs.mapper.material.ItemIterationValueMapper;
import com.lhs.mapper.material.ItemMapper;
import com.lhs.entity.po.material.Item;

import com.lhs.mapper.material.WorkShopProductsMapper;
import com.lhs.entity.dto.material.CompositeTableDTO;
import com.lhs.entity.dto.material.ItemCostDTO;

import com.lhs.entity.vo.material.ItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ItemService extends ServiceImpl<ItemMapper, Item> {


    private final ItemMapper itemMapper;

    private final ItemIterationValueMapper itemIterationValueMapper;

    private final WorkShopProductsMapper workShopProductsMapper;

    private final IdGenerator idGenerator;

    public ItemService(ItemMapper itemMapper,
                       ItemIterationValueMapper itemIterationValueMapper,
                       WorkShopProductsMapper workShopProductsMapper) {
        this.itemMapper = itemMapper;
        this.itemIterationValueMapper = itemIterationValueMapper;
        this.workShopProductsMapper = workShopProductsMapper;
        this.idGenerator = new IdGenerator(1L);
    }


    private final static double LMD_VALUE = 0.0036;

    /**
     * //根据蓝材料对应的常驻最高关卡效率En和旧蓝材料价值Vn计算新的蓝材料价值Vn+1  ，  Vn+1= Vn*1/En
     *
     * @param items 材料信息表Vn
     * @return 新的材料信息表Vn+1
     */
    @Transactional
    public List<Item> ItemValueCal(List<Item> items, StageParamDTO stageParamDTO) {



        Double expCoefficient = stageParamDTO.getExpCoefficient();
        Double lmdCoefficient = stageParamDTO.getLmdCoefficient();
        String version = stageParamDTO.getVersion();

        //上次迭代计算出的副产物价值
        Map<String, Double> itemIterationValue = getItemIterationValue(version);

        //加工站副产物价值
        Map<String, Double> workShopProductsValue = getWorkShopProductsValue(version);

        //加工站材料合成表
        List<CompositeTableDTO> compositeTableDTO = getCompositeTable();


        Map<String, Item> itemValueMap = new HashMap<>();
        for (Item item : items) {
            item.setVersion(version);
            item.setId(idGenerator.nextId());
            if(item.getCardNum()>8){
                item.setItemValue(item.getItemValueAp()*1.25);
                continue;
            }
            String itemId = item.getItemId();
            //设置经验书系数，经验书价值 = 龙门币价值 * 经验书系数
            if (itemId.equals("2004")) {
                item.setItemValueAp(LMD_VALUE * 2000 * expCoefficient);
            }
            if (itemId.equals("2003")) {
                item.setItemValueAp(LMD_VALUE * 1000 * expCoefficient);
            }
            if (itemId.equals("2002")) {
                item.setItemValueAp(LMD_VALUE * 400 * expCoefficient);
            }
            if (itemId.equals("2001")) {
                item.setItemValueAp(LMD_VALUE * 200 * expCoefficient);
            }
            if(itemId.equals("4001")){
                item.setItemValueAp(LMD_VALUE*lmdCoefficient);
            }
            //在itemValueMap 设置新的材料价值 新材料价值 = 旧材料价值/该材料主线最优关的关卡效率
            if(itemIterationValue.get(itemId)!=null){
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

        itemValueMap.forEach((k, item) -> item.setItemValue(item.getItemValueAp() * 1.25));
        saveByProductValue(items, version);  //保存新的材料价值表的加工站副产物平均产出价值
        saveBatch(items);  //更新材料表

        return items;
    }

    public void updateFixedItemValue(){

        Map<String,Double> map = new HashMap<>();

    }



    /**
     * 保存材料价值迭代系数
     * @param iterationValueList 材料价值迭代系数
     */
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
    public void deleteItemIterationValue(String version) {
        QueryWrapper<ItemIterationValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("version", version);
        itemIterationValueMapper.delete(queryWrapper);
    }

    /**
     * 获取材料表（外部API用，有缓存）
     * @param version 物品价值的版本号
     * @return 材料信息表
     */
    @RedisCacheable(key = "Item:itemValue", params = "version")
    public List<Item> getItemListCache(String version) {
        QueryWrapper<Item> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.in("version", version,"Fixed").orderByDesc("item_value_ap");
        return itemMapper.selectList(itemQueryWrapper);
    }

    /**
     * 获取材料信息表
     * @param stageParamDTO 关卡计算参数
     * @return  材料信息表
     */
    public List<Item> getItemList(StageParamDTO stageParamDTO) {
        QueryWrapper<Item> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.eq("version", stageParamDTO.getVersion());
        return itemMapper.selectList(itemQueryWrapper);
    }

    /**
     * 获取基础材料价值表
     * @return  基础材料价值表
     */
    public List<Item> getBaseItemList() {
        QueryWrapper<Item> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.eq("version", "Original");
        return itemMapper.selectList(itemQueryWrapper);
    }



    private Map<String, Double> getItemIterationValue(String version){
        //读取上次迭代计算出的副产物价值
        QueryWrapper<ItemIterationValue> iterationValueQueryWrapper = new QueryWrapper<>();
        iterationValueQueryWrapper.eq("version", version);
        List<ItemIterationValue> itemIterationValueList = itemIterationValueMapper.selectList(iterationValueQueryWrapper);

        //找不到读基础版本
        if (itemIterationValueList.isEmpty()) {
            iterationValueQueryWrapper.clear();
            iterationValueQueryWrapper.eq("version", "original");
            itemIterationValueList = itemIterationValueMapper.selectList(iterationValueQueryWrapper);
        }

        return itemIterationValueList.stream()
                .collect(Collectors.toMap(ItemIterationValue::getItemId, ItemIterationValue::getIterationValue));
    }

    private Map<String, Double> getWorkShopProductsValue(String version){

        LambdaQueryWrapper<WorkShopProducts> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkShopProducts::getVersion,version);
        List<WorkShopProducts> workShopProductsList = workShopProductsMapper.selectList(queryWrapper);

        //找不到读基础版本
        if ( workShopProductsList.isEmpty()) {
            LambdaQueryWrapper<WorkShopProducts> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(WorkShopProducts::getVersion,"original");
            workShopProductsList = workShopProductsMapper.selectList(queryWrapper1);
        }
        return workShopProductsList.stream()
                .collect(Collectors.toMap(WorkShopProducts::getItemRank, WorkShopProducts::getExpectValue));
    }

    private List<CompositeTableDTO> getCompositeTable(){
        String compositeTableText = FileUtil.read(ConfigUtil.Item + "composite_table.v2.json");
        //读取加工站材料合成表
        if(compositeTableText==null) throw new ServiceException(ResultCode.DATA_NONE);

        return JsonMapper.parseJSONArray(compositeTableText, new TypeReference<List<CompositeTableDTO>>() {
        });
    }

    /**
     * 保存加工站副产品期望价值
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
            log.info(rarity + "级材料副产物期望：" + expectValue / knockRating);
            WorkShopProducts workShopProducts = new WorkShopProducts();
            workShopProducts.setItemRank("rarity_" + rarity);  //副产物等级
            workShopProducts.setId(time++);
            workShopProducts.setVersion(version); //版本号
            workShopProducts.setExpectValue(expectValue); //副产物期望价值
            workShopProductsMapper.insert(workShopProducts); //存入表
        }
    }


}
