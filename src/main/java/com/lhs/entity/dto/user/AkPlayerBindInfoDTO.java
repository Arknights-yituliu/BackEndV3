package com.lhs.entity.dto.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data
public class AkPlayerBindInfoDTO {

    //方舟uid
    private String akUid;
    //方舟昵称
    private String akNickName;
    //频道名称
    private String channelName;
    //频道id
    private Integer channelMasterId;
    private Long warehouseInfoId;
}
