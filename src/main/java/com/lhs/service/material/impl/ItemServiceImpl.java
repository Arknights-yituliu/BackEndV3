package com.lhs.service.material.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.material.CompositeTableDTO;
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


        Double expCoefficient = stageParamDTO.getExpCoefficient();
        Double lmdValue = stageParamDTO.getLMDValue();
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

            String itemId = item.getItemId();
            //设置经验书系数，经验书价值 = 龙门币价值 * 经验书系数
            if (itemId.equals("2004")) {
                item.setItemValueAp(lmdValue * 2000 * expCoefficient);
            }
            if (itemId.equals("2003")) {
                item.setItemValueAp(lmdValue * 1000 * expCoefficient);
            }
            if (itemId.equals("2002")) {
                item.setItemValueAp(lmdValue * 400 * expCoefficient);
            }
            if (itemId.equals("2001")) {
                item.setItemValueAp(lmdValue * 200 * expCoefficient);
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
     * @param version 物品价值的版本号
     * @return 材料信息表
     */
    @Override
    @RedisCacheable(key = "Item:itemValue", params = "version")
    public List<Item> getItemListCache(String version) {
        LambdaQueryWrapper<Item> itemQueryWrapper = new LambdaQueryWrapper<>();
        System.out.println(version);
        itemQueryWrapper.in(Item::getVersion, version, "FIXED", "CHIP").orderByDesc(Item::getItemValueAp);
        return itemMapper.selectList(itemQueryWrapper);

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
        return itemMapper.selectList(itemQueryWrapper);
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
    public void updateOriginalFixedItemValue(StageParamDTO stageParamDTO) {
        LambdaQueryWrapper<Item> FIXEDLambdaQueryWrapper = new LambdaQueryWrapper<>();
        FIXEDLambdaQueryWrapper.eq(Item::getVersion, "FIXED");
        List<Item> fixedItems = itemMapper.selectList(FIXEDLambdaQueryWrapper);

        Double LMDValue = stageParamDTO.getLMDValue();
        Double EXPValue = stageParamDTO.getEXPValue();

        //采购凭证  （关卡AP - 龙门币价值*关卡掉落*倍率*关卡AP)/掉落数
        double itemValue_4006 = (30 - LMDValue * 12 * 30) / 21;
        //无人机  经验书价值 * 经验书产出数量
        double base_ap = EXPValue * 50 / 3;
        //赤金  无人机/赤金产出数量（1/24)
        double itemValue_3003 = base_ap * 24;
        double itemValue_32001 = itemValue_4006 * 90;
        double itemValue_3303 = (30 - LMDValue * 12 * 30) / (2 + 1.5 * (1.18 / 3 + 1.18 * 1.18 / 9));
        double itemValue_3302 = 1.18 * itemValue_3303 / 3;
        double itemValue_3301 = 1.18 * itemValue_3302 / 3;
        double AP36 = 36 - 36 * LMDValue * 12;
        double AP18 = 18 - 18 * LMDValue * 12;


        for (Item item : fixedItems) {
            if ("3003".equals(item.getItemId())) {
                item.setItemValueAp(itemValue_3003);
                itemMapper.updateById(item);
                continue;
            }

            if ("3303".equals(item.getItemId())) {
                item.setItemValueAp(itemValue_3303);
                itemMapper.updateById(item);
                continue;
            }

            if ("3302".equals(item.getItemId())) {
                item.setItemValueAp(itemValue_3302);
                itemMapper.updateById(item);
                continue;
            }

            if ("3301".equals(item.getItemId())) {
                item.setItemValueAp(itemValue_3301);
                itemMapper.updateById(item);
                continue;
            }

            if ("4006".equals(item.getItemId())) {
                item.setItemValueAp(itemValue_4006);
                itemMapper.updateById(item);
                continue;
            }

            if ("32001".equals(item.getItemId())) {
                item.setItemValueAp(itemValue_32001);
                itemMapper.updateById(item);
                continue;
            }

        }

        LambdaQueryWrapper<Item> CHIPLambdaQueryWrapper = new LambdaQueryWrapper<>();
        CHIPLambdaQueryWrapper.eq(Item::getVersion, "CHIP");
        List<Item> CHIPItems = itemMapper.selectList(CHIPLambdaQueryWrapper);


        String regex32X1 = "^32\\d1$";
        String regex32X2 = "^32\\d2$";
        String regex32X3 = "^32\\d3$";
        Pattern pattern32X1 = Pattern.compile(regex32X1);
        Pattern pattern32X2 = Pattern.compile(regex32X2);
        Pattern pattern32X3 = Pattern.compile(regex32X3);

        for (Item item : CHIPItems) {
            Matcher matcher32X1 = pattern32X1.matcher(item.getItemId());
            Matcher matcher32X2 = pattern32X2.matcher(item.getItemId());
            Matcher matcher32X3 = pattern32X3.matcher(item.getItemId());

            //芯片
            if (matcher32X1.matches()) {
                item.setItemValueAp(AP18);
                itemMapper.updateById(item);
                continue;
            }
            //芯片组
            if (matcher32X2.matches()) {
                item.setItemValueAp(AP36);
                itemMapper.updateById(item);
                continue;
            }

            //双芯片
            if (matcher32X3.matches()) {
                double value = AP36 * 2 + base_ap * 20 + itemValue_32001;
                item.setItemValueAp(value);
                itemMapper.updateById(item);
            }

        }

        LambdaQueryWrapper<Item> CHIP_LOW_OR_HEIGHT_VALUEQueryWrapper = new LambdaQueryWrapper<>();
        CHIP_LOW_OR_HEIGHT_VALUEQueryWrapper.eq(Item::getVersion, "CHIP_LOW_OR_HEIGHT_VALUE");
        List<Item> CHIP_LOW_OR_HEIGHT_VALUE = itemMapper.selectList(CHIP_LOW_OR_HEIGHT_VALUEQueryWrapper);


        for (Item item : CHIP_LOW_OR_HEIGHT_VALUE) {


            double CHIP1_HEIGHT_VALUE = (6 - 0.18) / 5 * AP18;

            double CHIP1_LOW_VALUE = (4 + 0.18) / 5 * AP18;

            double CHIP2_HEIGHT_VALUE = (6 - 0.18) / 5 * AP36;

            double CHIP2_LOW_VALUE = (4 + 0.18) / 5 * AP36;

            //强势芯片
            if ("3231".equals(item.getItemId()) || "3221".equals(item.getItemId())) {
                item.setItemValueAp(CHIP1_HEIGHT_VALUE);
                itemMapper.updateById(item);
            }
            //弱势芯片
            if ("3261".equals(item.getItemId()) || "3281".equals(item.getItemId())) {

                item.setItemValueAp(CHIP1_LOW_VALUE);
                itemMapper.updateById(item);
            }
            //均势芯片
            if ("3271".equals(item.getItemId()) || "3211".equals(item.getItemId()) ||
                    "3241".equals(item.getItemId()) || "3251".equals(item.getItemId())) {
                item.setItemValueAp(AP18);
                itemMapper.updateById(item);
            }

            //强势芯片组
            if ("3232".equals(item.getItemId()) || "3222".equals(item.getItemId())) {
                item.setItemValueAp(CHIP2_HEIGHT_VALUE);
                itemMapper.updateById(item);
            }
            //弱势芯片组
            if ("3262".equals(item.getItemId()) || "3282".equals(item.getItemId())) {
                item.setItemValueAp(CHIP2_LOW_VALUE);
                itemMapper.updateById(item);
            }

            //均势芯片组
            if ("3272".equals(item.getItemId()) || "3212".equals(item.getItemId()) ||
                    "3242".equals(item.getItemId()) || "3252".equals(item.getItemId())) {
                item.setItemValueAp(AP36);
                itemMapper.updateById(item);
            }

            //强势双芯片
            if ("3233".equals(item.getItemId()) || "3223".equals(item.getItemId())) {
                double value = CHIP2_HEIGHT_VALUE + base_ap * 20 + itemValue_32001;
                item.setItemValueAp(value);
                itemMapper.updateById(item);
            }
            //弱势双芯片
            if ("3263".equals(item.getItemId()) || "3283".equals(item.getItemId())) {
                double value = CHIP2_LOW_VALUE + base_ap * 20 + itemValue_32001;
                item.setItemValueAp(value);
                itemMapper.updateById(item);
            }
            //均势双芯片
            if ("3273".equals(item.getItemId()) || "3213".equals(item.getItemId()) ||
                    "3243".equals(item.getItemId()) || "3253".equals(item.getItemId())) {
                double value = AP36 + base_ap * 20 + itemValue_32001;
                item.setItemValueAp(value);
                itemMapper.updateById(item);
            }
        }
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
