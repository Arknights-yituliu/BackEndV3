package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("activity_store_data")
public class ActivityStoreData {
    @TableId
    private String actName;
    private Date endTime;
    private String storeData;
}
