package com.lhs.entity.dto.user;

import java.util.List;

/**
 * UC 干员数据全量读取结果（GET /oauth2/ak-accounts/operators 响应 data 部分）
 *
 * <p>UC 侧始终返回该游戏账号的全部干员，星级筛选由调用方完成。</p>
 *
 * @author BackEndV3
 */
public class UcOperatorListVO {

    /** 游戏账号 UID */
    private String akUid;

    /** 干员记录列表，无数据时为空数组 */
    private List<UcOperatorVO> items;

    public String getAkUid() {
        return akUid;
    }

    public void setAkUid(String akUid) {
        this.akUid = akUid;
    }

    public List<UcOperatorVO> getItems() {
        return items;
    }

    public void setItems(List<UcOperatorVO> items) {
        this.items = items;
    }
}
