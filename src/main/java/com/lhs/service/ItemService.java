package com.lhs.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.util.ConfigUtil;
import com.lhs.common.util.FileUtil;

import com.lhs.mapper.ItemMapper;
import com.lhs.entity.stage.Item;

import com.lhs.service.dto.CompositeTableDto;
import com.lhs.service.dto.ItemCost;

import com.lhs.service.vo.ItemVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ItemService extends ServiceImpl<ItemMapper, Item> {

    @Resource
    private ItemMapper itemMapper;

    /**
     * //根据蓝材料对应的常驻最高关卡效率En和旧蓝材料价值Vn计算新的蓝材料价值Vn+1  ，  Vn+1= Vn*1/En
     *
     * @param items               材料信息表Vn
     * @return 新的材料信息表Vn+1
     */
    @Transactional
    public List<Item> ItemValueCalculation(List<Item> items, Double expCoefficient) {

        String itemNameAndBestStageEffSrt = FileUtil.read(ConfigUtil.Item + "itemAndBestStageEff" + expCoefficient + ".json");
        if(itemNameAndBestStageEffSrt ==null){
            itemNameAndBestStageEffSrt = FileUtil.read(ConfigUtil.Item + "itemAndBestStageEff0.625.json");
        }
        JSONObject itemNameAndBestStageEff = JSONObject.parseObject(itemNameAndBestStageEffSrt); //读取上次关卡效率计算的结果中蓝材料对应的常驻最高关卡效率En

        String workShopProductsValueJson = FileUtil.read(ConfigUtil.Item + "workShopProductsValue"+expCoefficient+".json");
        if(workShopProductsValueJson ==null){
            workShopProductsValueJson = FileUtil.read(ConfigUtil.Item + "workShopProductsValue0.625.json");
        }
        JSONObject workShopProductsValue = JSONObject.parseObject(workShopProductsValueJson);  //读取根据Vn计算出的副产物价值

        String compositeTableJson = FileUtil.read(ConfigUtil.Item + "compositeTable.json");
        List<CompositeTableDto> compositeTableDto = JSONArray.parseArray(compositeTableJson, CompositeTableDto.class);  //读取加工站合成表

        long tableId = new Date().getTime();

        for (Item item :items) {
            item.setExpCoefficient(expCoefficient);//经验书系数
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
        itemNameAndBestStageEff.forEach((itemName, En) -> {
            double itemValueNew = itemValueMap.get(itemName).getItemValueAp() * 1 / Double.parseDouble(String.valueOf(En));
            itemValueMap.get(itemName).setItemValueAp(itemValueNew);
        });


        compositeTableDto.forEach(table -> {     //循环加工站合成表计算新价值
            Integer rarity = itemValueMap.get(table.getId()).getRarity();
            double itemValueNew = 0.0;
            StringBuilder message = new StringBuilder(itemValueMap.get(table.getId()).getItemName()).append(" = ( ");
            if (rarity < 3) {
                for (ItemCost itemCost : table.getItemCost()) {
                    itemValueNew = (itemValueMap.get(itemCost.getId()).getItemValueAp() +
                            Double.parseDouble(workShopProductsValue.getString("rarity_" + (rarity))) - 0.36 * rarity)
                            / itemCost.getCount();

//                        message.append(itemValueMap.get(itemCost.getId()).getItemValueAp())
//                                 .append(" + ")
//                                 .append(Double.parseDouble(workShopProductsValue.getString("rarity_"+(rarity))))
//                                 .append(" - ")
//                                 .append(0.36*rarity)
//                                 .append(" ) / ")
//                                 .append(itemCost.getCount());
                } //灰，绿色品质是向下拆解   灰，绿色材料 = 蓝材料 + 副产物 - 龙门币

            } else {
                for (ItemCost itemCost : table.getItemCost()) {
                    itemValueNew += itemValueMap.get(itemCost.getId()).getItemValueAp() * itemCost.getCount();

//                        message.append(itemValueMap.get(itemCost.getId()).getItemValueAp())
//                                .append(" * ")
//                                .append(itemCost.getCount())
//                                .append(" + ");

                }//紫，金色品质是向上合成    蓝材料  + 龙门币 - 副产物 = 紫，金色材料
                itemValueNew = itemValueNew + 0.36 * (rarity - 1) - Double.parseDouble(workShopProductsValue.getString("rarity_" + (rarity - 1)));

                message.append(" ) + ").append(0.36 * (rarity - 1)).append(" - ")
                        .append(Double.parseDouble(workShopProductsValue.getString("rarity_" + (rarity - 1))));
            }

            itemValueMap.get(table.getId()).setItemValueAp(itemValueNew);  //存入新材料价值
        });

        QueryWrapper<Item> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.eq("exp_coefficient", expCoefficient);
        int delete = itemMapper.delete(itemQueryWrapper);

        if (delete > -1) {
            itemValueMap.forEach((k, item) ->{
//                System.out.println(item);
                item.setItemValue(item.getItemValueAp() * 1.25);
            });
            saveByProductValue(items,expCoefficient);  //保存Vn+1的加工站副产物平均产出价值
            saveBatch(items);  //更新材料表
        }


        return items;


    }

    /**
     * 保存加工站副产品期望价值
     *
     * @param items 新材料信息表Vn+1
     */
    public void saveByProductValue(List<Item> items,Double expCoefficient) {
        double knockRating = 0.2;
        HashMap<Object, Object> hashMap = new HashMap<>();
        items.stream()
                .filter(item -> item.getWeight() > 0)
                .collect(Collectors.groupingBy(Item::getRarity))
                .forEach((rarity, list) -> {
                    hashMap.put("rarity_" + rarity, list.stream().mapToDouble(item -> item.getItemValueAp() * item.getWeight())
                            .sum() / items.size() * knockRating);
                });
        FileUtil.save(ConfigUtil.Item, "workShopProductsValue"+expCoefficient+".json", JSON.toJSONString(hashMap));

    }

    /**
     * 获取材料信息表
     * @param expCoefficient 经验书系数，一般为0.625（还有1.0、0.73和0.0）
     * @return 材料信息表
     */
    @RedisCacheable(key = "itemValue#expCoefficient")
    public List<Item> queryItemListCache(Double expCoefficient) {
        QueryWrapper<Item> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.eq("exp_coefficient", expCoefficient).orderByDesc("item_value_ap");
        return itemMapper.selectList(itemQueryWrapper);
    }

    public List<Item> queryItemList(Double expCoefficient) {
        QueryWrapper<Item> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.eq("exp_coefficient", expCoefficient);
        return itemMapper.selectList(itemQueryWrapper);
    }


    public void exportItemExcel(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("itemValue", "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

            List<Item> list = queryItemListCache(0.625);
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
        List<Item> list = queryItemListCache(0.625);
        List<ItemVo> itemVoList = new ArrayList<>();
        for (Item item : list) {
            ItemVo itemVo = new ItemVo();
            BeanUtils.copyProperties(item, itemVo);
            itemVoList.add(itemVo);
        }
        String jsonForMat = JSON.toJSONString(JSONArray.parseArray(JSON.toJSONString(itemVoList)), SerializerFeature.PrettyFormat,
                SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullListAsEmpty);
        FileUtil.save(response, ConfigUtil.Item, "ItemValue.json", jsonForMat);
    }



}
