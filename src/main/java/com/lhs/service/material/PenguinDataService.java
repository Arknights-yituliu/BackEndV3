package com.lhs.service.material;

import java.util.List;
import java.util.Map;

import com.lhs.entity.dto.item.custom.ItemValueConfigDTO;
import com.lhs.entity.dto.item.custom.StageDropAndInfoDTO;


public interface PenguinDataService {
    Map<String, List<StageDropAndInfoDTO>> getStageDropCollect(ItemValueConfigDTO itemValueConfigDTO);
}
