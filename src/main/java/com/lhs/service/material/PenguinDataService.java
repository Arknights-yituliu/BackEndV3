package com.lhs.service.material;

import java.util.List;
import java.util.Map;

import com.lhs.entity.dto.material.ItemValueConfigDTO;
import com.lhs.entity.dto.material.StageDropAndInfoDTO;
import com.lhs.entity.po.material.Stage;


public interface PenguinDataService {
    /**
     * 装配企鹅物流掉率与关卡信息
     *
     * @param itemValueConfigDTO 定价配置
     * @param stageInfoMap       关卡元数据，由调用方一次查询后传入，避免重复全表扫描
     * @return 关卡id → 掉落明细列表
     */
    Map<String, List<StageDropAndInfoDTO>> getStageDropCollect(ItemValueConfigDTO itemValueConfigDTO,
            Map<String, Stage> stageInfoMap);
}
