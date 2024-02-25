package com.lhs.service.item;


import com.lhs.common.annotation.RedisCacheable;
import com.lhs.entity.po.dev.HoneyCake;
import com.lhs.entity.po.item.PackItem;
import com.lhs.entity.vo.item.PackInfoVO;
import com.lhs.entity.po.item.StorePerm;
import com.lhs.entity.vo.item.ActivityStoreDataVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

public interface StoreService {

    void updateStorePerm();

    Map<String, List<StorePerm>> getStorePerm();


    /**
     * 获得当前未关闭的活动商店性价比
     * @return 活动商店性价比
     */
    @RedisCacheable(key = "Item:StoreAct",timeout=86400)
    List<ActivityStoreDataVO> getActivityStoreData();

    /**
     * 获取活动商店性价比历史数据
     * @return  活动商店性价比历史数据
     */
    List<ActivityStoreDataVO> getActivityStoreHistoryData();

    /**
     * 根据活动名称更新活动商店
     * @param activityStoreDataVo 新的活动商店数据
     * @param developerLevel 开发者权限等级
     * @return 更新状态消息
     */
    String updateActivityStoreDataByActivityName(ActivityStoreDataVO activityStoreDataVo, Boolean developerLevel);

    /**
     * 上传活动商店性价比图片
     * @param file 图片文件
     * @return 图片链接
     */
    String uploadActivityBackgroundImage(MultipartFile file);

    /**
     * 更新礼包数据
     * @param packInfoVO 礼包数据
     * @return 更新后的礼包数据
     */
    PackInfoVO updateStorePackById(PackInfoVO packInfoVO);

    /**
     * 定期清理归档的礼包内容
     */
    void deleteArchivedPackContentData();

    /**
     * 获得礼包性价比数据（带缓存
     * @param saleStatus 礼包售卖状态
     * @return 礼包性价比数据
     */
    List<PackInfoVO> listPackInfoBySaleStatus(Integer saleStatus);

    /**
     * 获得全部礼包性价比数据（不带缓存
     * @return 礼包性价比数据
     */
    List<PackInfoVO> listAllPackInfoData();

    PackInfoVO getPackById(String idStr);

   List<PackItem> listPackItem();

    PackItem saveOrUpdatePackItem(PackItem newPackItem);

    void deletePackItemById(String id);

    void updatePackState(String id, Integer state);


    void uploadImage(MultipartFile file,Long id);

    String clearPackCache();



    void updateHoneyCake(List<HoneyCake> honeyCakeList);

    //    @RedisCacheable(key = "HoneyCake",timeout=86400)
    Map<String,HoneyCake>  getHoneyCake();

    List<HoneyCake> getHoneyCakeList();
}
