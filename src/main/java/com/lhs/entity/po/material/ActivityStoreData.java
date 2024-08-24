package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("activity_store_data")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActivityStoreData {
    @TableId
    private String actName;
    private Date endTime;
    private String result;
}
