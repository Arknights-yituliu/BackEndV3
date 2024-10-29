package com.lhs.entity.po.rougeSeed;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName
public class RougeSeedTag {
    private Long tagId;
    private Long seedId;
    private String tag;
    private Long createTime;
    private Boolean deleteFlag;

}
