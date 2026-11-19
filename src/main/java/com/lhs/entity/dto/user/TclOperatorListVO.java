package com.lhs.entity.dto.user;

import java.util.List;

/**
 * TCL 干员数据全量读取结果（GET /pat/ak-accounts/operators 响应 data 部分）
 *
 * <p>TCL 侧始终返回该游戏账号的全部干员，星级筛选由调用方完成。</p>
 *
 * @author BackEndV3
 */
public class TclOperatorListVO {

    /** 游戏账号 UID */
    private String akUid;

    /** 干员记录列表，无数据时为空数组 */
    private List<TclOperatorVO> items;

    public String getAkUid() {
        return akUid;
    }

    public void setAkUid(String akUid) {
        this.akUid = akUid;
    }

    public List<TclOperatorVO> getItems() {
        return items;
    }

    public void setItems(List<TclOperatorVO> items) {
        this.items = items;
    }
}
