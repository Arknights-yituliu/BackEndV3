package com.lhs.service.material;

import com.lhs.entity.po.material.ItemIterationValue;
import com.lhs.entity.dto.material.StageParamDTO;
import com.lhs.entity.po.material.Item;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;



public interface ItemService  {


    @Transactional
    List<Item> ItemValueCal(List<Item> items, StageParamDTO stageParamDTO);

    void saveItemIterationValue(List<ItemIterationValue> iterationValueList);

    void deleteItemIterationValue(String version);


    List<Item> getItemListCache(String version);

    List<Item> getItemList(StageParamDTO stageParamDTO);

    List<Item> getBaseItemList();

    void updateOriginalFixedItemValue(StageParamDTO stageParamDTO);
}
