package com.lhs.service.material;

import java.util.List;
import java.util.Map;

import com.lhs.entity.dto.material.ItemValueConfigDTO;
import com.lhs.entity.dto.material.StageDropAndInfoDTO;


public interface PenguinDataService {
    Map<String, List<StageDropAndInfoDTO>> getStageDropCollect(ItemValueConfigDTO itemValueConfigDTO);
}
