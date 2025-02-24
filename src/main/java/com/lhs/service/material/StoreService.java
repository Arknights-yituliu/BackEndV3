package com.lhs.service.material;


import com.lhs.entity.dto.material.StageConfigDTO;

import com.lhs.entity.vo.material.ActivityStoreDataVO;

import java.util.*;

public interface StoreService {


    /**
     * 获得当前未关闭的活动商店性价比
     *
     * @return 活动商店性价比
     */
    List<ActivityStoreDataVO> getActivityStoreData(StageConfigDTO stageConfigDTO);

    /**
     * 获取活动商店性价比历史数据
     *
     * @return 活动商店性价比历史数据
     */
    List<ActivityStoreDataVO> getActivityStoreHistoryData();

    /**
     * 根据活动名称更新活动商店
     *
     * @param activityStoreDataVo 新的活动商店数据
     * @return 更新状态消息
     */
    String updateActivityStoreDataByActivityName(ActivityStoreDataVO activityStoreDataVo);








}
