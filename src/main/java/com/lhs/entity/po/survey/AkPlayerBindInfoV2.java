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
@Entity
public class AkPlayerBindInfoV2 {

    @Id
    @TableId
    private Long id;
    private Long uid;
    private String userName;
    private String AkUid;
    private String AkNickName;
    private String ip;   //ip地址
    private Long lastTime;  //创建时间
    private boolean deleteFlag;


}
