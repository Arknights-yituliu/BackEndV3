package com.lhs.service.material;


import com.lhs.entity.dto.item.custom.ItemInfoDTO;
import com.lhs.entity.dto.item.custom.ItemValueConfigDTO;

import java.util.List;

public interface CustomItemService {






    List<ItemInfoDTO> getCustomItemList(ItemValueConfigDTO itemValueConfigDTO);
}
