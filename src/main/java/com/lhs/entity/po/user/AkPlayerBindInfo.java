package com.lhs.entity.po.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName
@Data
public class AkPlayerBindInfo {
    @TableId
    private Long id;
    //方舟uid
    private String akUid;
    //方舟昵称
    private String akNickName;
    //频道名称
    private String channelName;
    //频道id
    private Integer channelMasterId;

    private Long warehouseInfoId;
    //删除标记
    private Boolean deleteFlag;
    //
    private Long updateTime;

}
