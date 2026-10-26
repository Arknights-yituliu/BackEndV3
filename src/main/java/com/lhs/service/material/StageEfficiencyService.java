package com.lhs.service.material;

import com.lhs.entity.dto.material.ItemValueConfigDTO;
import com.lhs.entity.vo.material.StageEfficiencyResultVO;

/**
 * 关卡效率与推荐关卡服务
 */
public interface StageEfficiencyService {

    /**
     * 根据物品定价配置计算各关卡的效率并返回推荐关卡
     *
     * @param itemValueConfigDTO 物品定价配置（与自定义价值接口共用）
     * @return 推荐关卡汇总结果
     */
    StageEfficiencyResultVO getRecommendedStage(ItemValueConfigDTO itemValueConfigDTO);
}
