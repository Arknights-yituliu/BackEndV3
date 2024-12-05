package com.lhs.entity.dto.material;

import com.lhs.entity.po.material.Item;
import com.lhs.entity.po.material.ItemIterationValue;
import com.lhs.entity.po.material.StageResult;
import com.lhs.entity.po.material.StageResultDetail;
import lombok.Data;

import java.util.*;

@Data
public class StageResultTmpDTO {

    //关卡详情集合
    private List<StageResultDetail> stageResultDetailList;

    //关卡效率集合
    private List<StageResult> stageResultList;

    //材料系列迭代值字典
    private Map<String, Double> itemIterationValueMap;

    private List<ItemIterationValue> itemIterationValueList;

    private List<Item> itemList;

    /**
     * 为关卡详情集合赋值
     *
     * @param stageResultDetailList 新的关卡详情集合
     */
    public void setStageResultDetailList(List<StageResultDetail> stageResultDetailList) {
        this.stageResultDetailList = stageResultDetailList != null ? new ArrayList<>(stageResultDetailList) : null;
    }

    /**
     * 向关卡详情集合中添加一个或多个元素
     *
     * @param stageResultDetails 要添加的一个或多个关卡详情
     */
    public void addStageResultDetail(StageResultDetail stageResultDetails) {
        if (this.stageResultDetailList == null) {
            this.stageResultDetailList = new ArrayList<>();
        }
        this.stageResultDetailList.add(stageResultDetails);
    }

    /**
     * 为关卡效率集合赋值
     *
     * @param stageResultList 新的关卡效率集合
     */
    public void setStageResultList(List<StageResult> stageResultList) {
        this.stageResultList = stageResultList != null ? new ArrayList<>(stageResultList) : null;
    }

    /**
     * 向关卡效率集合中添加一个或多个元素
     *
     * @param stageResults 要添加的一个或多个关卡效率
     */
    public void addStageResult(StageResult stageResults) {
        if (this.stageResultList == null) {
            this.stageResultList = new ArrayList<>();
        }
        this.stageResultList.add(stageResults);
    }

    /**
     * 为材料系列迭代值字典赋值
     *
     * @param itemIterationValueMap 新的材料系列迭代值字典
     */
    public void setItemIterationValueMap(Map<String, Double> itemIterationValueMap) {
        this.itemIterationValueMap = itemIterationValueMap != null ? new HashMap<>(itemIterationValueMap) : null;
    }

    /**
     * 向材料系列迭代值字典中添加或更新键值对
     *
     * @param key   材料系列的键
     * @param value 迭代值
     */
    public void putItemIterationValue(String key, Double value) {
        if (this.itemIterationValueMap == null) {
            this.itemIterationValueMap = new HashMap<>();
        }
        this.itemIterationValueMap.put(key, value);
    }

    /**
     * 从材料系列迭代值字典中根据系列id取出值
     *
     * @param key   材料系列的键
     */
    public Double getItemIterationValueByItemSeriesId(String key) {
        if (this.itemIterationValueMap == null) {
            return 0.0;
        }
        return this.itemIterationValueMap.get(key);
    }


    /**
     * 从材料系列迭代值字典中移除指定键
     *
     * @param key 材料系列的键
     */
    public void removeItemIterationValue(String key) {
        if (this.itemIterationValueMap != null) {
            this.itemIterationValueMap.remove(key);
        }
    }

    /**
     * 清空材料系列迭代值字典
     */
    public void clearItemIterationValueMap() {
        if (this.itemIterationValueMap != null) {
            this.itemIterationValueMap.clear();
        }
    }
}