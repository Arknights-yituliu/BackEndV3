package com.lhs.entity.po.stage;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("store_act")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoreAct {
    @TableId
    private String actName;
    private Date endTime;
    private String result;
}
