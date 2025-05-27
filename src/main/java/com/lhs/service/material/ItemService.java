package com.lhs.service.material;

import com.lhs.entity.po.material.ItemIterationValue;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.material.Item;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;



public interface ItemService  {


    @Transactional
    List<Item> calculatedItemValue( StageConfigDTO stageConfigDTO);

    void saveItemIterationValue(List<ItemIterationValue> iterationValueList);

    void deleteItemIterationValue(String version);

    List<Item> getItemListCache(StageConfigDTO stageConfigDTO);

    List<Item> getCustomItemList(StageConfigDTO stageConfigDTO);

    Map<String, Item> getItemMapCache(StageConfigDTO stageConfigDTO);

    List<Item> getItemList(StageConfigDTO stageConfigDTO);

    List<Item> updateFixedItemValue(StageConfigDTO stageConfigDTO);


    List<Item>  calculatedCustomItemValue(StageConfigDTO stageConfigDTO);

    Long checkItemValue(StageConfigDTO stageConfigDTO);
}
