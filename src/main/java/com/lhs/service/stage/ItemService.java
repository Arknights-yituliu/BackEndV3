package com.lhs.service.stage;

import com.alibaba.excel.EasyExcel;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.FileUtil;

import com.lhs.common.util.JsonMapper;
import com.lhs.entity.po.stage.ItemIterationValue;
import com.lhs.entity.po.stage.WorkShopProducts;
import com.lhs.entity.dto.stage.StageParamDTO;
import com.lhs.mapper.ItemIterationValueMapper;
import com.lhs.mapper.ItemMapper;
import com.lhs.entity.po.stage.Item;

import com.lhs.mapper.WorkShopProductsMapper;
import com.lhs.entity.dto.stage.CompositeTableDTO;
import com.lhs.entity.dto.stage.ItemCostDTO;

import com.lhs.entity.vo.stage.ItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;
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
    public List<Item> ItemValueCal(List<Item> items, StageParamDTO stageParamDTO) {

        double expCoefficient =  stageParamDTO.getExpCoefficient();
        String version = stageParamDTO.getVersion();

        //读取上次迭代计算出的副产物价值
        QueryWrapper<ItemIterationValue> iterationValueQueryWrapper = new QueryWrapper<>();
        iterationValueQueryWrapper.eq("version",version);
        List<ItemIterationValue> itemIterationValueList = itemIterationValueMapper.selectList(iterationValueQueryWrapper);

        //找不到读基础版本
        if(itemIterationValueList.size()<1){
            iterationValueQueryWrapper.clear();
            iterationValueQueryWrapper.eq("version","original");
            itemIterationValueList = itemIterationValueMapper.selectList(iterationValueQueryWrapper);
        }

        //读取上次关卡效率计算的结果中蓝材料对应的常驻最高关卡效率
        QueryWrapper<WorkShopProducts> workShopProductsQueryWrapper = new QueryWrapper<>();
        workShopProductsQueryWrapper.eq("version",expCoefficient);
        List<WorkShopProducts> workShopProductsList = workShopProductsMapper.selectList(workShopProductsQueryWrapper);

        //找不到读基础版本
        if(workShopProductsList.size()<1){
            workShopProductsQueryWrapper.clear();
            workShopProductsQueryWrapper.eq("version","original");
            workShopProductsList = workShopProductsMapper.selectList(workShopProductsQueryWrapper);
        }

        //加工站副产物价值
        Map<String, Double> workShopProductsValue =
                workShopProductsList.stream().collect(Collectors.toMap(WorkShopProducts::getRank, WorkShopProducts::getExpectValue));

        String compositeTableText = FileUtil.read(ApplicationConfig.Item + "compositeTable.json");
        //读取加工站材料合成表
        List<CompositeTableDTO> compositeTableDTO = JsonMapper.parseJSONArray(compositeTableText, new TypeReference<List<CompositeTableDTO>>() {
        });

        Date date = new Date();
        long tableId = date.getTime();

        for (Item item :items) {
            item.setVersion(version); //经验书系数
            item.setId(tableId++);
            //设置经验书系数，经验书价值 = 龙门币价值 * 经验书系数
            if (item.getItemId().equals("2004")) {
                item.setItemValueAp(0.0036*2000 * expCoefficient);
            }
            if (item.getItemId().equals("2003")) {
                item.setItemValueAp(0.0036*1000 * expCoefficient);
            }
            if (item.getItemId().equals("2002")) {
                item.setItemValueAp(0.0036*400 * expCoefficient);
            }
            if (item.getItemId().equals("2001")) {
                item.setItemValueAp(0.0036*200 * expCoefficient);
            }

        }

        // Map<材料id,材料信息类>
        Map<String, Item> itemValueMap = items.stream().collect(Collectors.toMap(Item::getItemName, Function.identity()));


        //在itemValueMap 设置新的材料价值 新材料价值 = 旧材料价值/该材料主线最优关的关卡效率
        for(ItemIterationValue itemIterationValue:itemIterationValueList){
            double itemValueNew = itemValueMap.get(itemIterationValue.getItemName()).getItemValueAp() / itemIterationValue.getIterationValue();
            itemValueMap.get(itemIterationValue.getItemName()).setItemValueAp(itemValueNew);
        }

        compositeTableDTO.forEach(table -> {     //根据加工站合成表计算新价值
            Integer rarity = itemValueMap.get(table.getId()).getRarity();
            double itemValueNew = 0.0;
            StringBuilder message = new StringBuilder(itemValueMap.get(table.getId()).getItemName()).append(" = ( ");


            if (rarity < 3) {
                //灰，绿色品质是向下拆解   灰，绿色材料 = 蓝材料 + 副产物 - 龙门币
                for (ItemCostDTO itemCostDTO : table.getItemCost()) {
                    itemValueNew = (itemValueMap.get(itemCostDTO.getId()).getItemValueAp() +
                            workShopProductsValue.get("rarity_" + (rarity)) - 0.36 * rarity)
                            / itemCostDTO.getCount();
                }

            } else {
                //紫，金色品质是向上合成    蓝材料  + 龙门币 - 副产物 = 紫，金色材料
                for (ItemCostDTO itemCostDTO : table.getItemCost()) {
                    itemValueNew += itemValueMap.get(itemCostDTO.getId()).getItemValueAp() * itemCostDTO.getCount();
                }
                itemValueNew = itemValueNew + 0.36 * (rarity - 1) - workShopProductsValue.get("rarity_" + (rarity - 1));
            }

            itemValueMap.get(table.getId()).setItemValueAp(itemValueNew);  //存入新材料价值
        });

        QueryWrapper<Item> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.eq("version", version);
        int delete = itemMapper.delete(itemQueryWrapper);


        itemValueMap.forEach((k, item) ->{
                item.setItemValue(item.getItemValueAp() * 1.25);
        });
        saveByProductValue(items,version);  //保存Vn+1的加工站副产物平均产出价值
        saveBatch(items);  //更新材料表

        return items;
    }

    /**
     * 保存加工站副产品期望价值
     *
     * @param items 新材料信息表Vn+1
     * @param version 版本
     */
    public void saveByProductValue(List<Item> items,String version) {
        double knockRating = 0.2; //副产物爆率

        QueryWrapper<WorkShopProducts> workShopProductsQueryWrapper = new QueryWrapper<>();
        workShopProductsQueryWrapper.eq("version",version);
        int delete = workShopProductsMapper.delete(workShopProductsQueryWrapper);

            Map<Integer, List<Item>> collect = items.stream()
                    .filter(item -> item.getWeight() > 0)
                    .collect(Collectors.groupingBy(Item::getRarity));

            long time = new Date().getTime();

            for(Integer rarity :collect.keySet()){
                List<Item> list = collect.get(rarity);
                //副产物期望 = 所有材料的期望价值（材料价值 * 材料出率 /100）之和 * 副产物爆率
                double expectValue = list.stream().mapToDouble(item -> item.getItemValueAp() * item.getWeight()/100).sum()* knockRating;
                WorkShopProducts workShopProducts = new WorkShopProducts();
                workShopProducts.setRank("rarity_" + rarity);  //副产物等级
                workShopProducts.setId(time++);
                workShopProducts.setVersion(version); //版本号
                workShopProducts.setExpectValue(expectValue); //副产物期望价值
                workShopProductsMapper.insert(workShopProducts); //存入表
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


    public List<Item> queryItemList(StageParamDTO stageParamDTO) {
        QueryWrapper<Item> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.eq("version", stageParamDTO.getVersion());
        return itemMapper.selectList(itemQueryWrapper);
    }

    public List<Item> queryBaseItemList() {
        QueryWrapper<Item> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.eq("version","original");
        return itemMapper.selectList(itemQueryWrapper);
    }

    public void exportItemExcel(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("itemValue", "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

            List<Item> list = getItemListCache("public.0.625");
            List<ItemVO> itemVOList = new ArrayList<>();
            for (Item item : list) {
                ItemVO itemVo = new ItemVO();
                BeanUtils.copyProperties(item, itemVo);
                itemVOList.add(itemVo);
            }

            EasyExcel.write(response.getOutputStream(), ItemVO.class).sheet("Sheet1").doWrite(itemVOList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportItemJson(HttpServletResponse response) {
        List<Item> list = getItemListCache(new StageParamDTO().getVersion());
        List<ItemVO> itemVOList = new ArrayList<>();
        for (Item item : list) {
            ItemVO itemVo = new ItemVO();
            BeanUtils.copyProperties(item, itemVo);
            itemVOList.add(itemVo);
        }
        String jsonForMat = JsonMapper.toJSONString(JsonMapper.toJSONString(itemVOList));

        FileUtil.save(response, ApplicationConfig.Item, "ItemValue.json", jsonForMat);
    }


}
