package com.lhs.service.resultVo;

import com.alibaba.excel.annotation.ExcelProperty;

public class ItemCustomValue {

    @ExcelProperty("物品名称")
    private String itemName;
    @ExcelProperty("等效理智价值")
    private Double itemValue;

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Double getItemValue() {
        return itemValue;
    }

    public void setItemValue(Double itemValue) {
        this.itemValue = itemValue;
    }

    @Override
    public String toString() {
        return "ItemCustomValue{" +
                "itemName='" + itemName + '\'' +
                ", itemValue='" + itemValue + '\'' +
                '}';
    }
}
