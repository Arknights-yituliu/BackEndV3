package com.lhs.service.material;


import com.lhs.common.annotation.RedisCacheable;
import com.lhs.entity.dto.item.custom.ItemValueConfigDTO;

public interface CustomItemService {


    void customItemValueCalculation();


    void getCustomItemList(ItemValueConfigDTO itemValueConfigDTO);

    void getStageDropCollect(ItemValueConfigDTO itemValueConfigDTO);
}
