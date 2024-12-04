package com.lhs.service.material;

import com.lhs.entity.po.material.ItemIterationValue;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.material.Item;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;



public interface ItemService  {


    @Transactional
    List<Item> ItemValueCal(List<Item> items, StageConfigDTO stageConfigDTO);

    void saveItemIterationValue(List<ItemIterationValue> iterationValueList);

    void deleteItemIterationValue(String version);


    List<Item> getItemListCache(String version);

    List<Item> getItemList(StageConfigDTO stageConfigDTO);

    List<Item> getBaseItemList();

    List<Item> updateFloatingValueItem(StageConfigDTO stageConfigDTO);

    List<Item> getItemListCache(StageConfigDTO stageConfigDTO);
}
