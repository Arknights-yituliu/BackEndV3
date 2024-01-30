package com.lhs.service.item;


import com.lhs.common.annotation.RedisCacheable;
import com.lhs.entity.po.dev.HoneyCake;
import com.lhs.entity.po.item.PackItem;
import com.lhs.entity.vo.item.PackPromotionRatioVO;
import com.lhs.entity.po.item.StorePerm;
import com.lhs.entity.vo.item.StoreActVO;

import java.util.*;

public interface StoreService {

    void updateStorePerm();

    Map<String, List<StorePerm>> getStorePerm();

    String updateActStoreByActName(StoreActVO storeActVo, Boolean level);

    @RedisCacheable(key = "Item:StoreAct",timeout=86400)
    List<StoreActVO> getStoreAct();

    List<StoreActVO> selectActStoreHistory();

    void updateHoneyCake(List<HoneyCake> honeyCakeList);

    //    @RedisCacheable(key = "HoneyCake",timeout=86400)
    Map<String,HoneyCake>  getHoneyCake();

    List<HoneyCake> getHoneyCakeList();

    PackPromotionRatioVO updateStorePackById(PackPromotionRatioVO packPromotionRatioVO);

    List<PackPromotionRatioVO> getPackPromotionRatioList();

    PackPromotionRatioVO getPackById(String idStr);

   List<PackItem> getItemList();

    PackItem saveOrUpdatePackItem(PackItem newPackItem);

    void deletePackItemById(String id);

    void updatePackState(String id, Integer state);


}
