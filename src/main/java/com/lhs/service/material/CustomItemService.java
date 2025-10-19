package com.lhs.service.material;


import com.lhs.entity.dto.item.custom.ItemValueConfigDTO;

public interface CustomItemService {


    void customItemValueCalculation();



    void getCustomItemList(ItemValueConfigDTO itemValueConfigDTO, Integer maxIteration);
}
