package com.lhs.entity.po.rogueSeed;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName
public class RogueSeedTag {
    private Long tagId;
    private Long seedId;
    private String tag;
    private Long createTime;
    private Boolean deleteFlag;

}
