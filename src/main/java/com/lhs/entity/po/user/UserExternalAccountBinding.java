package com.lhs.entity.po.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName
public class UserExternalAccountBinding {

    @TableId
    private Long id;
    private Long uid;
    private String akUid;
    private Long createTime;
    private Long updateTime;
    private Boolean deleteFlag;
}
