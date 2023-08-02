package com.lhs.service.stage;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.FileUtil;

import com.lhs.entity.stage.ItemIterationValue;
import com.lhs.entity.stage.WorkShopProducts;
import com.lhs.mapper.ItemIterationValueMapper;
import com.lhs.mapper.ItemMapper;
import com.lhs.entity.stage.Item;

import com.lhs.mapper.WorkShopProductsMapper;
import com.lhs.vo.stage.CompositeTable;
import com.lhs.vo.stage.ItemCost;

import com.lhs.vo.stage.ItemVo;
import com.lhs.vo.stage.StageAndItemCoefficient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ItemService extends ServiceImpl<ItemMapper, Item> {


    private final ItemMapper itemMapper;

    private final ItemIterationValueMapper itemIterationValueMapper;

    private final WorkShopProductsMapper workShopProductsMapper;


    public ItemService(ItemMapper itemMapper,
                       ItemIterationValueMapper itemIterationValueMapper,
                       WorkShopProductsMapper workShopProductsMapper){
        this.itemMapper = itemMapper;
        this.itemIterationValueMapper = itemIterationValueMapper;
        this.workShopProductsMapper = workShopProductsMapper;
    }


    /**
     * //根据蓝材料对应的常驻最高关卡效率En和旧蓝材料价值Vn计算新的蓝材料价值Vn+1  ，  Vn+1= Vn*1/En
     *
     * @param items               材料信息表Vn
     * @return 新的材料信息表Vn+1
     */
    @Transactional
    public List<Item> ItemValueCalculation(List<Item> items, StageAndItemCoefficient stageAndItemCoefficient) {

        double expCoefficient =  stageAndItemCoefficient.getExpCoefficient();
        String version = stageAndItemCoefficient.getType() + "-" + expCoefficient;

        //读取上次迭代根据Vn计算出的副产物价值
        QueryWrapper<ItemIterationValue> iterationValueQueryWrapper = new QueryWrapper<>();
        iterationValueQueryWrapper.eq("version",version);
        List<ItemIterationValue> itemIterationValueList = itemIterationValueMapper.selectList(iterationValueQueryWrapper);

        //找不到读默认的0.625版本
        if(itemIterationValueList.size()<1){
            iterationValueQueryWrapper.clear();
            iterationValueQueryWrapper.eq("version","public-0.625");
            itemIterationValueList = itemIterationValueMapper.selectList(iterationValueQueryWrapper);
        }

        //读取上次关卡效率计算的结果中蓝材料对应的常驻最高关卡效率En
        QueryWrapper<WorkShopProducts> workShopProductsQueryWrapper = new QueryWrapper<>();
        workShopProductsQueryWrapper.eq("version",expCoefficient);
        List<WorkShopProducts> workShopProductsList = workShopProductsMapper.selectList(workShopProductsQueryWrapper);

        //找不到读默认的0.625版本
        if(workShopProductsList.size()<1){
            workShopProductsQueryWrapper.clear();
            workShopProductsQueryWrapper.eq("version","public-0.625");
            workShopProductsList = workShopProductsMapper.selectList(workShopProductsQueryWrapper);
        }

        Map<String, Double> workShopProductsValue =
                workShopProductsList.stream().collect(Collectors.toMap(WorkShopProducts::getRank, WorkShopProducts::getExpectValue));

        String compositeTableJson = FileUtil.read(ApplicationConfig.Item + "compositeTable.json");
        List<CompositeTable> compositeTable = JSONArray.parseArray(compositeTableJson, CompositeTable.class);  //读取加工站合成表

        long tableId = new Date().getTime();

        for (Item item :items) {
            item.setVersion(version);//经验书系数
            item.setId(tableId++);
            if (item.getItemId().equals("2004")) {
                item.setItemValueAp(7.2 * expCoefficient);
            }
            if (item.getItemId().equals("2003")) {
                item.setItemValueAp(3.6 * expCoefficient);
            }
            if (item.getItemId().equals("2002")) {
                item.setItemValueAp(1.44 * expCoefficient);
            }
            if (item.getItemId().equals("2001")) {
                item.setItemValueAp(0.72 * expCoefficient);
            }
        }


        Map<String, Item> itemValueMap = items.stream().collect(Collectors.toMap(Item::getItemName, Function.identity()));  //将旧的材料Vn集合转成map方便调用


        //在itemValueMap 设置新的材料价值Vn+1 ， Vn+1= Vn*1/En
        for(ItemIterationValue itemIterationValue:itemIterationValueList){
            double itemValueNew = itemValueMap.get(itemIterationValue.getItemName()).getItemValueAp() / itemIterationValue.getIterationValue();
            itemValueMap.get(itemIterationValue.getItemName()).setItemValueAp(itemValueNew);
        }


        compositeTable.forEach(table -> {     //循环加工站合成表计算新价值
            Integer rarity = itemValueMap.get(table.getId()).getRarity();
            double itemValueNew = 0.0;
            StringBuilder message = new StringBuilder(itemValueMap.get(table.getId()).getItemName()).append(" = ( ");
            if (rarity < 3) {
                for (ItemCost itemCost : table.getItemCost()) {
                    itemValueNew = (itemValueMap.get(itemCost.getId()).getItemValueAp() +
                            workShopProductsValue.get("rarity_" + (rarity)) - 0.36 * rarity)
                            / itemCost.getCount();

                } //灰，绿色品质是向下拆解   灰，绿色材料 = 蓝材料 + 副产物 - 龙门币

            } else {
                for (ItemCost itemCost : table.getItemCost()) {
                    itemValueNew += itemValueMap.get(itemCost.getId()).getItemValueAp() * itemCost.getCount();

                }//紫，金色品质是向上合成    蓝材料  + 龙门币 - 副产物 = 紫，金色材料
                itemValueNew = itemValueNew + 0.36 * (rarity - 1) - workShopProductsValue.get("rarity_" + (rarity - 1));


            }

            itemValueMap.get(table.getId()).setItemValueAp(itemValueNew);  //存入新材料价值
        });

        QueryWrapper<Item> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.eq("version", version);
        int delete = itemMapper.delete(itemQueryWrapper);

        if (delete > -1) {
            itemValueMap.forEach((k, item) ->{
//                System.out.println(item);
                item.setItemValue(item.getItemValueAp() * 1.25);
            });
            saveByProductValue(items,version);  //保存Vn+1的加工站副产物平均产出价值
            saveBatch(items);  //更新材料表
        }


        return items;


    }

    /**
     * 保存加工站副产品期望价值
     *
     * @param items 新材料信息表Vn+1
     * @param version 版本
     */
    public void saveByProductValue(List<Item> items,String version) {
        double knockRating = 0.2;

        QueryWrapper<WorkShopProducts> workShopProductsQueryWrapper = new QueryWrapper<>();
        workShopProductsQueryWrapper.eq("version",version);
        System.out.println(version);
        int delete = workShopProductsMapper.delete(workShopProductsQueryWrapper);
        if(delete>-1){
            Map<Integer, List<Item>> collect = items.stream()
                    .filter(item -> item.getWeight() > 0)
                    .collect(Collectors.groupingBy(Item::getRarity));

            long time = new Date().getTime();

            for(Integer rarity :collect.keySet()){
                List<Item> list = collect.get(rarity);
                double expectValue = list.stream().mapToDouble(item -> item.getItemValueAp() * item.getWeight()/100).sum()* knockRating;
                WorkShopProducts workShopProducts = new WorkShopProducts();
                workShopProducts.setRank("rarity_" + rarity);
                workShopProducts.setId(time++);
                workShopProducts.setVersion(version);
                workShopProducts.setExpectValue(expectValue);

                workShopProductsMapper.insert(workShopProducts);
            }
        }

    }


    public void saveItemIterationValue(List<ItemIterationValue> iterationValueList){
        long time = new Date().getTime();
        for(ItemIterationValue iterationValue:iterationValueList){
            iterationValue.setId(time++);
            itemIterationValueMapper.insert(iterationValue);
        }
    }

    public Integer deleteItemIterationValue(String version){
        QueryWrapper<ItemIterationValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("version",version);
        return itemIterationValueMapper.delete(queryWrapper);
    }

    /**
     * 获取材料信息表
     * @param version 经验书系数，一般为0.625（还有1.0、0.73和0.0）
     * @return 材料信息表
     */
    @RedisCacheable(key = "itemValue#version")
    public List<Item> getItemListCache(String version) {
        QueryWrapper<Item> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.eq("version", version).orderByDesc("item_value_ap");
        return itemMapper.selectList(itemQueryWrapper);
    }


    public List<Item> queryItemList(String version) {
        QueryWrapper<Item> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.eq("version",version);
        return itemMapper.selectList(itemQueryWrapper);
    }


    public void exportItemExcel(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("itemValue", "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

            List<Item> list = getItemListCache("public-0.625");
            List<ItemVo> itemVoList = new ArrayList<>();
            for (Item item : list) {
                ItemVo itemVo = new ItemVo();
                BeanUtils.copyProperties(item, itemVo);
                itemVoList.add(itemVo);
            }

            EasyExcel.write(response.getOutputStream(), ItemVo.class).sheet("Sheet1").doWrite(itemVoList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportItemJson(HttpServletResponse response) {
        List<Item> list = getItemListCache("public-0.625");
        List<ItemVo> itemVoList = new ArrayList<>();
        for (Item item : list) {
            ItemVo itemVo = new ItemVo();
            BeanUtils.copyProperties(item, itemVo);
            itemVoList.add(itemVo);
        }
        String jsonForMat = JSON.toJSONString(JSONArray.parseArray(JSON.toJSONString(itemVoList)), SerializerFeature.PrettyFormat,
                SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullListAsEmpty);
        FileUtil.save(response, ApplicationConfig.Item, "ItemValue.json", jsonForMat);
    }


}
