package com.lhs.entity.dto.material;


import com.lhs.entity.po.material.Item;
import com.lhs.entity.po.material.ItemIterationValue;
import com.lhs.entity.po.material.WorkShopProducts;

import java.util.List;
import java.util.Map;

public class ItemIterationParamDTO {
    private List<ItemIterationValue> itemIterationValueList;
    private List<WorkShopProducts> workShopProductsList;

    private Map<String, List<PenguinMatrixDTO>> matrixCollect;

    private List<Item> itemList;



    public void init() {
        WorkShopProducts workShopProductsT1 = new WorkShopProducts();
        workShopProductsT1.setItemRank("rarity_1");
        workShopProductsT1.setExpectValue(0.393);
        WorkShopProducts workShopProductsT2 = new WorkShopProducts();
        workShopProductsT2.setItemRank("rarity_2");
        workShopProductsT2.setExpectValue(1.172);
        WorkShopProducts workShopProductsT3 = new WorkShopProducts();
        workShopProductsT3.setItemRank("rarity_3");
        workShopProductsT3.setExpectValue(5.204);
        WorkShopProducts workShopProductsT4 = new WorkShopProducts();
        workShopProductsT4.setItemRank("rarity_4");
        workShopProductsT4.setExpectValue(17.598);
    }

    public List<ItemIterationValue> getItemIterationValueList() {
        return itemIterationValueList;
    }

    public void setItemIterationValueList(List<ItemIterationValue> itemIterationValueList) {
        this.itemIterationValueList = itemIterationValueList;
    }

    public List<WorkShopProducts> getWorkShopProductsList() {
        return workShopProductsList;
    }

    public void setWorkShopProductsList(List<WorkShopProducts> workShopProductsList) {
        this.workShopProductsList = workShopProductsList;
    }

    public Map<String, List<PenguinMatrixDTO>> getMatrixCollect() {
        return matrixCollect;
    }

    public void setMatrixCollect(Map<String, List<PenguinMatrixDTO>> matrixCollect) {
        this.matrixCollect = matrixCollect;
    }

    public List<Item> getItemList() {
        return itemList;
    }

    public void setItemList(List<Item> itemList) {
        this.itemList = itemList;
    }
}
