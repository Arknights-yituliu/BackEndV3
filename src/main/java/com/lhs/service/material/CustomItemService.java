package com.lhs.service.material;


import com.lhs.entity.dto.material.ItemInfoDTO;
import com.lhs.entity.dto.material.ItemValueConfigDTO;

import java.util.List;

public interface CustomItemService {






    List<ItemInfoDTO> getCustomItemList(ItemValueConfigDTO itemValueConfigDTO);
}
