package com.lhs.entity.po.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName
public class ImageInfo {

    @TableId
    private String imageName;
    private String imageLink;
    private String imageId;
    private Long createTime;
}
