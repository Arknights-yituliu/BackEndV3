package com.lhs.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lhs.common.config.FileConfig;
import com.lhs.common.util.FileUtil;
import com.lhs.entity.ResultVo;
import com.lhs.entity.Stage;
import com.lhs.mapper.ItemMapper;
import com.lhs.entity.Item;
import com.lhs.mapper.ResultMapper;
import com.lhs.service.resultVo.CompositeTable;
import com.lhs.service.resultVo.ItemCost;

import com.lhs.service.resultVo.ItemVo;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;



@Service
public class ItemService extends ServiceImpl<ItemMapper,Item>  {


    @Resource
    private ItemMapper itemMapper;
    @Resource
    private ResultMapper resultMapper;
    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    /**
     *  //根据蓝材料对应的常驻最高关卡效率En和旧蓝材料价值Vn计算新的蓝材料价值Vn+1  ，  Vn+1= Vn*1.25/En
     * @param items   材料信息表Vn
     * @param itemNameAndStageEff  map<蓝材料名称，蓝材料对应的常驻最高关卡效率En>
     * @return  新的材料信息表Vn+1
     */
    @Transactional
    public List<Item> ItemValueCalculation(List<Item> items, HashMap<String, Double> itemNameAndStageEff,Double expCoefficient) {

        JSONObject byProductJson = JSONObject.parseObject(resultMapper.selectOne(new QueryWrapper<ResultVo>().eq("path","product_value")
                .orderByDesc("create_time").last("limit 1")).getResult());  //读取根据Vn计算出的副产物价值

        List<CompositeTable> composite_table = JSONArray.parseArray(resultMapper.selectOne(new QueryWrapper<ResultVo>().eq("path","composite_table")
                .orderByDesc("create_time").last("limit 1")).getResult(), CompositeTable.class);  //读取加工站合成表

        Map<String, Item> itemValueMap = items.stream().collect(Collectors.toMap(Item::getItemName, Function.identity()));  //将旧的材料Vn集合转成map方便调用

        items.forEach(item -> item.setExpCoefficient(expCoefficient));//经验书系数
        itemNameAndStageEff.forEach((id,En)-> itemValueMap.get(id).setItemValue(itemValueMap.get(id).getItemValue()*1.25/En)); //在itemValueMap 设置新的材料价值Vn+1 ， Vn+1= Vn*1.25/En

        composite_table.forEach(table->{
            Integer rarity = itemValueMap.get(table.getId()).getRarity();
            double itemValueNew = 0.0;
            if(rarity<3){
                    for(ItemCost itemCost : table.getItemCost()){    itemValueNew += itemValueMap.get(itemCost.getId()).getItemValue()/itemCost.getCount(); } //灰，绿色品质是向下拆解   Vn+1 += Vn+1/合成需求个数
                    itemValueNew += Double.parseDouble(byProductJson.getString("rarity_"+(rarity))) -0.45*rarity;    //灰，绿色材料是+副产物价值    灰，绿色材料 = 蓝材料 + 副产物 - 龙门币
                }else  {
                    for(ItemCost itemCost :table.getItemCost()){  itemValueNew += itemValueMap.get(itemCost.getId()).getItemValue()*itemCost.getCount();  }//紫，金色品质是向上合成    Vn+1 +=Vn+1*合成需求个数
                    itemValueNew -= Double.parseDouble(byProductJson.getString("rarity_"+(rarity-1))) -0.45*rarity;  //紫，金色材料是减副产物价值   蓝材料  + 龙门币 - 副产物 = 紫，金色材料
                }

            itemValueMap.get(table.getId()).setItemValue(itemValueNew);
        });

        itemValueMap.forEach((k,item)->item.setItemValueAp(item.getItemValue()/1.25));
        saveByProductValue(items);  //保存Vn+1的加工站副产物平均产出价值
        updateBatchById(items);  //更新材料表
        List<ItemVo> itemVoList = new ArrayList<>();
        items.forEach(item -> {
            ItemVo itemVo = new ItemVo();
            BeanUtils.copyProperties(item,itemVo);
            itemVoList.add(itemVo);
        });
        redisTemplate.opsForValue().set("item/value/"+expCoefficient,itemVoList);

        String saveDate = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date());

        FileUtil.save(FileConfig.Backup,"itemValue_"+saveDate +"_"+expCoefficient+".json",JSON.toJSONString(items));
        return items;
    }

    /**
     * 保存加工站副产品期望价值
     * @param items  材料信息表Vn+1
     */
    public void saveByProductValue(List<Item> items) {
        double knockRating = 0.18;
        HashMap<Object, Object> hashMap = new HashMap<>();
        items.stream()
                .filter(item -> item.getWeight() > 0)
                .collect(Collectors.groupingBy(Item::getRarity))
                .forEach((rarity,list)->{
                    hashMap.put( "rarity_"+rarity, list.stream().mapToDouble(item -> item.getItemValue() * item.getWeight())
                            .sum() / items.size() * knockRating );
                } );
        resultMapper.insert(new ResultVo("product_value", JSON.toJSONString(hashMap)));
    }

    /**
     * 获取材料信息表
     * @param expCoefficient  经验书系数，一般为0.625（还有1.0和0.0）
     * @return  材料信息表
     */
    public List<Item> queryItemList(Double expCoefficient) {
        return itemMapper.selectList(new QueryWrapper<Item>().eq("exp_coefficient",expCoefficient).le("id", "199"));
    }


    public void exportItemExcel(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("itemValue", "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

            List<Item> list = itemMapper.selectList(new QueryWrapper<Item>().le("id",200));
            List<ItemVo> itemVoList = new ArrayList<>();
            for(Item item:list){
                ItemVo itemVo = new ItemVo();
                BeanUtils.copyProperties(item,itemVo);
                itemVoList.add(itemVo);
            }

            EasyExcel.write(response.getOutputStream(), ItemVo.class).sheet("Sheet1").doWrite(itemVoList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportItemJson(HttpServletResponse response) {
        List<Item> list = itemMapper.selectList(new QueryWrapper<Item>().le("id",200));
        List<ItemVo> itemVoList = new ArrayList<>();
        for(Item item:list){
            ItemVo itemVo = new ItemVo();
            BeanUtils.copyProperties(item,itemVo);
            itemVoList.add(itemVo);
        }
        String jsonForMat = JSON.toJSONString(JSONArray.parseArray(JSON.toJSONString(itemVoList)), SerializerFeature.PrettyFormat,
                SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullListAsEmpty);
        FileUtil.save(response,FileConfig.Item,"ItemValue.json",jsonForMat);
    }
}
