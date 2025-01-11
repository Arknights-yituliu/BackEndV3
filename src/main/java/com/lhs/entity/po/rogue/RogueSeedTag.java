package com.lhs.entity.po.rogue;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName
public class RogueSeedTag {
    private Long tagId;
    private Long seedId;
    private String tag;
    private Date createTime;
    private Boolean deleteFlag;

}
