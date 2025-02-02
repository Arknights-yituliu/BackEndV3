package com.lhs.service.material.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.enums.StageType;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.dto.material.*;
import com.lhs.entity.po.material.*;
import com.lhs.mapper.DataCacheMapper;
import com.lhs.mapper.material.ItemIterationValueMapper;
import com.lhs.mapper.material.ItemMapper;
import com.lhs.mapper.material.WorkShopProductsMapper;
import com.lhs.mapper.material.service.ItemMapperService;
import com.lhs.service.material.ItemService;
import com.lhs.service.material.StageService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemMapper itemMapper;

    private final ItemIterationValueMapper itemIterationValueMapper;

    private final WorkShopProductsMapper workShopProductsMapper;

    private final ItemMapperService itemMapperService;

    private final IdGenerator idGenerator;

    private final DataCacheMapper dataCacheMapper;

    private final StageService stageService;

    private final RedisTemplate<String, Object> redisTemplate;

    private static final JsonNode ITEM_SERIES_TABLE = JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.Config + "item_series_table.json"));


    public ItemServiceImpl(ItemMapper itemMapper,
                           ItemIterationValueMapper itemIterationValueMapper,
                           WorkShopProductsMapper workShopProductsMapper,
                           ItemMapperService itemMapperService,
                           DataCacheMapper dataCacheMapper,
                           StageService stageService,
                           RedisTemplate<String, Object> redisTemplate) {
        this.itemMapper = itemMapper;
        this.itemIterationValueMapper = itemIterationValueMapper;
        this.workShopProductsMapper = workShopProductsMapper;
        this.itemMapperService = itemMapperService;
        this.dataCacheMapper = dataCacheMapper;
        this.stageService = stageService;
        this.redisTemplate = redisTemplate;
        this.idGenerator = new IdGenerator(1L);
    }


//    private final static double LMD_VALUE = 0.0036;

    /**
     * //根据蓝材料对应的常驻最高关卡效率En和旧蓝材料价值Vn计算新的蓝材料价值Vn+1  ，  Vn+1= Vn*1/En
     *
     * @return 新的材料信息表Vn+1
     */
    @Transactional
    @Override
    public List<Item> calculatedItemValue(StageConfigDTO stageConfigDTO) {

        List<Item> itemList = getItemList(stageConfigDTO);

        Double lmdValue = stageConfigDTO.getLMDValue();
        Double EXPValue = stageConfigDTO.getEXPValue();
        String version = stageConfigDTO.getVersionCode();

        //材料迭代价值
        Map<String, Double> itemIterationValue = getItemIterationValue(version);

        //加工站副产物价值
        Map<String, Double> workShopProductsValue = getWorkShopProductsValue(version);

        //加工站材料合成表
        List<CompositeTableDTO> compositeTableDTO = getCompositeTable();

        Map<String, Item> itemValueMap = new HashMap<>();

        for (Item item : itemList) {
            item.setVersion(version);
            item.setId(idGenerator.nextId());

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

            List<ItemCustomValueDTO> customItem = stageConfigDTO.getCustomItem();
            Map<String, Double> customItemMap = new HashMap<>();
            if (customItem != null) {
                customItemMap = customItem.stream().collect(Collectors.toMap(ItemCustomValueDTO::getItemId, ItemCustomValueDTO::getItemValue));
            }

            Double value = customItemMap.get(itemId);
            if (value != null) {
                item.setItemValueAp(value);
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
                    Item rowItem = itemValueMap.get(itemCostDTO.getId());
                    itemValueNew = (rowItem.getItemValueAp() +
                            workShopProductsValue.get("rarity_" + rarity) - 0.36 * rarity) / itemCostDTO.getCount();
                }
            } else {
                //紫，金色品质是向上合成    紫，金色材料 =  合成所需蓝材料价值之和  + 龙门币 - 副产物
                for (ItemCostDTO itemCostDTO : table.getPathway()) {
                    Item rowItem = itemValueMap.get(itemCostDTO.getId());
                    itemValueNew += rowItem.getItemValueAp() * itemCostDTO.getCount();
                }
                itemValueNew = itemValueNew + 0.36 * (rarity - 1) - workShopProductsValue.get("rarity_" + (rarity - 1));
            }

            item.setItemValueAp(itemValueNew);  //存入新材料价值
        });

        QueryWrapper<Item> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.eq("version", version);
        itemMapper.delete(itemQueryWrapper);

        //保存新的材料价值表的加工站副产物平均产出价值
        saveByProductValue(itemList, version);
        //更新材料表
        itemMapperService.saveBatch(itemList);

        List<Item> fixedItemValue = updateFixedItemValue(stageConfigDTO);

        itemList.addAll(fixedItemValue);


        return itemList;
    }


    @Override
    public List<Item> calculatedCustomItemValue(StageConfigDTO stageConfigDTO) {

        long start = System.currentTimeMillis();

        List<CompositeTableDTO> compositeTableDTO = getCompositeTable();

        //读取一个加工站的每级材料产出期望
        LambdaQueryWrapper<WorkShopProducts> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkShopProducts::getVersion, "ORIGINAL");
        List<WorkShopProducts> workShopProductsList = workShopProductsMapper.selectList(queryWrapper);

        //读取18种蓝材料的迭代系数
        LambdaQueryWrapper<ItemIterationValue> iterationValueQueryWrapper = new LambdaQueryWrapper<>();
        iterationValueQueryWrapper.eq(ItemIterationValue::getVersion, "ORIGINAL");
        List<ItemIterationValue> itemIterationValueList = itemIterationValueMapper.selectList(iterationValueQueryWrapper);

        //读取关卡信息
        Map<String, Stage> stageInfoMap = stageService.getStageInfoMap();

        //读取材料信息
        List<Item> itemList = getItemList(stageConfigDTO);

        itemList.addAll(updateFixedItemValue(stageConfigDTO));
        Map<String, Item> itemMap = itemList.stream().collect(Collectors.toMap(Item::getItemId, Function.identity()));


        //获得一个企鹅物流掉落数据的Map对象，key为关卡id，value为关卡掉落集合，过滤掉低于样本阈值的数据，合并标准和磨难难度的关卡掉落
        Map<String, List<PenguinMatrixDTO>> matrixCollect = PenguinMatrixCollect
                .filterAndMergePenguinData(itemMap, stageInfoMap,
                        stageConfigDTO.getStageBlackMap(), stageConfigDTO.getSampleSize());

        //关卡迭代传递的参数
        ItemIterationParamDTO itemIterationParamDTO = new ItemIterationParamDTO();
        itemIterationParamDTO.setWorkShopProductsList(workShopProductsList);
        itemIterationParamDTO.setMatrixCollect(matrixCollect);
        itemIterationParamDTO.setItemIterationValueList(itemIterationValueList);

        DecimalFormat df = new DecimalFormat("0.00'%'");


        for (int i = 0; i < 100; i++) {
            LogUtils.info(stageConfigDTO.getVersionCode() + "第" + (i + 1) + "次迭代材料价值");
            //迭代材料价值
            calculatedCustomItemValue(stageConfigDTO, itemList, itemIterationParamDTO, compositeTableDTO);
            //获取迭代参数
            ItemIterationParamDTO nextItemIterationParam = getItemIterationParam(itemMap, stageConfigDTO, stageInfoMap, matrixCollect);
            //重设迭代参数
            itemIterationParamDTO.setMatrixCollect(nextItemIterationParam.getMatrixCollect());
            itemIterationParamDTO.setItemIterationValueList(nextItemIterationParam.getItemIterationValueList());
            itemList = itemIterationParamDTO.getItemList();
            itemMap = itemIterationParamDTO.getItemList().stream().collect(Collectors.toMap(Item::getItemId, Function.identity()));


            matrixCollect = nextItemIterationParam.getMatrixCollect();

            List<ItemIterationValue> list = nextItemIterationParam.getItemIterationValueList();
            boolean flag = true;
            for (ItemIterationValue iterationValue : list) {
                if (Math.abs(1.0 - iterationValue.getIterationValue()) > 0.0001) {
                    flag = false;
                }
            }

            if (flag) {
                break;
            }
        }

        itemIterationParamDTO.getItemIterationValueList().forEach(e -> {
            LogUtils.info(stageConfigDTO.getVersionCode() + " {} " + e.getItemName() + "-" + e.getStageCode() + "-" + e.getIterationValue());
        });
        return itemList;

    }

    public void calculatedCustomItemValue(StageConfigDTO stageConfigDTO,
                                          List<Item> itemList,
                                          ItemIterationParamDTO itemIterationParamDTO,
                                          List<CompositeTableDTO> compositeTableDTO) {


        Double lmdValue = stageConfigDTO.getLMDValue();
        Double EXPValue = stageConfigDTO.getEXPValue();
        String version = stageConfigDTO.getVersionCode();

        //加工站副产物价值
        List<WorkShopProducts> workShopProductsList = itemIterationParamDTO.getWorkShopProductsList();
        Map<String, Double> workShopProductsValue = workShopProductsList.stream()
                .collect(Collectors.toMap(WorkShopProducts::getItemRank, WorkShopProducts::getExpectValue));

        //材料迭代价值
        List<ItemIterationValue> itemIterationValueList = itemIterationParamDTO.getItemIterationValueList();
        Map<String, Double> itemIterationValueMap = itemIterationValueList.stream()
                .collect(Collectors.toMap(ItemIterationValue::getItemId, ItemIterationValue::getIterationValue));


        Map<String, Item> itemValueMap = new HashMap<>();

        List<ItemCustomValueDTO> customItem = stageConfigDTO.getCustomItem();
        Map<String, Double> customItemMap = customItem.stream().collect(Collectors.toMap(ItemCustomValueDTO::getItemId, ItemCustomValueDTO::getItemValue));

        for (Item item : itemList) {
            item.setVersion(version);
            item.setId(idGenerator.nextId());

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
            if (itemIterationValueMap.get(itemId) != null) {
                item.setItemValueAp(item.getItemValueAp() / itemIterationValueMap.get(itemId));
            }


            Double value = customItemMap.get(itemId);
            if (value != null) {
                item.setItemValueAp(value);
            }
            itemValueMap.put(itemId, item);
        }

        //根据加工站合成表计算新价值
        for(CompositeTableDTO table:compositeTableDTO){
            Item item = itemValueMap.get(table.getId());
            if(customItemMap.get(table.getId())!=null){
              continue;
            }
            Integer rarity = item.getRarity();
            double itemValueNew = 0.0;
            if (table.getResolve()) {
                //灰，绿色品质是向下拆解   灰，绿色材料 = （蓝材料价值 + 副产物 - 龙门币）/合成蓝材料的所需灰绿材料数量
                for (ItemCostDTO itemCostDTO : table.getPathway()) {
                    Item rowItem = itemValueMap.get(itemCostDTO.getId());
                    itemValueNew = (rowItem.getItemValueAp() +
                            workShopProductsValue.get("rarity_" + rarity) - 0.36 * rarity) / itemCostDTO.getCount();
                }

            } else {
                //紫，金色品质是向上合成    紫，金色材料 =  合成所需蓝材料价值之和  + 龙门币 - 副产物
                for (ItemCostDTO itemCostDTO : table.getPathway()) {
                    Item rowItem = itemValueMap.get(itemCostDTO.getId());
                    itemValueNew += rowItem.getItemValueAp() * itemCostDTO.getCount();
                }
                itemValueNew = itemValueNew + 0.36 * (rarity - 1) - workShopProductsValue.get("rarity_" + (rarity - 1));
            }
            item.setItemValueAp(itemValueNew);  //存入新材料价值
        }





        List<WorkShopProducts> workShopProductValue = getWorkShopProductValue(itemList);
        itemIterationParamDTO.setWorkShopProductsList(workShopProductValue);
        itemIterationParamDTO.setItemList(itemList);
    }

    private List<WorkShopProducts> getWorkShopProductValue(List<Item> items) {
        double knockRating = 0.2; //副产物爆率

        Map<Integer, List<Item>> collect = items.stream()
                .filter(item -> item.getWeight() > 0)
                .collect(Collectors.groupingBy(Item::getRarity));

        long time = new Date().getTime();

        List<WorkShopProducts> workShopProductsList = new ArrayList<>();

        for (Integer rarity : collect.keySet()) {
            List<Item> list = collect.get(rarity);
            //副产物期望 = 所有材料的期望价值（材料价值 * 材料出率 /100）之和 * 副产物爆率
            double expectValue = list.stream().mapToDouble(item -> item.getItemValueAp() * item.getWeight()).sum() * knockRating;

            WorkShopProducts workShopProducts = new WorkShopProducts();
            workShopProducts.setItemRank("rarity_" + rarity);  //副产物等级
            workShopProducts.setExpectValue(expectValue); //副产物期望价值
            workShopProductsList.add(workShopProducts); //存入表
        }

        return workShopProductsList;
    }

    private ItemIterationParamDTO getItemIterationParam(Map<String, Item> itemMap,
                                                        StageConfigDTO stageConfigDTO,
                                                        Map<String, Stage> stageInfoMap,
                                                        Map<String, List<PenguinMatrixDTO>> matrixCollect) {

        List<ItemIterationValue> itemIterationValueDTOList = new ArrayList<>();

        Map<String, List<PenguinMatrixDTO>> nextMatrixCollect = new HashMap<>();

        for (String stageId : matrixCollect.keySet()) {
            //关卡信息
            Stage stage = stageInfoMap.get(stageId);
            //来自企鹅物流的关卡掉落
            List<PenguinMatrixDTO> stageDropList = matrixCollect.get(stageId);

            if (calculateStageDropValueCount(stage, itemMap, stageConfigDTO, itemIterationValueDTOList, stageDropList)) {
                nextMatrixCollect.put(stageId, stageDropList);
            }

        }


        Map<String, List<ItemIterationValue>> collect = itemIterationValueDTOList.stream()
                .collect(Collectors.groupingBy(ItemIterationValue::getItemId));


        List<ItemIterationValue> iterationValueList = new ArrayList<>();
        for (String itemId : collect.keySet()) {
            List<ItemIterationValue> list = collect.get(itemId);
            list.sort(Comparator.comparing(ItemIterationValue::getIterationValue).reversed());
            ItemIterationValue itemIterationValue = list.get(0);
            iterationValueList.add(itemIterationValue);
        }

        ItemIterationParamDTO itemIterationParamDTO = new ItemIterationParamDTO();
        itemIterationParamDTO.setItemIterationValueList(iterationValueList);
        itemIterationParamDTO.setMatrixCollect(nextMatrixCollect);
        return itemIterationParamDTO;
    }


    private Boolean calculateStageDropValueCount(Stage stage, Map<String, Item> itemMap,
                                                 StageConfigDTO stageConfigDTO,
                                                 List<ItemIterationValue> itemIterationValueList,
                                                 List<PenguinMatrixDTO> stageDropList) {


        Double apCost = Double.valueOf(stage.getApCost());
        String stageId = stage.getStageId();
        String stageCode = stage.getStageCode();
        double countStageDropApValue = 0.0;

        String maxValueItemId = "";
        double maxValue = 0.0;

        //计算关卡掉落的一些详细信息
        for (PenguinMatrixDTO stageDrop : stageDropList) {
            //该条掉落物品的详细信息
            Item item = itemMap.get(stageDrop.getItemId());
            //材料掉率
            Double knockRating = ((double) stageDrop.getQuantity() / (double) stageDrop.getTimes());
            //计算该条掉落的期望产出理智价值
            double result = item.getItemValueAp() * knockRating;
            if (result > maxValue) {
                maxValue = result;
                maxValueItemId = item.getItemId();
            }


            countStageDropApValue += result;
        }


        double stageEfficiency = countStageDropApValue / apCost;


        if (stageEfficiency < 0.7) {
            return false;
        }

        String stageType = stage.getStageType();

        if (!stageConfigDTO.getUseActivityStage()) {
            if (!(StageType.MAIN.equals(stageType) || StageType.ACT_PERM.equals(stageType))) {
                return false;
            }
        }

        if (ITEM_SERIES_TABLE.get(maxValueItemId) != null) {
            String seriesId = ITEM_SERIES_TABLE.get(maxValueItemId).get("seriesId").asText();
            String series = ITEM_SERIES_TABLE.get(maxValueItemId).get("series").asText();
            ItemIterationValue itemIterationValue = new ItemIterationValue();
            itemIterationValue.setItemId(seriesId);
            itemIterationValue.setItemName(series);
            itemIterationValue.setIterationValue(stageEfficiency);
            itemIterationValue.setStageCode(stage.getStageCode());
            itemIterationValueList.add(itemIterationValue);
        }

        return true;
    }


    @Override
    public Long checkItemValue(StageConfigDTO stageConfigDTO) {

        Object value = redisTemplate.opsForValue().get("ItemValueCheck:" + stageConfigDTO.getId());
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(value));

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


    @RedisCacheable(key = "Item:itemValueList", paramOrMethod = "getVersionCode")
    @Override
    public List<Item> getItemListCache(StageConfigDTO stageConfigDTO) {
        LambdaQueryWrapper<Item> itemQueryWrapper = new LambdaQueryWrapper<>();
        itemQueryWrapper.in(Item::getVersion, stageConfigDTO.getVersionCode()).orderByDesc(Item::getItemValueAp);
        List<Item> itemList = itemMapper.selectList(itemQueryWrapper);
        List<Item> floatingValueItemList = updateFixedItemValue(stageConfigDTO);
        itemList.addAll(floatingValueItemList);
        return itemList;
    }

    @Override
    public List<Item> getCustomItemList(StageConfigDTO stageConfigDTO) {
        return calculatedCustomItemValue(stageConfigDTO);
    }


    @Override
    public Map<String, Item> getItemMapCache(StageConfigDTO stageConfigDTO) {
        List<Item> itemListCache = getItemListCache(stageConfigDTO);
        return itemListCache.stream().collect(Collectors.toMap(Item::getItemId, Function.identity()));
    }


    /**
     * 获取材料信息表
     *
     * @param stageConfigDTO 关卡计算参数
     * @return 材料信息表
     */
    @Override
    public List<Item> getItemList(StageConfigDTO stageConfigDTO) {
        LambdaQueryWrapper<Item> itemQueryWrapper = new LambdaQueryWrapper<>();
        itemQueryWrapper.in(Item::getVersion, stageConfigDTO.getVersionCode()).orderByDesc(Item::getItemValueAp);

        List<Item> itemList = itemMapper.selectList(itemQueryWrapper);
        if (itemList.isEmpty()) {
            itemQueryWrapper.clear();
            itemQueryWrapper.eq(Item::getVersion, "ORIGINAL");
            itemList = itemMapper.selectList(itemQueryWrapper);
        }
        return itemList;
    }


    @Override
    public List<Item> updateFixedItemValue(StageConfigDTO stageConfigDTO) {
        LambdaQueryWrapper<Item> itemLambdaQueryWrapper = new LambdaQueryWrapper<>();
        itemLambdaQueryWrapper.in(Item::getVersion, "FIXED", "CHIP");
        List<Item> itemList = itemMapper.selectList(itemLambdaQueryWrapper);

        //龙门币价值
        Double LMDValue = stageConfigDTO.getLMDValue();
        //经验书价值
        Double EXPValue = stageConfigDTO.getEXPValue();

        FloatingValueItem floatingValueItem = new FloatingValueItem();
        Map<String, Double> floatingValueItemMap = floatingValueItem.initValue(LMDValue, EXPValue, true);

        for (Item item : itemList) {
            String itemId = item.getItemId();
            if (floatingValueItemMap.get(itemId) != null) {
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
        String compositeTableText = FileUtil.read(ConfigUtil.Config + "composite_table.v2.json");
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
        workShopProductsMapper.delete(workShopProductsQueryWrapper);

        Map<Integer, List<Item>> collect = items.stream()
                .filter(item -> item.getWeight() > 0)
                .collect(Collectors.groupingBy(Item::getRarity));

        long time = new Date().getTime();

        for (Integer rarity : collect.keySet()) {
            List<Item> list = collect.get(rarity);
            //副产物期望 = 所有材料的期望价值（材料价值 * 材料出率 /100）之和 * 副产物爆率
            double expectValue = list.stream().mapToDouble(item -> item.getItemValueAp() * item.getWeight()).sum() * knockRating;
            LogUtils.info(rarity + "级材料副产物期望：" + expectValue / knockRating);
            WorkShopProducts workShopProducts = new WorkShopProducts();
            workShopProducts.setItemRank("rarity_" + rarity);  //副产物等级
            workShopProducts.setId(time++);
            workShopProducts.setVersion(version); //版本号
            workShopProducts.setExpectValue(expectValue); //副产物期望价值
            workShopProductsMapper.insert(workShopProducts); //存入表
        }
    }


}
