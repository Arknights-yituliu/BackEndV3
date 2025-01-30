package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data

@TableName
public class ItemCustom {

    @TableId
    private String id;
    private Integer zoneIndex;
    private String name;
    private Double value;


}
