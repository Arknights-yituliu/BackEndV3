package com.lhs.entity.dto.survey;

import com.lhs.entity.po.survey.WarehouseInfo;
import lombok.Data;

import java.util.List;

@Data
public class WarehouseInventoryAPIParams {
    private String token;
    private String akUid;
    private List<WarehouseInfo> list;

}
