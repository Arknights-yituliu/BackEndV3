package com.lhs.service.item;


import com.lhs.entity.po.admin.HoneyCake;
import com.lhs.entity.po.item.StorePerm;
import com.lhs.entity.vo.item.ActivityStoreDataVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

public interface StoreService {

    void updateStorePerm();

    Map<String, List<StorePerm>> getStorePerm();


    /**
     * 获得当前未关闭的活动商店性价比
     *
     * @return 活动商店性价比
     */
    List<ActivityStoreDataVO> getActivityStoreData();

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
     * @param developerLevel      开发者权限等级
     * @return 更新状态消息
     */
    String updateActivityStoreDataByActivityName(ActivityStoreDataVO activityStoreDataVo, Boolean developerLevel);

    /**
     * 上传活动商店性价比图片
     *
     * @param file 图片文件
     * @return 图片链接
     */
    String uploadActivityBackgroundImage(MultipartFile file);


    void updateHoneyCake(List<HoneyCake> honeyCakeList);

    //    @RedisCacheable(key = "HoneyCake",timeout=86400)
    Map<String, HoneyCake> getHoneyCake();

    List<HoneyCake> getHoneyCakeList();

    Map<String, List<StorePerm>> getStorePermV2();
}
