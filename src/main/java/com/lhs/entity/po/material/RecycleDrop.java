package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * 一次回收站收获记录，一条记录对应一个物品。
 */
@TableName
public class RecycleDrop {
    @TableId
    private Long id;

    /** 物品 id */
    private String itemId;

    /** 物品产出时间 */
    private Date dropTime;

    /** 产出该物品的干员 id 列表，以 JSON 数组字符串存储 */
    private String operators;
    private String server;
    private String source;
    private String uid;
    private String version;
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Date getDropTime() {
        return dropTime;
    }

    public void setDropTime(Date dropTime) {
        this.dropTime = dropTime;
    }

    public String getOperators() {
        return operators;
    }

    public void setOperators(String operators) {
        this.operators = operators;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
