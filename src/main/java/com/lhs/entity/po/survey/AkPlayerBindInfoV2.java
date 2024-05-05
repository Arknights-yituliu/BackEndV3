package com.lhs.entity.po.survey;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@TableName(value = "ak_player_bind_info_v2")
@Table
public class AkPlayerBindInfoV2 {

    @TableId
    private Long id;
    //一图流uid
    private Long uid;
    //方舟uid
    private String akUid;
    //方舟昵称
    private String akNickName;
    //频道名称
    private String channelName;
    //频道id
    private Integer channelMasterId;
    //是否是一图流账号默认绑定的uid
    private Boolean defaultFlag;
    //最后活跃时间
    private Long lastActiveTime;
    //删除标记
    private Boolean deleteFlag;

}
